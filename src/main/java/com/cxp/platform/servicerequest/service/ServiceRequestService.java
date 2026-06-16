package com.cxp.platform.servicerequest.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cxp.platform.ai.application.AiClassificationService;
import com.cxp.platform.ai.port.AiClassificationResponse;
import com.cxp.platform.auth.application.CurrentUserProvider;
import com.cxp.platform.auth.domain.UserProfile;
import com.cxp.platform.auth.infrastructure.persistence.UserProfileRepository;
import com.cxp.platform.common.idempotency.entity.IdempotencyRequest;
import com.cxp.platform.common.idempotency.repository.IdempotencyRequestRepository;
import com.cxp.platform.common.tenant.TenantContext;
import com.cxp.platform.location.application.LocationIntelligenceService;
import com.cxp.platform.location.domain.LocationRoutingRequest;
import com.cxp.platform.location.domain.LocationRoutingResult;
import com.cxp.platform.servicerequest.dto.ServiceRequestCreateRequest;
import com.cxp.platform.servicerequest.dto.ServiceRequestEventResponse;
import com.cxp.platform.servicerequest.dto.ServiceRequestEventsResponse;
import com.cxp.platform.servicerequest.dto.ServiceRequestResponse;
import com.cxp.platform.servicerequest.entity.ServiceRequest;
import com.cxp.platform.servicerequest.entity.ServiceRequestCategory;
import com.cxp.platform.servicerequest.entity.ServiceRequestExternalSyncStatus;
import com.cxp.platform.servicerequest.entity.ServiceRequestPriority;
import com.cxp.platform.servicerequest.entity.ServiceRequestSource;
import com.cxp.platform.servicerequest.entity.ServiceRequestStatus;
import com.cxp.platform.servicerequest.entity.ServiceRequestType;
import com.cxp.platform.servicerequest.entity.ServiceRequestVisibility;
import com.cxp.platform.servicerequest.exception.ServiceRequestNotFoundException;
import com.cxp.platform.servicerequest.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRequestEventService serviceRequestEventService;
    private final IdempotencyRequestRepository idempotencyRequestRepository;
    private final AiClassificationService aiClassificationService;
    private final LocationIntelligenceService locationIntelligenceService;
    private final UserProfileRepository userProfileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ServiceRequestMapper mapper;
    private final ObjectMapper objectMapper;

    /**
     * Creates a citizen service request.
     *
     * CONSISTENCY MODEL: this method is @Transactional, but two external HTTP calls
     * occur inside the transaction boundary before any DB write:
     *   1. AI sidecar (AiClassificationService) — runs when category/priority are absent.
     *   2. Ward-locator (LocationIntelligenceService) — runs when GPS coordinates are present.
     *
     * RISK: the DB connection is held open for the combined duration of both calls.
     *   - AI calls can take 5–30 seconds with LLM backends.
     *   - Ward-locator calls are fast but add latency on every request.
     *   - Under high concurrency this can exhaust the DB connection pool.
     * Both external calls have fallbacks (AI returns a default; ward-locator returns empty)
     * so they will not trigger a rollback — but they DO extend transaction duration.
     *
     * FUTURE FIX: extract external calls into a non-transactional preamble, then enter
     * @Transactional only for the DB write phase. This keeps the DB connection window
     * narrow (steps 4–8 only). Deferred to a future architecture phase.
     *
     * ATOMICITY INVARIANT: steps 4 (save ServiceRequest) + 5 (recordCreated event) are
     * inside this same @Transactional and MUST succeed together. If either fails, both
     * roll back — no orphaned request without an audit event, and no orphaned event.
     *
     * AUTHORITY: governingBodyId is derived from GPS coordinates via the ward FK chain
     * (local DB). It is NOT read from JWT/TenantContext — citizens supply location, not
     * tenant identity. Ward-locator provides a geographic hint; governingBodyId is
     * authoritative only when confirmed by the local WardRepository.
     *
     * EVENTUAL CONSISTENCY: Salesforce sync happens asynchronously via ServiceRequestSyncWorker
     * on a 60-second poll. The local DB is the system of record; Salesforce is the
     * external read-model. Sync failure does not affect the citizen-visible intake result.
     */
    @Transactional
    public ServiceRequestResponse create(ServiceRequestCreateRequest request, String idempotencyKey) {

        // userId is optional — citizen flows may be unauthenticated
        UUID userId = currentUserProvider.getCurrentUserIdOrNull();

        // type is optional from the mobile app — citizen intake defaults to COMPLAINT.
        ServiceRequestType resolvedType =
                request.type() != null ? request.type() : ServiceRequestType.COMPLAINT;
        log.info("SERVICE_REQUEST_CREATE userId={} type={} typeSentByClient={} category={} priority={}",
                userId, resolvedType, request.type(), request.category(), request.priority());

        // 1. AI enrichment resolution.
        //    Analyze→submit flow: mobile pre-fills category, priority, aiConfidence from the
        //    analyze response. AI is not re-run — the citizen's reviewed values are authoritative.
        //    Direct/non-AI submit: no pre-fill; backend runs AI classification as a fallback.
        boolean needsAi = request.category() == null || request.priority() == null;

        AiClassificationResponse aiResponse = needsAi
                ? aiClassificationService.classifyServiceRequest(request.description())
                : null;

        ServiceRequestPriority resolvedPriority = request.priority() != null
                ? request.priority()
                : AiServiceRequestAnalyzeService.resolvePriority(aiResponse != null ? aiResponse.getPriority() : null);

        // resolveCategory normalises the raw AI String → canonical enum, falling back to OTHER.
        ServiceRequestCategory resolvedCategory = request.category() != null
                ? request.category()
                : AiServiceRequestAnalyzeService.resolveCategory(aiResponse != null ? aiResponse.getCategory() : null);

        // Confidence: prefer the value carried forward from the analyze step (passed by mobile),
        // then fall back to the confidence from a fresh AI call on this request, then null.
        Double resolvedAiConfidence = request.aiConfidence() != null
                ? request.aiConfidence()
                : (aiResponse != null ? aiResponse.getConfidence() : null);

        // 2. Location routing.
        //    Case A — request contains GPS coordinates:
        //      coordinates → ward-locator → ward → governingBodyId
        //    Case B — no coordinates:
        //      fall back to the authenticated user's profile ward (residential jurisdiction).
        //      This covers scenarios like "submit a complaint about my area" from the home screen.
        LocationRoutingResult routing = locationIntelligenceService.resolveRouting(
                LocationRoutingRequest.builder()
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .build()
        );

        if (routing.getWardId() == null) {
            routing = profileGeographyFallback(userId);
            log.debug("service_request_geo_fallback userId={} wardId={}", userId, routing.getWardId());
        }

        // Ward is the primary routing unit. governing_body_id is optional metadata derived from the ward FK chain.
        if (routing.getWardId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot assign ward jurisdiction: no GPS coordinates provided and no ward found in your profile. " +
                    "Provide latitude/longitude, or complete onboarding to register your ward."
            );
        }

        UUID governingBodyId = routing.getGoverningBodyId();

        // 3. Idempotency guard — scoped to the jurisdiction resolved from location.
        //    Application-layer check: returns the cached response for duplicate requests.
        //    DB-layer guard: uq_sr_idempotency (V53) is the fallback for concurrent races —
        //    if two requests with the same key both pass this check, the second INSERT into
        //    service_request will fail on the unique index rather than creating a duplicate row.
        if (idempotencyKey != null) {
            IdempotencyRequest existing = idempotencyRequestRepository
                    .findByIdempotencyKeyAndGoverningBodyIdAndExpiresAtAfter(
                            idempotencyKey, governingBodyId, LocalDateTime.now())
                    .orElse(null);
            if (existing != null) {
                try {
                    String payload = existing.getResponsePayload() != null
                            ? existing.getResponsePayload()
                            : existing.getResponseBody();
                    return objectMapper.readValue(payload, ServiceRequestResponse.class);
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException("Failed to deserialize idempotent response", ex);
                }
            }
        }

        // 4. Persist — governingBodyId assigned from GIS, not from caller.
        //    source: AI_ASSISTED when mobile pre-filled category+priority from the analyze step;
        //    MANUAL when the backend had to run AI classification on this request.
        //    visibilityScope: citizen's stated intent; null defaults to PRIVATE.
        //    isPublic: derived from visibilityScope — never accepted from the client.
        ServiceRequestVisibility resolvedVisibility = request.visibilityScope() != null
                ? request.visibilityScope()
                : ServiceRequestVisibility.PRIVATE;
        boolean resolvedIsPublic = resolvedVisibility != ServiceRequestVisibility.PRIVATE;

        ServiceRequest serviceRequest = ServiceRequest.builder()
                .type(resolvedType)
                .title(request.title())
                .description(request.description())
                .category(resolvedCategory)
                .priority(resolvedPriority)
                .status(ServiceRequestStatus.OPEN)
                .addressText(resolveAddressText(request, routing))
                .latitude(request.latitude())
                .longitude(request.longitude())
                .departmentId(request.departmentId())
                .governingBodyId(governingBodyId)
                .wardId(routing.getWardId())
                .wardName(routing.getWardName())
                .formattedAddress(routing.getFormattedAddress())
                .aiConversationId(request.aiConversationId())
                .aiConfidence(resolvedAiConfidence)
                .source(!needsAi ? ServiceRequestSource.AI_ASSISTED : ServiceRequestSource.MANUAL)
                .visibilityScope(resolvedVisibility)
                .isPublic(resolvedIsPublic)
                .externalSyncStatus(ServiceRequestExternalSyncStatus.PENDING)
                .externalSyncAttempts(0)
                .idempotencyKey(idempotencyKey)
                .createdByUserId(userId)
                .updatedByUserId(userId)
                .build();

        ServiceRequest saved = serviceRequestRepository.save(serviceRequest);

        // 5. Immutable audit event — includes enrichment snapshot for auditability.
        serviceRequestEventService.recordCreated(saved, buildEnrichmentMetadata(
                resolvedCategory, resolvedPriority, resolvedAiConfidence, !needsAi, routing));

        // 6. Cache idempotency response
        ServiceRequestResponse response = mapper.toResponse(saved);
        if (idempotencyKey != null) {
            try {
                String payload = objectMapper.writeValueAsString(response);
                idempotencyRequestRepository.save(
                        IdempotencyRequest.builder()
                                .id(UUID.randomUUID())
                                .idempotencyKey(idempotencyKey)
                                .governingBodyId(governingBodyId)
                                .requestPath("/api/v1/service-requests")
                                .responseStatus(HttpStatus.OK.value())
                                .responseBody(payload)
                                .responsePayload(payload)
                                .createdAt(LocalDateTime.now())
                                .expiresAt(LocalDateTime.now().plusHours(24))
                                .build()
                );
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("Failed to serialize response for idempotency cache", ex);
            }
        }

        return response;
    }

    // ── Read queries — dual-mode access model ────────────────────────────────
    //
    // Citizen (Supabase JWT, no tenantId):
    //   TenantContext is unbound. Queries are scoped to the citizen's own submitted
    //   requests (createdByUserId). Citizens see only what they submitted.
    //
    // Operator (platform JWT, tenantId present):
    //   TenantContext is bound. Queries are scoped to the entire governing body.
    //   Operators see all requests within their jurisdiction.
    //
    // ARCHITECTURAL RULE: Never call TenantContext.get() on a citizen-reachable path.
    //   Use TenantContext.getOrNull() and branch on null to select the correct query.
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ServiceRequestResponse> list(Pageable pageable) {
        UUID governingBodyId = TenantContext.getOrNull();
        if (governingBodyId != null) {
            log.info("SERVICE_REQUEST_LIST operator_flow governingBodyId={}", governingBodyId);
            return serviceRequestRepository
                    .findByGoverningBodyIdOrderByCreatedAtDesc(governingBodyId, pageable)
                    .map(mapper::toResponse);
        }
        UUID userId = currentUserProvider.getCurrentUserId();
        log.info("SERVICE_REQUEST_LIST citizen_flow userId={}", userId);
        return serviceRequestRepository
                .findByCreatedByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponse get(UUID id) {
        return mapper.toResponse(findScopedById(id));
    }

    @Transactional(readOnly = true)
    public ServiceRequestEventsResponse getEvents(UUID id) {
        ServiceRequest request = findScopedById(id);
        List<ServiceRequestEventResponse> events = serviceRequestEventService
                .getEventsForRequest(request.getId())
                .stream()
                .map(e -> ServiceRequestEventResponse.builder()
                        .id(e.getId())
                        .eventType(e.getEventType())
                        .oldStatus(e.getOldStatus())
                        .newStatus(e.getNewStatus())
                        .actorType(e.getActorType() != null ? e.getActorType() : "SYSTEM")
                        .actorUserId(e.getActorUserId())
                        .notes(e.getNotes())
                        .metadataJson(e.getMetadataJson())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
        log.info("SERVICE_REQUEST_EVENTS id={} count={}", id, events.size());
        return new ServiceRequestEventsResponse(request.getId(), null, events);
    }

    // --- Operator-only write workflow ---

    @Transactional
    public ServiceRequestResponse updateStatus(UUID id, ServiceRequestStatus newStatus) {
        UUID userId = currentUserProvider.getCurrentUserId();
        // Status changes are operator actions — require tenant context.
        ServiceRequest request = findTenantScoped(id);
        ServiceRequestStatus previousStatus = request.getStatus();

        request.setStatus(newStatus);
        request.setUpdatedByUserId(userId);

        if (newStatus == ServiceRequestStatus.RESOLVED || newStatus == ServiceRequestStatus.CLOSED) {
            request.setResolvedAt(Instant.now());
        }

        ServiceRequest saved = serviceRequestRepository.save(request);
        serviceRequestEventService.recordStatusChanged(saved, previousStatus, userId);

        return mapper.toResponse(saved);
    }

    // ── Scoped lookup helpers ─────────────────────────────────────────────────

    /**
     * Finds a service request by ID using whichever scope is available.
     * Operator: scoped to governing body (TenantContext).
     * Citizen: scoped to the submitting user (createdByUserId).
     */
    private ServiceRequest findScopedById(UUID id) {
        UUID governingBodyId = TenantContext.getOrNull();
        if (governingBodyId != null) {
            return serviceRequestRepository.findByIdAndGoverningBodyId(id, governingBodyId)
                    .orElseThrow(() -> new ServiceRequestNotFoundException(id));
        }
        UUID userId = currentUserProvider.getCurrentUserId();
        return serviceRequestRepository.findByIdAndCreatedByUserId(id, userId)
                .orElseThrow(() -> new ServiceRequestNotFoundException(id));
    }

    /** Operator-only: requires TenantContext to be bound. */
    private ServiceRequest findTenantScoped(UUID id) {
        return serviceRequestRepository.findByIdAndGoverningBodyId(id, TenantContext.get())
                .orElseThrow(() -> new ServiceRequestNotFoundException(id));
    }

    private String resolveAddressText(ServiceRequestCreateRequest request, LocationRoutingResult routing) {
        if (request.addressText() != null && !request.addressText().isBlank()) {
            return request.addressText();
        }
        return routing.getFormattedAddress();
    }

    /**
     * Builds a LocationRoutingResult from the authenticated user's stored profile geography.
     * Used as a fallback when the service request carries no GPS coordinates.
     * Returns an empty result if the user is unauthenticated or has no profile ward.
     */
    private LocationRoutingResult profileGeographyFallback(UUID userId) {
        if (userId == null) {
            return LocationRoutingResult.builder().build();
        }
        return userProfileRepository.findByUserId(userId)
                .filter(UserProfile::hasWard)
                .map(p -> LocationRoutingResult.builder()
                        .governingBodyId(p.getGoverningBodyUuid())
                        .wardId(p.getWardUuid())
                        .wardName(p.getWardName())
                        .build())
                .orElse(LocationRoutingResult.builder().build());
    }

    private String buildEnrichmentMetadata(
            ServiceRequestCategory category,
            ServiceRequestPriority priority,
            Double confidence,
            boolean aiEnriched,
            LocationRoutingResult routing) {
        try {
            Map<String, Object> ai = new LinkedHashMap<>();
            ai.put("category", category);
            ai.put("priority", priority != null ? priority.name() : null);
            ai.put("confidence", confidence);
            ai.put("enriched", aiEnriched);

            Map<String, Object> location = new LinkedHashMap<>();
            location.put("wardId", routing.getWardId() != null ? routing.getWardId().toString() : null);
            location.put("wardName", routing.getWardName());

            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("ai", ai);
            snapshot.put("location", location);

            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.warn("enrichment_metadata_serialization_failed error={}", e.getMessage());
            return null;
        }
    }
}

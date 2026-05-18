package com.cxp.platform.complaint.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cxp.platform.ai.domain.AiConversation;
import com.cxp.platform.ai.port.AiClassificationResponse;
import com.cxp.platform.ai.repository.AiConversationRepository;
import com.cxp.platform.ai.application.AiClassificationService;
import com.cxp.platform.complaint.api.ComplaintResponse;
import com.cxp.platform.complaint.api.CreateAIComplaintRequest;
import com.cxp.platform.complaint.api.CreateManualComplaintRequest;
import com.cxp.platform.complaint.domain.Complaint;
import com.cxp.platform.complaint.domain.enums.ComplaintCreatedVia;
import com.cxp.platform.complaint.domain.enums.ExternalSyncStatus;
import com.cxp.platform.complaint.domain.enums.ComplaintStatus;
import com.cxp.platform.complaint.exception.ComplaintNotFoundException;
import com.cxp.platform.complaint.event.ComplaintCreatedEvent;
import com.cxp.platform.complaint.repository.ComplaintRepository;
import com.cxp.platform.common.idempotency.entity.IdempotencyRequest;
import com.cxp.platform.common.idempotency.repository.IdempotencyRequestRepository;
import com.cxp.platform.common.tenant.TenantContext;
import com.cxp.platform.governance.domain.Ward;
import com.cxp.platform.governance.repository.WardRepository;
import com.cxp.platform.location.domain.LocationRoutingRequest;
import com.cxp.platform.location.domain.LocationRoutingResult;
import com.cxp.platform.location.domain.WardLookupResult;
import com.cxp.platform.location.application.LocationIntelligenceService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

/**
 * Application service orchestrating complaint creation workflow.
 */
@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintEventService complaintEventService;
    private final IdempotencyRequestRepository idempotencyRequestRepository;
    private final AiClassificationService aiClassificationService;
    private final AiConversationRepository aiConversationRepository;
    private final WardRepository wardRepository;
    private final LocationIntelligenceService locationIntelligenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            ComplaintEventService complaintEventService,
            IdempotencyRequestRepository idempotencyRequestRepository,
            AiClassificationService aiClassificationService,
            AiConversationRepository aiConversationRepository,
            WardRepository wardRepository,
            LocationIntelligenceService locationIntelligenceService,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {

        this.complaintRepository = complaintRepository;
        this.complaintEventService = complaintEventService;
        this.idempotencyRequestRepository = idempotencyRequestRepository;
        this.aiClassificationService = aiClassificationService;
        this.aiConversationRepository = aiConversationRepository;
        this.wardRepository = wardRepository;
        this.locationIntelligenceService = locationIntelligenceService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new complaint.
     *
     * Flow:
     * 0) Enforce tenant-scoped idempotency to prevent duplicate complaint creation due to retries
     * 1) Apply defaults
     * 2) Use AI to auto-classify complaint text
     * 3) Derive location routing snapshot at creation time for audit and analytics
     * 4) Persist complaint
     * 5) Record complaint event
     * 6) Publish domain event for external integrations
     */
    @Transactional
    public Complaint createComplaint(Complaint complaint, String idempotencyKey) {
        UUID tenantId = TenantContext.get();
        IdempotencyRequest existing = idempotencyRequestRepository
                .findByIdempotencyKeyAndGoverningBodyIdAndExpiresAtAfter(
                        idempotencyKey,
                        tenantId,
                        LocalDateTime.now()
                )
                .orElse(null);
        if (existing != null) {
            try {
                String payload = existing.getResponsePayload() != null
                        ? existing.getResponsePayload()
                        : existing.getResponseBody();
                return objectMapper.readValue(payload, Complaint.class);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("Failed to deserialize idempotent complaint response", ex);
            }
        }

        // Default status
        if (complaint.getStatus() == null) {
            complaint.setStatus(ComplaintStatus.OPEN);
        }

        // External sync metadata
        complaint.setExternalSystem("SALESFORCE");
        complaint.setExternalSyncStatus(ExternalSyncStatus.PENDING);
        complaint.setExternalSyncAttempts(0);
        complaint.setExternalLastSyncAt(null);
        complaint.setExternalReference(null);

        // AI Classification via AI Service layer
        AiClassificationResponse aiResponse =
                aiClassificationService.classifyComplaint(
                        complaint.getDescription()
                );

        complaint.setPriority(aiResponse.getPriority());

        LocationRoutingRequest request = LocationRoutingRequest.builder()
                .latitude(complaint.getLatitude())
                .longitude(complaint.getLongitude())
                .reportedLocationText(complaint.getReportedLocationText())
                .build();

        LocationRoutingResult routing =
                locationIntelligenceService.resolveRouting(request);

        /**
         * Persisting routing snapshot derived at creation time for
         * audit, analytics, and external system integrations.
         */
        complaint.setMunicipalityId(routing.getMunicipalityId());
        complaint.setMunicipalityName(routing.getMunicipalityName());
        complaint.setWardId(routing.getWardId());
        complaint.setWardName(routing.getWardName());
        complaint.setFormattedAddress(routing.getFormattedAddress());

        Ward ward = wardRepository.findById(routing.getWardId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Ward not found with id " + routing.getWardId()));
        UUID governingBodyId = ward.getGoverningBody().getId();
        complaint.setGoverningBodyId(governingBodyId);

        // Save complaint
        Complaint savedComplaint = complaintRepository.save(complaint);

        // Record event in complaint_event table
        complaintEventService.createCreatedEvent(savedComplaint);

        // Publish domain event for integrations
        eventPublisher.publishEvent(
                new ComplaintCreatedEvent(savedComplaint.getId())
        );

        try {
            String responsePayload = objectMapper.writeValueAsString(savedComplaint);
            idempotencyRequestRepository.save(
                    IdempotencyRequest.builder()
                            .id(UUID.randomUUID())
                            .idempotencyKey(idempotencyKey)
                            .governingBodyId(governingBodyId)
                            .requestHash(null)
                            .requestPath("/complaints")
                            .responseStatus(HttpStatus.OK.value())
                            .responseBody(responsePayload)
                            .responsePayload(responsePayload)
                            .createdAt(LocalDateTime.now())
                            .expiresAt(LocalDateTime.now().plusHours(24))
                            .build()
            );
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Failed to serialize complaint response for idempotency", ex);
        }

        return savedComplaint;
    }

    /**
     * Enforces tenant isolation by fetching complaints scoped to the
     * governing body from the current request tenant context.
     */
    public List<Complaint> getAllComplaints() {
        return complaintRepository.findByGoverningBodyIdOrderByCreatedAtDesc(
                TenantContext.get()
        );
    }

    /**
     * Complaint lookup must always be tenant scoped to prevent
     * cross-municipality data access.
     */
    public Complaint getComplaintById(UUID id) {
        return complaintRepository.findByIdAndGoverningBodyId(id, TenantContext.get())
                .orElseThrow(() -> new ComplaintNotFoundException(id));
    }

    /**
     * Creates official complaint using AI conversation after user confirmation.
     */
    @Transactional
    public ComplaintResponse createFromAI(CreateAIComplaintRequest request) {
        AiConversation conversation = aiConversationRepository.findById(request.aiConversationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "AI conversation not found with id " + request.aiConversationId()));

        LocationRoutingResult routing = locationIntelligenceService.resolveRouting(
                LocationRoutingRequest.builder()
                        .reportedLocationText(request.reportedLocationText())
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .build()
        );

        WardLookupResult wardLookupResult = WardLookupResult.builder()
                .municipalityId(routing.getMunicipalityId())
                .municipalityName(routing.getMunicipalityName())
                .wardId(routing.getWardId())
                .wardName(routing.getWardName())
                .build();
        Ward ward = resolveWard(wardLookupResult);
        UUID governingBodyId = ward.getGoverningBody().getId();

        Complaint complaint = Complaint.builder()
                .title(request.title())
                .description(request.description())
                .reportedLocationText(request.reportedLocationText())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .wardId(wardLookupResult.getWardId())
                .wardName(wardLookupResult.getWardName())
                .municipalityId(wardLookupResult.getMunicipalityId())
                .municipalityName(wardLookupResult.getMunicipalityName())
                .governingBodyId(governingBodyId)
                .status(ComplaintStatus.OPEN)
                .createdVia(ComplaintCreatedVia.AI_ASSISTANT)
                .aiConversationId(conversation.getId())
                .externalSyncStatus(ExternalSyncStatus.PENDING)
                .externalSyncAttempts(0)
                .externalLastSyncAt(null)
                .externalReference(null)
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);
        complaintEventService.createCreatedEvent(savedComplaint);
        eventPublisher.publishEvent(new ComplaintCreatedEvent(savedComplaint.getId()));

        return new ComplaintResponse(
                savedComplaint.getId(),
                savedComplaint.getStatus() != null ? savedComplaint.getStatus().name() : null,
                savedComplaint.getDepartment(),
                savedComplaint.getPriority(),
                savedComplaint.getCreatedAt()
        );
    }

    /**
     * Creates complaint without AI assistance.
     */
    @Transactional
    public ComplaintResponse createManual(CreateManualComplaintRequest request) {
        LocationRoutingResult routing = locationIntelligenceService.resolveRouting(
                LocationRoutingRequest.builder()
                        .reportedLocationText(request.reportedLocationText())
                        .latitude(request.latitude())
                        .longitude(request.longitude())
                        .build()
        );

        WardLookupResult wardLookupResult = WardLookupResult.builder()
                .municipalityId(routing.getMunicipalityId())
                .municipalityName(routing.getMunicipalityName())
                .wardId(routing.getWardId())
                .wardName(routing.getWardName())
                .build();
        Ward ward = resolveWard(wardLookupResult);
        UUID governingBodyId = ward.getGoverningBody().getId();

        Complaint complaint = Complaint.builder()
                .title(request.title())
                .description(request.description())
                .reportedLocationText(request.reportedLocationText())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .wardId(wardLookupResult.getWardId())
                .wardName(wardLookupResult.getWardName())
                .municipalityId(wardLookupResult.getMunicipalityId())
                .municipalityName(wardLookupResult.getMunicipalityName())
                .governingBodyId(governingBodyId)
                .status(ComplaintStatus.OPEN)
                .createdVia(ComplaintCreatedVia.MANUAL_FORM)
                .department(request.departmentCode())
                .externalSyncStatus(ExternalSyncStatus.PENDING)
                .externalSyncAttempts(0)
                .externalLastSyncAt(null)
                .externalReference(null)
                .build();

        Complaint savedComplaint = complaintRepository.save(complaint);
        complaintEventService.createCreatedEvent(savedComplaint);
        eventPublisher.publishEvent(new ComplaintCreatedEvent(savedComplaint.getId()));

        return new ComplaintResponse(
                savedComplaint.getId(),
                savedComplaint.getStatus() != null ? savedComplaint.getStatus().name() : null,
                savedComplaint.getDepartment(),
                savedComplaint.getPriority(),
                savedComplaint.getCreatedAt()
        );
    }

    /**
     * Fetches Ward entity and validates existence.
     */
    private Ward resolveWard(WardLookupResult result) {
        return wardRepository.findById(result.getWardId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Ward not found with id " + result.getWardId()));
    }

}

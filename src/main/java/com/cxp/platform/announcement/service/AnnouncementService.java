package com.cxp.platform.announcement.service;

import com.cxp.platform.announcement.domain.Announcement;
import com.cxp.platform.announcement.domain.AnnouncementPriority;
import com.cxp.platform.announcement.domain.AnnouncementStatus;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import com.cxp.platform.announcement.dto.AnnouncementCreateRequest;
import com.cxp.platform.announcement.dto.AnnouncementResponse;
import com.cxp.platform.announcement.dto.CitizenFeedResponse;
import com.cxp.platform.announcement.exception.AnnouncementNotFoundException;
import com.cxp.platform.announcement.repository.AnnouncementRepository;
import com.cxp.platform.auth.application.CurrentUserProvider;
import com.cxp.platform.auth.domain.UserProfile;
import com.cxp.platform.auth.infrastructure.persistence.UserProfileRepository;
import com.cxp.platform.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    // Passed to feed queries as the "everything city-wide or above" scope ladder.
    // ZONE is excluded here — zone resolution requires a ward→zone lookup that
    // belongs in the future AudienceResolverService, not this query.
    private static final List<AnnouncementTargetScope> BROAD_SCOPES =
            List.of(AnnouncementTargetScope.CITY,
                    AnnouncementTargetScope.DISTRICT,
                    AnnouncementTargetScope.STATE);

    private final AnnouncementRepository announcementRepository;
    private final UserProfileRepository userProfileRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AnnouncementMapper mapper;

    /**
     * Creates an announcement on behalf of the authenticated operator.
     *
     * governingBodyId is taken from TenantContext (populated from JWT).
     * targetScope is the operator's chosen geographic audience level.
     * Exactly one matching geo FK (wardId / zoneId / cityId / districtId / stateId)
     * must be provided; mismatches return HTTP 400.
     *
     * If status is PUBLISHED, publishedAt is set to now.
     * DRAFT announcements have no publishedAt and are invisible to citizens.
     */
    @Transactional
    public AnnouncementResponse create(AnnouncementCreateRequest request) {
        UUID userId = currentUserProvider.getCurrentUserId();
        UUID governingBodyId = TenantContext.get();

        validateTargeting(request);

        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "expires_at must be a future timestamp");
        }

        AnnouncementStatus resolvedStatus = resolveStatus(request.status());
        Instant resolvedPublishedAt = (resolvedStatus == AnnouncementStatus.PUBLISHED)
                ? Instant.now() : null;

        Announcement announcement = Announcement.builder()
                .title(request.title())
                .content(request.content())
                .summary(request.summary())
                .category(request.category())
                .priority(request.priority() != null ? request.priority() : AnnouncementPriority.NORMAL)
                .status(resolvedStatus)
                .governingBodyId(governingBodyId)
                .targetScope(request.targetScope())
                .wardId(request.wardId())
                .zoneId(request.zoneId())
                .cityId(request.cityId())
                .districtId(request.districtId())
                .stateId(request.stateId())
                .publishedAt(resolvedPublishedAt)
                .expiresAt(request.expiresAt())
                .pinned(Boolean.TRUE.equals(request.pinned()))
                .tags(request.tags())
                .createdByUserId(userId)
                .updatedByUserId(userId)
                .build();

        Announcement saved = announcementRepository.save(announcement);

        log.info("announcement_created id={} status={} scope={} gbId={}",
                saved.getId(), saved.getStatus(), saved.getTargetScope(), governingBodyId);

        return mapper.toResponse(saved);
    }

    /**
     * Returns all announcements for the operator's governing body.
     * Includes all statuses (DRAFT, PUBLISHED, ARCHIVED, EXPIRED).
     * Pinned announcements appear first, then newest-first.
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list() {
        return announcementRepository
                .findByGoverningBodyIdOrderByPinnedDescCreatedAtDesc(TenantContext.get())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    /**
     * Returns a single announcement scoped to the operator's governing body.
     */
    @Transactional(readOnly = true)
    public AnnouncementResponse get(UUID id) {
        return mapper.toResponse(findTenantScoped(id));
    }

    /**
     * Public civic announcement feed — citizen-safe, no TenantContext required.
     *
     * This endpoint is permitAll — it must NEVER call TenantContext.get().
     *
     * Dual-mode based on TenantContext availability:
     *
     *   Operator context (TenantContext is bound — platform JWT with governing_body_id):
     *     Scoped to the governing body. With wardId: ward + broad-scope announcements.
     *     Without wardId: broad-scope (CITY/DISTRICT/STATE) announcements only.
     *
     *   Community/citizen context (TenantContext not bound — Supabase JWT or no auth):
     *     Geography-primary query — no governing body filter. Supports the ward-led MVP
     *     where a governing body may not yet be onboarded.
     *     With wardId: returns WARD-scoped announcements for that ward only.
     *       (City/district/state are unknown here — use GET /my-feed for the full
     *        profile-personalised set with all scope levels.)
     *     Without wardId: no geography context — returns empty list.
     *
     * ZONE is excluded pending the AudienceResolverService.
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getPublicFeed(UUID wardId) {
        UUID governingBodyId = TenantContext.getOrNull();  // citizen-safe: never throws
        Instant now = Instant.now();
        AnnouncementStatus published = AnnouncementStatus.PUBLISHED;

        List<Announcement> feed;
        if (governingBodyId != null) {
            // Operator path — governing body scoped.
            feed = (wardId != null)
                    ? announcementRepository.findFeedByGoverningBodyIdAndWardId(
                            governingBodyId, wardId, BROAD_SCOPES, published, now)
                    : announcementRepository.findFeedByGoverningBodyId(
                            governingBodyId, BROAD_SCOPES, published, now);
        } else {
            // Community/citizen path — no governing body required.
            // city/district/state are null (unknown without a profile) so only
            // WARD-scoped announcements will be matched when wardId is provided.
            if (wardId == null) {
                log.debug("announcement_public_feed reason=no_ward_no_tenant returning_empty");
                return List.of();
            }
            feed = announcementRepository.findFeedForCitizenByGeography(
                    wardId, null, null, null, published, now);
        }

        log.debug("announcement_feed_served gbId={} wardId={} count={}",
                governingBodyId, wardId, feed.size());
        return feed.stream().map(mapper::toResponse).toList();
    }

    /**
     * Returns the personalised civic announcement feed for the authenticated citizen.
     *
     * Geography is resolved entirely from the citizen's stored user_profile — the client
     * sends zero geography parameters. This prevents client-side targeting manipulation
     * and ensures the backend is the single source of truth for civic jurisdiction.
     *
     * Matching rules (any match qualifies the announcement):
     *   WARD     → announcement.ward_id     = profile.ward_uuid
     *   CITY     → announcement.city_id     = profile.city_uuid
     *   DISTRICT → announcement.district_id = profile.district_uuid
     *   STATE    → announcement.state_id    = profile.state_uuid
     *
     * A null profile geography FK matches nothing — no special-casing required.
     * ZONE targeting is excluded pending the AudienceResolverService implementation.
     *
     * No governing_body_id filter is applied — citizens see all published announcements
     * in their geography regardless of which issuing authority created them. This supports
     * ward-led pilot mode where citizens may not be linked to a governing body.
     */
    @Transactional(readOnly = true)
    public List<CitizenFeedResponse> getMyFeed() {
        UUID userId = currentUserProvider.getCurrentUserId();

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Civic profile not found. Please complete onboarding before accessing the feed."));

        List<Announcement> feed = announcementRepository.findFeedForCitizenByGeography(
                profile.getWardUuid(),
                profile.getCityUuid(),
                profile.getDistrictUuid(),
                profile.getStateUuid(),
                AnnouncementStatus.PUBLISHED,
                Instant.now());

        log.debug("citizen_feed_served userId={} wardUuid={} count={}",
                userId, profile.getWardUuid(), feed.size());

        return feed.stream().map(mapper::toCitizenResponse).toList();
    }

    private Announcement findTenantScoped(UUID id) {
        return announcementRepository.findByIdAndGoverningBodyId(id, TenantContext.get())
                .orElseThrow(() -> new AnnouncementNotFoundException(id));
    }

    private AnnouncementStatus resolveStatus(AnnouncementStatus requested) {
        if (requested == null || requested == AnnouncementStatus.DRAFT) {
            return AnnouncementStatus.DRAFT;
        }
        if (requested == AnnouncementStatus.PUBLISHED) {
            return AnnouncementStatus.PUBLISHED;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "status must be DRAFT or PUBLISHED on creation; ARCHIVED and EXPIRED are system-managed");
    }

    /**
     * Validates that exactly the geo FK matching targetScope is provided and all
     * others are null. Returns cleanly on success; throws HTTP 400 on violation.
     */
    private void validateTargeting(AnnouncementCreateRequest req) {
        AnnouncementTargetScope scope = req.targetScope();
        UUID wardId     = req.wardId();
        UUID zoneId     = req.zoneId();
        UUID cityId     = req.cityId();
        UUID districtId = req.districtId();
        UUID stateId    = req.stateId();

        switch (scope) {
            case WARD -> {
                requirePresent("ward_id", wardId, scope);
                requireAbsent("zone_id",     zoneId,     scope);
                requireAbsent("city_id",     cityId,     scope);
                requireAbsent("district_id", districtId, scope);
                requireAbsent("state_id",    stateId,    scope);
            }
            case ZONE -> {
                requirePresent("zone_id", zoneId, scope);
                requireAbsent("ward_id",     wardId,     scope);
                requireAbsent("city_id",     cityId,     scope);
                requireAbsent("district_id", districtId, scope);
                requireAbsent("state_id",    stateId,    scope);
            }
            case CITY -> {
                requirePresent("city_id", cityId, scope);
                requireAbsent("ward_id",     wardId,     scope);
                requireAbsent("zone_id",     zoneId,     scope);
                requireAbsent("district_id", districtId, scope);
                requireAbsent("state_id",    stateId,    scope);
            }
            case DISTRICT -> {
                requirePresent("district_id", districtId, scope);
                requireAbsent("ward_id", wardId, scope);
                requireAbsent("zone_id", zoneId, scope);
                requireAbsent("city_id", cityId, scope);
                requireAbsent("state_id", stateId, scope);
            }
            case STATE -> {
                requirePresent("state_id", stateId, scope);
                requireAbsent("ward_id",     wardId,     scope);
                requireAbsent("zone_id",     zoneId,     scope);
                requireAbsent("city_id",     cityId,     scope);
                requireAbsent("district_id", districtId, scope);
            }
        }
    }

    private void requirePresent(String fieldName, UUID value, AnnouncementTargetScope scope) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " is required when target_scope is " + scope);
        }
    }

    private void requireAbsent(String fieldName, UUID value, AnnouncementTargetScope scope) {
        if (value != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must be null when target_scope is " + scope);
        }
    }
}

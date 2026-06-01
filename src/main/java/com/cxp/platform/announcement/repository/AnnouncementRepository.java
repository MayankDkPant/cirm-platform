package com.cxp.platform.announcement.repository;

import com.cxp.platform.announcement.domain.Announcement;
import com.cxp.platform.announcement.domain.AnnouncementStatus;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    // ── Operator / admin queries ──────────────────────────────────────────────

    /** All announcements for a governing body — operator dashboard view, all statuses. */
    List<Announcement> findByGoverningBodyIdOrderByPinnedDescCreatedAtDesc(UUID governingBodyId);

    /** Tenant-scoped lookup — prevents cross-tenant data access. */
    Optional<Announcement> findByIdAndGoverningBodyId(UUID id, UUID governingBodyId);

    // ── Civic feed queries ────────────────────────────────────────────────────
    // All announcements are public civic communications by design.
    // No is_public pre-filter is needed — status = PUBLISHED is the authoritative gate.
    //
    // The audience-resolver service will wrap these with citizen-profile filtering
    // (verified address, zone membership) when that feature is implemented.

    /**
     * General civic feed: all published, active, non-expired announcements for the
     * governing body at broad geographic scopes (CITY, DISTRICT, STATE).
     *
     * Used when the citizen's ward is unknown. Returns city-wide and above — no
     * ward-specific or zone-specific announcements are included since the citizen's
     * precise location is unresolved.
     */
    @Query("""
            SELECT a FROM Announcement a
            WHERE a.governingBodyId = :gbId
              AND a.status         = :status
              AND a.isActive       = true
              AND a.targetScope    IN :broadScopes
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
            ORDER BY a.pinned DESC, a.publishedAt DESC
            """)
    List<Announcement> findFeedByGoverningBodyId(
            @Param("gbId")        UUID governingBodyId,
            @Param("broadScopes") List<AnnouncementTargetScope> broadScopes,
            @Param("status")      AnnouncementStatus status,
            @Param("now")         Instant now);

    /**
     * Ward-scoped civic feed: includes the citizen's ward-specific announcements
     * plus all broader-scope announcements (CITY, DISTRICT, STATE).
     *
     * ZONE-level announcements are not included in this query because resolving which
     * zone a ward belongs to requires an additional lookup; this is deferred to the
     * audience-resolver service in a future iteration.
     *
     * broadScopes should be [CITY, DISTRICT, STATE] — passed by the caller so the
     * repository stays stateless and the caller controls the scope ladder.
     */
    @Query("""
            SELECT a FROM Announcement a
            WHERE a.governingBodyId = :gbId
              AND a.status         = :status
              AND a.isActive       = true
              AND (a.wardId = :wardId OR a.targetScope IN :broadScopes)
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
            ORDER BY a.pinned DESC, a.publishedAt DESC
            """)
    List<Announcement> findFeedByGoverningBodyIdAndWardId(
            @Param("gbId")        UUID governingBodyId,
            @Param("wardId")      UUID wardId,
            @Param("broadScopes") List<AnnouncementTargetScope> broadScopes,
            @Param("status")      AnnouncementStatus status,
            @Param("now")         Instant now);

    /**
     * Authenticated citizen feed: returns all published, active, non-expired announcements
     * that match any geography dimension carried in the citizen's user profile.
     *
     * Each targetScope arm is matched against the corresponding profile FK:
     *   WARD     → wardId     must equal the citizen's ward
     *   CITY     → cityId     must equal the citizen's city
     *   DISTRICT → districtId must equal the citizen's district
     *   STATE    → stateId    must equal the citizen's state
     *
     * Null profile FKs match nothing safely — a citizen whose profile lacks a
     * districtUuid will simply receive no DISTRICT announcements.
     *
     * ZONE is excluded here; zone resolution (which zone a ward belongs to) is
     * deferred to the future AudienceResolverService.
     */
    @Query("""
            SELECT a FROM Announcement a
            WHERE a.governingBodyId = :gbId
              AND a.status          = :status
              AND a.isActive        = true
              AND (
                  (a.targetScope = 'WARD'     AND a.wardId     = :wardId)
               OR (a.targetScope = 'CITY'     AND a.cityId     = :cityId)
               OR (a.targetScope = 'DISTRICT' AND a.districtId = :districtId)
               OR (a.targetScope = 'STATE'    AND a.stateId    = :stateId)
              )
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
            ORDER BY a.pinned DESC, a.publishedAt DESC
            """)
    List<Announcement> findFeedForCitizen(
            @Param("gbId")        UUID governingBodyId,
            @Param("wardId")      UUID wardId,
            @Param("cityId")      UUID cityId,
            @Param("districtId")  UUID districtId,
            @Param("stateId")     UUID stateId,
            @Param("status")      AnnouncementStatus status,
            @Param("now")         Instant now);

    /**
     * Geography-primary citizen feed — no governing body filter.
     *
     * Used by authenticated citizen endpoints where the JWT may carry no governing_body_id
     * claim (ward-led pilot mode, or onboarding not yet linked to a governing body).
     * The citizen's ward/city/district/state from user_profile are sufficient to target
     * relevant announcements from any issuing authority in their geography.
     *
     * ZONE is excluded here; zone resolution is deferred to the future AudienceResolverService.
     */
    @Query("""
            SELECT a FROM Announcement a
            WHERE a.status   = :status
              AND a.isActive = true
              AND (
                  (a.targetScope = 'WARD'     AND a.wardId     = :wardId)
               OR (a.targetScope = 'CITY'     AND a.cityId     = :cityId)
               OR (a.targetScope = 'DISTRICT' AND a.districtId = :districtId)
               OR (a.targetScope = 'STATE'    AND a.stateId    = :stateId)
              )
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
            ORDER BY a.pinned DESC, a.publishedAt DESC
            """)
    List<Announcement> findFeedForCitizenByGeography(
            @Param("wardId")      UUID wardId,
            @Param("cityId")      UUID cityId,
            @Param("districtId")  UUID districtId,
            @Param("stateId")     UUID stateId,
            @Param("status")      AnnouncementStatus status,
            @Param("now")         Instant now);

    // ── Future queries (hooks for upcoming features) ──────────────────────────
    // findByGoverningBodyIdAndStatus         — scheduled expiry sweep
    // findByTagsContaining                   — topic-following feed (once tags → JSONB)
    // findByGoverningBodyIdAndPriority       — urgent/critical notification routing
    // findByGoverningBodyIdAndZoneId         — zone-scoped feed (post audience-resolver)
    // findByTargetScopeAndCityId             — cross-tenant city-level feed aggregation
}

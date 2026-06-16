package com.cxp.platform.announcement.domain;

import com.cxp.platform.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "announcement",
        indexes = {
                @Index(name = "idx_ann_tenant_created", columnList = "governing_body_id, created_at"),
                @Index(name = "idx_ann_target_scope",   columnList = "governing_body_id, target_scope")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement extends BaseEntity {

    // ── Core content ──────────────────────────────────────────────────────────

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String summary;

    @Column(length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AnnouncementPriority priority = AnnouncementPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    // ── Governance ────────────────────────────────────────────────────────────

    /** Issuing authority — derived from the operator's JWT tenant at creation. */
    @Column(name = "governing_body_id", nullable = false)
    private UUID governingBodyId;

    // ── Geographic targeting ──────────────────────────────────────────────────
    // targetScope records the geographic audience level chosen by the operator.
    // Exactly ONE of the five FK columns below must be non-null, matching the scope.
    //
    // All announcements are public civic communications — no PRIVATE concept.
    // The scope here is purely about geographic reach, not access control.
    //
    // FUTURE: when multi-ward or multi-city targeting is required, the single-FK
    // model will be replaced by an announcement_target junction table, and
    // targetScope will describe the type of targets rather than imply a single FK.

    @Enumerated(EnumType.STRING)
    @Column(name = "target_scope", nullable = false, length = 30)
    private AnnouncementTargetScope targetScope;

    /** Populated when targetScope = WARD. */
    @Column(name = "ward_id")
    private UUID wardId;

    /** Populated when targetScope = ZONE. */
    @Column(name = "zone_id")
    private UUID zoneId;

    /** Populated when targetScope = CITY. */
    @Column(name = "city_id")
    private UUID cityId;

    /** Populated when targetScope = DISTRICT. */
    @Column(name = "district_id")
    private UUID districtId;

    /** Populated when targetScope = STATE. */
    @Column(name = "state_id")
    private UUID stateId;

    // ── Publishing lifecycle ──────────────────────────────────────────────────

    @Column(name = "published_at")
    private Instant publishedAt;

    /** Null = no expiry. A scheduled sweep transitions status to EXPIRED when passed. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean pinned = false;

    // ── Future extensibility ──────────────────────────────────────────────────
    // Stored as TEXT (plain JSON string). Ready to migrate to JSONB for GIN indexing.

    /**
     * Flexible JSON object store for future extensions:
     * multilingual translations, AI-generated summaries, media attachment refs,
     * reaction aggregates, CTA links, etc.
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;

    /**
     * JSON array of topic labels, e.g. {@code ["water", "road", "safety"]}.
     * Used for topic-following, notification routing, and civic engagement analytics.
     */
    @Column(columnDefinition = "TEXT")
    private String tags;

    // ── Actor audit ───────────────────────────────────────────────────────────

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;
}

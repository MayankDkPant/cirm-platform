package com.cxp.platform.servicerequest.entity;

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
        name = "service_request",
        indexes = {
                @Index(name = "idx_service_request_type", columnList = "type"),
                @Index(name = "idx_service_request_status", columnList = "status"),
                @Index(name = "idx_service_request_tenant_created", columnList = "governing_body_id, created_at"),
                @Index(name = "idx_sr_sync_status_attempts", columnList = "external_sync_status, external_sync_attempts")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceRequestType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // VARCHAR(150) in DB — enum enforced at application layer, no schema migration needed.
    @Enumerated(EnumType.STRING)
    @Column(length = 150)
    private ServiceRequestCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ServiceRequestPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ServiceRequestStatus status;

    // ── Location fields — geo taxonomy ───────────────────────────────────────
    // USER-SUPPLIED TEXT: raw address from citizen; not geocoded, not normalized.
    @Column(name = "address_text", columnDefinition = "TEXT")
    private String addressText;

    // GPS COORDINATE (WGS-84, DOUBLE PRECISION): client-supplied at submission time.
    // Null when no coordinates provided. Note: UserProfile stores as DECIMAL(10,8)/(11,8) —
    // types differ but precision is equivalent for real-world GPS accuracy.
    private Double latitude;
    private Double longitude;

    // CANONICAL ID: routing target derived from GPS → ward-locator → governing body FK.
    @Column(name = "department_id")
    private UUID departmentId;

    // CANONICAL ID: authoritative governing body UUID. FK-derived, NOT from JWT/client.
    @Column(name = "governing_body_id")
    private UUID governingBodyId;

    // CANONICAL ID: ward resolved from GPS via ward-locator. FK → ward(id) ON DELETE SET NULL.
    @Column(name = "ward_id")
    private UUID wardId;

    // DENORMALIZED LABEL: ward display name cached at creation time from ward-locator.
    // May become stale if governance data is renamed. Join via ward_id for authoritative name.
    @Column(name = "ward_name", length = 255)
    private String wardName;

    // DENORMALIZED LABEL: always null in the current implementation. Ward-locator returns
    // wardNo + wardName only — no formatted address. Will populate once geocoding is added.
    @Column(name = "formatted_address", length = 500)
    private String formattedAddress;

    @Column(name = "ai_conversation_id")
    private UUID aiConversationId;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceRequestSource source;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    // --- Visibility ---
    // visibilityScope persists the citizen's stated intent for audience scope.
    // isPublic is a derived convenience flag (true when scope != PRIVATE) that
    // enables cheap boolean filtering in civic-feed queries.
    // Neither field directly grants access — the runtime audience-resolver service
    // interprets them alongside ward_id, governing_body_id, and citizen profile.

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility_scope", nullable = false, length = 50)
    @Builder.Default
    private ServiceRequestVisibility visibilityScope = ServiceRequestVisibility.PRIVATE;

    // --- External sync ---

    @Enumerated(EnumType.STRING)
    @Column(name = "external_sync_status", nullable = false, length = 30)
    private ServiceRequestExternalSyncStatus externalSyncStatus;

    @Column(name = "external_sync_attempts", nullable = false)
    @Builder.Default
    private Integer externalSyncAttempts = 0;

    @Column(name = "external_sync_last_attempt_at")
    private Instant externalSyncLastAttemptAt;

    @Column(name = "external_sync_error", columnDefinition = "TEXT")
    private String externalSyncError;

    // --- Idempotency ---

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    // --- Actor audit ---

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "updated_by_user_id")
    private UUID updatedByUserId;
}

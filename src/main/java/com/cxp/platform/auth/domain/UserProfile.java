package com.cxp.platform.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Civic profile of a citizen — the enrichment layer on top of the identity account.
 *
 * Intentionally separate from {@link User}:
 *   - User        = authentication identity (stable, auth-provider-agnostic)
 *   - UserProfile = civic profile (address, ward affiliation, geography)
 *
 * Geography columns (ward_uuid, zone_uuid, city_uuid, district_uuid, state_uuid)
 * are populated by the provisioning service during onboarding via address geocoding
 * and the ward-locator lookup. The frontend must never derive or send these values.
 *
 * Audit timestamps:
 *   createdAt — set once on INSERT by @PrePersist; never updated thereafter (updatable=false).
 *   updatedAt — set on INSERT and refreshed on every UPDATE by @PrePersist/@PreUpdate.
 *   Both are managed automatically — callers must never set them manually.
 *   Columns exist in the DB since V8; they are made NOT NULL by V51 after backfilling nulls.
 *
 * Does NOT extend BaseEntity because user_profile has no is_active column.
 * The @PrePersist / @PreUpdate pattern mirrors BaseEntity semantics.
 *
 * Schema: user_profile (V8 baseline, extended by V18, V45, V46, V51).
 */
@Entity
@Table(name = "user_profile")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ── Identity link ─────────────────────────────────────────────────────────

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    // ── Personal info (citizen-supplied at onboarding) ────────────────────────

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "phone_number", length = 50)
    private String phoneNumber;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    // ── Civic geography (backend-derived, never client-supplied) ──────────────
    // Populated by the provisioning service after geocoding + ward-locator lookup.
    // Nullable: a profile is created before geography is resolved; fields are
    // backfilled once the address resolves to a known governance hierarchy node.

    @Column(name = "ward_uuid")
    private UUID wardUuid;

    @Column(name = "zone_uuid")
    private UUID zoneUuid;

    @Column(name = "city_uuid")
    private UUID cityUuid;

    @Column(name = "district_uuid")
    private UUID districtUuid;

    @Column(name = "state_uuid")
    private UUID stateUuid;

    // Column name kept as municipality_uuid for schema compatibility (V8/V45).
    // Field renamed to governingBodyUuid to match the authoritative term used everywhere else.
    @Column(name = "municipality_uuid")
    private UUID governingBodyUuid;

    // ── Resolved location (backend-stored, from geocoding result) ────────────

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    /** DENORMALIZED LABEL — cached from ward-locator at provisioning time. May become stale. Join via wardUuid for authoritative name. */
    @Column(name = "ward_name", length = 255)
    private String wardName;

    /**
     * USER-SUPPLIED TEXT — not geocoded. Stores the citizen's raw address input from provisioning.
     * Despite the name implying normalization, this is the free-text address the citizen typed.
     * Will become geocoder-normalized once a geocoding integration is added.
     * DB comment corrected in V54.
     */
    @Column(name = "address_normalized", length = 500)
    private String addressNormalized;

    // ── Verification flags ────────────────────────────────────────────────────

    @Column(name = "is_phone_verified")
    @Builder.Default
    private boolean phoneVerified = false;

    @Column(name = "is_email_verified")
    @Builder.Default
    private boolean emailVerified = false;

    // ── Audit timestamps ──────────────────────────────────────────────────────
    // Managed automatically by @PrePersist and @PreUpdate — never set by callers.
    // Columns exist since V8; V51 backfills nulls and adds NOT NULL.

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── Convenience ───────────────────────────────────────────────────────────

    /** True when the profile has a resolved ward — i.e. onboarding is complete. */
    public boolean hasWard() {
        return wardUuid != null;
    }
}

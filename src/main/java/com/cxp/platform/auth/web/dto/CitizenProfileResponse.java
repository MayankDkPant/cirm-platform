package com.cxp.platform.auth.web.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Citizen civic profile returned by GET /api/v1/users/me. " +
                      "Combines auth identity (from User) and civic profile (from UserProfile). " +
                      "The deprecated appUserId and postalCode aliases are emitted alongside canonical fields " +
                      "for backward compatibility.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenProfileResponse {

    // ── Auth identity (from User) ─────────────────────────────────────────────

    @Schema(description = "Canonical platform user identity (User.id). " +
                          "Stable across all profile updates — not the UserProfile resource id.")
    private UUID id;

    @Schema(description = "Citizen email address sourced from the auth identity record.", nullable = true)
    private String email;

    @Schema(description = "Platform role: CITIZEN or OPERATOR. Not a Spring Security authority name.")
    private String role;

    @Schema(description = "Account creation timestamp. Citizen-safe membership signal.")
    private Instant memberSince;

    // ── Civic profile (from UserProfile) ─────────────────────────────────────

    @Schema(description = "Citizen's full name from the civic profile.", nullable = true)
    private String fullName;

    @Schema(description = "Citizen's mobile number used for OTP authentication.", nullable = true)
    private String mobileNumber;

    @Schema(description = "Free-text address line from the civic profile.", nullable = true)
    private String addressLine;

    @Schema(description = "Postal / PIN code (Indian convention). Stored as postalCode internally; " +
                          "exposed under the canonical mobile-facing name pinCode.",
            nullable = true)
    private String pinCode;

    // ── Governance hierarchy — canonical IDs ──────────────────────────────────
    // Authoritative references into the governance discovery APIs.
    // Null when the citizen has not completed onboarding (no ward resolved).

    @Schema(description = "Canonical ward identifier. Authoritative reference into GET /governing-bodies/{id}/wards. " +
                          "Null when onboarding is incomplete.", nullable = true)
    private String wardId;

    @Schema(description = "Denormalized ward display label. Use for display only; wardId is authoritative.",
            nullable = true)
    private String wardName;

    @Schema(description = "Canonical governing authority identifier for this citizen's ward.", nullable = true)
    private String governingBodyId;

    @Schema(description = "Canonical city identifier resolved via the governance hierarchy.", nullable = true)
    private String cityId;

    @Schema(description = "Canonical district identifier resolved via the governance hierarchy.", nullable = true)
    private String districtId;

    @Schema(description = "Canonical state identifier resolved via the governance hierarchy.", nullable = true)
    private String stateId;

    // ── Governance hierarchy — derived display labels ────────────────────────
    // Convenience labels derived server-side from the canonical IDs.
    // Use canonical IDs for programmatic navigation; use labels for display only.

    @Schema(description = "Denormalized city display label. Use for display only.", nullable = true)
    private String city;

    @Schema(description = "Denormalized district display label. Use for display only.", nullable = true)
    private String district;

    @Schema(description = "Denormalized state display label. Use for display only.", nullable = true)
    private String state;

    @Schema(description = "GPS latitude from the citizen's civic profile.", nullable = true)
    private Double latitude;

    @Schema(description = "GPS longitude from the citizen's civic profile.", nullable = true)
    private Double longitude;

    @Schema(description = "True when the citizen has a verified phone or email. " +
                          "False for all users until a verification flow runs.")
    private boolean isVerified;

    @Schema(description = "True when the citizen has completed the onboarding flow (ward resolved).")
    private boolean onboardingComplete;

    @Schema(description = "Sub-locality or neighbourhood within the ward. " +
                          "Deferred — always null until address normalization is extended.",
            nullable = true)
    private String locality;

    @Schema(description = "Citizen profile photo URL. " +
                          "Deferred — always null until a media service is implemented.",
            nullable = true)
    private String profileImageUrl;

    // ── Transitional backward-compat aliases ─────────────────────────────────

    /** @deprecated use {@code id} — will be removed in a future release */
    @Deprecated
    @Schema(description = "DEPRECATED — use id. " +
                          "Backward-compatibility alias retained for one release cycle. " +
                          "Will be removed in a future release.",
            deprecated = true)
    @JsonGetter("appUserId")
    public UUID getAppUserId() { return id; }

    /** @deprecated use {@code pinCode} — will be removed in a future release */
    @Deprecated
    @Schema(description = "DEPRECATED — use pinCode. " +
                          "Backward-compatibility alias retained for one release cycle. " +
                          "Will be removed in a future release.",
            deprecated = true, nullable = true)
    @JsonGetter("postalCode")
    public String getPostalCode() { return pinCode; }
}

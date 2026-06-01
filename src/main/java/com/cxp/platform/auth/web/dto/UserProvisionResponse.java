package com.cxp.platform.auth.web.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Schema(description = "Response from POST /api/v1/users/provision. " +
                      "Returned on both first-run provisioning and subsequent profile updates. " +
                      "The deprecated appUserId alias is emitted alongside id for backward compatibility.")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProvisionResponse {

    @Schema(description = "Canonical platform user identity (User.id). " +
                          "Stable across all profile updates. Persist this as the durable citizen identifier.")
    private UUID id;

    // ── Resolved governance hierarchy ─────────────────────────────────────────
    // wardId and governingBodyId are canonical persisted identifiers.
    // wardName, city, district, state are denormalized display labels returned for FE convenience.

    @Schema(description = "Canonical ward identifier resolved for this citizen. " +
                          "Authoritative reference into GET /governing-bodies/{id}/wards.", nullable = true)
    private String wardId;

    @Schema(description = "Denormalized ward display label. Use for display only; wardId is the authoritative reference.",
            nullable = true)
    private String wardName;

    @Schema(description = "Zone identifier within the ward hierarchy. Null when zone resolution is not configured.",
            nullable = true)
    private String zoneId;

    @Schema(description = "Canonical governing authority identifier for this citizen's ward.", nullable = true)
    private String governingBodyId;

    @Schema(description = "Canonical city identifier resolved via the governance hierarchy.", nullable = true)
    private String cityId;

    @Schema(description = "Denormalized city display label. Use for display only.", nullable = true)
    private String city;

    @Schema(description = "Canonical district identifier resolved via the governance hierarchy.", nullable = true)
    private String districtId;

    @Schema(description = "Denormalized district display label. Use for display only.", nullable = true)
    private String district;

    @Schema(description = "Canonical state identifier resolved via the governance hierarchy.", nullable = true)
    private String stateId;

    @Schema(description = "Denormalized state display label. Use for display only.", nullable = true)
    private String state;

    @Schema(description = "True when the citizen has completed the onboarding flow (ward resolved).")
    private boolean onboardingComplete;

    // ── Transitional backward-compat aliases ─────────────────────────────────

    /** @deprecated use {@code id} — will be removed in a future release */
    @Deprecated
    @Schema(description = "DEPRECATED — use id. " +
                          "Backward-compatibility alias retained for one release cycle. " +
                          "Will be removed in a future release.",
            deprecated = true)
    @JsonGetter("appUserId")
    public UUID getAppUserId() { return id; }
}

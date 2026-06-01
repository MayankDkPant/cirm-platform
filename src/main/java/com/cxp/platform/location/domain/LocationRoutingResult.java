package com.cxp.platform.location.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Geo field taxonomy for this result object:
 *
 *   CANONICAL IDs (authoritative, DB-derived):
 *     governingBodyId — derived from Ward → GoverningBody FK in the local DB.
 *                       This is the authoritative source — never trust municipalityId
 *                       from the external ward-locator service over this value.
 *     wardId          — UUID resolved from the ward-locator name → local ward table lookup.
 *
 *   DENORMALIZED LABELS (cached convenience, may become stale):
 *     wardName        — display name from the ward-locator service.
 *     formattedAddress — ALWAYS NULL in the current implementation. The ward-locator
 *                       returns wardNo + wardName only. Will be populated once a geocoding
 *                       pass is added to DefaultLocationIntelligenceService.
 *
 *   LEGACY / DEPRECATED (do not use in new code):
 *     municipalityId   — legacy alias for governingBodyId, sourced from the external
 *                        ward-locator service via WardLookupResult. No longer read by
 *                        any downstream service (CitizenProvisioningService uses
 *                        governingBodyId). Retained for backward compatibility only.
 *     municipalityName — never populated in the HTTP path (WardLocatorResponse has no
 *                        municipality name field). Populated only in MockWardLookupService.
 *
 *   GPS COORDINATES (optional, client-supplied):
 *     resolvedLatitude / resolvedLongitude — echo of the client-supplied GPS coordinates.
 *                       Carried back so callers can persist without re-deriving.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRoutingResult {

    /**
     * Authoritative governing body UUID derived from the Ward → GoverningBody FK in the
     * local DB. Set by DefaultLocationIntelligenceService.resolveGoverningBodyId().
     * Null when ward lookup fails or coordinates cannot be mapped to a jurisdiction.
     */
    private UUID governingBodyId;

    /** @deprecated Legacy alias for governingBodyId from the external ward-locator service. Use governingBodyId. */
    @Deprecated
    private UUID municipalityId;

    /** @deprecated Never populated in the HTTP ward-locator path. Use governingBodyId for authoritative identity. */
    @Deprecated
    private String municipalityName;

    private UUID wardId;
    private String wardName;

    /**
     * Always null in the current implementation — ward-locator returns wardNo + wardName only.
     * Will carry geocoder-formatted address once that integration is added.
     */
    private String formattedAddress;

    /** GPS latitude echoed from client input. Null when no coordinates were provided. */
    private Double resolvedLatitude;

    /** GPS longitude echoed from client input. Null when no coordinates were provided. */
    private Double resolvedLongitude;
}

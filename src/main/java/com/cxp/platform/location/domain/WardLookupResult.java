package com.cxp.platform.location.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Raw result of a GIS ward lookup from coordinates.
 *
 * municipalityId / municipalityName are legacy field names for the governing body —
 * they were introduced before the internal terminology converged on "governing body."
 * These values are populated by HttpWardLookupService but are NOT read by any downstream
 * service (DefaultLocationIntelligenceService derives governingBodyId independently from
 * the local WardRepository to ensure DB-level authority). Retained for backward compatibility.
 *
 * wardId / wardName are the authoritative output of this result.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardLookupResult {

    /** @deprecated Legacy name for governing body ID. Not read downstream — use wardId and let callers derive governingBodyId from the local DB. */
    @Deprecated
    private UUID municipalityId;

    /** @deprecated Never populated in the HTTP path (WardLocatorResponse has no municipality name). */
    @Deprecated
    private String municipalityName;

    private UUID wardId;
    private String wardName;
}

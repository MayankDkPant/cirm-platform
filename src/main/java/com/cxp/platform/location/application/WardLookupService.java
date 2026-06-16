package com.cxp.platform.location.application;

import com.cxp.platform.location.domain.WardLookupResult;

/**
 * Resolves ward jurisdiction from GPS coordinates using internally stored GIS boundary data.
 *
 * Terminology note: "municipality" in this interface and its implementations is legacy naming
 * for "governing body" — the platform canonical term introduced after the location module was
 * originally written. Internal field names (municipalityId, municipalityName) in WardLookupResult
 * and LocationRoutingResult are deprecated internal aliases; they are not exposed in the HTTP API.
 * The authoritative governing body identifier is always derived from the local DB (GoverningBody FK),
 * never from the external ward-locator service.
 */
public interface WardLookupService {

    WardLookupResult findWardByCoordinates(Double latitude, Double longitude);
}

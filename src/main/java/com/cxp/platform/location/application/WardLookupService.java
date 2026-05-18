package com.cxp.platform.location.application;

import com.cxp.platform.location.domain.WardLookupResult;

/**
 * Application service orchestrating use cases for the module.
 *
 * Resolves municipality and ward information from coordinates using
 * internally stored GIS boundary data.
 */
public interface WardLookupService {

    WardLookupResult findWardByCoordinates(Double latitude, Double longitude);
}

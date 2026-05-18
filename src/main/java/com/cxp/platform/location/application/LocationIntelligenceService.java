package com.cxp.platform.location.application;

import com.cxp.platform.location.domain.LocationRoutingRequest;
import com.cxp.platform.location.domain.LocationRoutingResult;

/**
 * Application service orchestrating use cases for the module.
 */
public interface LocationIntelligenceService {

    LocationRoutingResult resolveRouting(LocationRoutingRequest request);
}

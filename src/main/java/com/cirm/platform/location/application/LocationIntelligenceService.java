package com.cirm.platform.location.application;

import com.cirm.platform.location.domain.LocationRoutingRequest;
import com.cirm.platform.location.domain.LocationRoutingResult;

/**
 * Application service orchestrating use cases for the module.
 */
public interface LocationIntelligenceService {

    LocationRoutingResult resolveRouting(LocationRoutingRequest request);
}

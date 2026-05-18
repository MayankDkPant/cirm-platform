package com.cxp.platform.location.application;

import com.cxp.platform.location.domain.WardLookupResult;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Application service orchestrating use cases for the module.
 *
 * Temporary stub implementation for ward lookup until GIS polygon-based
 * boundary resolution is implemented.
 */
@Service
public class MockWardLookupService implements WardLookupService {

    @Override
    public WardLookupResult findWardByCoordinates(Double latitude, Double longitude) {
           return WardLookupResult.builder()
            .municipalityId(UUID.fromString("58724ce3-e5ff-4ab1-83af-a1a1c96bccac"))
            .municipalityName("Dehradun Municipal Corporation")
            .wardId(UUID.fromString("7851a83b-8573-407c-834f-5a0945bf46d0"))
            .wardName("Ballupur Ward")
            .build();
    }
}



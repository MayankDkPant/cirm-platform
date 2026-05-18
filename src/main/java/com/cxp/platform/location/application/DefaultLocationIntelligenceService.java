package com.cxp.platform.location.application;

import com.cxp.platform.location.port.GeocodingClient;
import com.cxp.platform.location.port.GeocodingResult;
import com.cxp.platform.location.domain.LocationRoutingRequest;
import com.cxp.platform.location.domain.LocationRoutingResult;
import com.cxp.platform.location.domain.WardLookupResult;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Application service orchestrating use cases for the module.
 *
 * Temporary orchestration service that combines geocoding and ward lookup
 * to produce routing metadata until full GIS and routing rules are implemented.
 * Ward lookup is optional and may be unavailable for some municipalities
 * during onboarding phases.
 */
@Service
public class DefaultLocationIntelligenceService implements LocationIntelligenceService {

    private final GeocodingClient geocodingClient;
    private final Optional<WardLookupService> wardLookupService;

    public DefaultLocationIntelligenceService(
            GeocodingClient geocodingClient,
            Optional<WardLookupService> wardLookupService) {
        this.geocodingClient = geocodingClient;
        this.wardLookupService = wardLookupService;
    }

    /**
     * Orchestrates reverse geocoding and ward lookup to resolve
     * civic location context from complaint coordinates.
     */
    @Override
    public LocationRoutingResult resolveRouting(LocationRoutingRequest request) {
        GeocodingResult geo = geocodingClient.reverseGeocode(
                request.getLatitude(),
                request.getLongitude()
        );

        WardLookupResult ward = wardLookupService
                .map(service ->
                        service.findWardByCoordinates(
                                request.getLatitude(),
                                request.getLongitude()
                        )
                )
                .orElse(null);

        return LocationRoutingResult.builder()
                .municipalityId(ward != null ? ward.getMunicipalityId() : null)
                .municipalityName(ward != null ? ward.getMunicipalityName() : null)
                .wardId(ward != null ? ward.getWardId() : null)
                .wardName(ward != null ? ward.getWardName() : null)
                .formattedAddress(geo.getFormattedAddress())
                .build();
    }
}

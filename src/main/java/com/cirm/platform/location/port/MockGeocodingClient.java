package com.cirm.platform.location.port;

import org.springframework.stereotype.Component;

/**
 * Temporary stub implementation of {@link GeocodingClient} used during development.
 * This component returns deterministic mocked geocoding responses until real geocoding
 * provider integration is implemented.
 */
@Component
public class MockGeocodingClient implements GeocodingClient {

    @Override
    public GeocodingResult forwardGeocode(String text) {
        return GeocodingResult.builder()
                .latitude(30.3250)
                .longitude(78.0410)
                .formattedAddress("Ballupur, Dehradun")
                .confidenceScore(0.8)
                .build();
    }

    @Override
    public GeocodingResult reverseGeocode(Double latitude, Double longitude) {
    return GeocodingResult.builder()
            .latitude(30.3165)
            .longitude(78.0322)
            .formattedAddress("Ballupur Chowk, Dehradun")
            .confidenceScore(0.9)
            .build();
    }
}


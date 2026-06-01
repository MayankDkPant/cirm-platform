package com.cxp.platform.location.port;

/**
 * Stub implementation of {@link GeocodingClient} — NOT registered as a Spring bean.
 *
 * Geocoding (address → coordinates, coordinates → address) is not used in the current
 * architecture. The mobile app sends GPS coordinates directly; the backend passes them
 * to the ward-locator service without any geocoding step.
 *
 * Retained for reference. When a real geocoding provider is integrated:
 *   - Create a production implementation (e.g. NominatimGeocodingClient @Profile("!local"))
 *   - Add @Component @Profile("local") here for dev parity
 *   - Re-inject GeocodingClient into DefaultLocationIntelligenceService
 */
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
                .latitude(latitude)
                .longitude(longitude)
                .formattedAddress(null)
                .confidenceScore(0.0)
                .build();
    }
}

package com.cxp.platform.location.port;

/**
 * Contract for geocoding providers used by the location module.
 * Supports address-to-coordinate (forward) and coordinate-to-address (reverse) resolution.
 */
public interface GeocodingClient {

    /**
     * Resolves a free-text location description into coordinates and formatted address metadata.
     *
     * @param text raw location text provided by a user or another system
     * @return resolved geocoding result with coordinates, formatted address, and confidence
     */
    GeocodingResult forwardGeocode(String text);

    /**
     * Resolves coordinates into a human-readable formatted address and confidence metadata.
     *
     * @param latitude latitude coordinate to resolve
     * @param longitude longitude coordinate to resolve
     * @return resolved geocoding result containing the same coordinates and derived address details
     */
    GeocodingResult reverseGeocode(Double latitude, Double longitude);
}

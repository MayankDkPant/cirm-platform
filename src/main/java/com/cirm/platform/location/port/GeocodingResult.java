package com.cirm.platform.location.port;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents normalized geocoding output for both forward and reverse lookups.
 * Contains coordinates, a human-readable address, and confidence of the match.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeocodingResult {

    private Double latitude;
    private Double longitude;
    private String formattedAddress;
    private Double confidenceScore;
}

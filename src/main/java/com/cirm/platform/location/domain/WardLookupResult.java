package com.cirm.platform.location.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Represents the result of GIS-based ward lookup performed using coordinates.
 * Includes resolved municipality and ward identifiers with display names.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardLookupResult {

    private UUID municipalityId;
    private String municipalityName;
    private UUID wardId;
    private String wardName;
}

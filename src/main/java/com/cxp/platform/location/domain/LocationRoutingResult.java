package com.cxp.platform.location.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRoutingResult {

    private UUID municipalityId;
    private String municipalityName;
    private UUID wardId;
    private String wardName;
    private String formattedAddress;
}

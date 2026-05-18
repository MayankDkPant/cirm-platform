package com.cxp.platform.location.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationRoutingRequest {

    private Double latitude;
    private Double longitude;
    private String reportedLocationText;
}

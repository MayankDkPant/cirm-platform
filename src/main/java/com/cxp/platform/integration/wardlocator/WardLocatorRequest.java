package com.cxp.platform.integration.wardlocator;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WardLocatorRequest(
        @JsonProperty("lat") double lat,
        @JsonProperty("lon") double lon
) {}

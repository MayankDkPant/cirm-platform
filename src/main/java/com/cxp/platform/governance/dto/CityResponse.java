package com.cxp.platform.governance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "City lookup item for the governance discovery hierarchy. " +
                      "Use id to navigate to GET /api/v1/cities/{cityId}/governing-bodies. " +
                      "Only active cities are returned — inactive cities are filtered server-side.")
public record CityResponse(

        @Schema(description = "Canonical city identifier.")
        UUID id,

        @Schema(description = "City display name.")
        String name
) {}

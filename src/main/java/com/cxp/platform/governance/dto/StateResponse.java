package com.cxp.platform.governance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "State lookup item for the governance discovery hierarchy. " +
                      "Use id to navigate to GET /api/v1/states/{stateId}/districts.")
public record StateResponse(

        @Schema(description = "Canonical state identifier.")
        UUID id,

        @Schema(description = "State display name.")
        String name
) {}

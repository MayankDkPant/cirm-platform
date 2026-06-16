package com.cxp.platform.governance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "District lookup item for the governance discovery hierarchy. " +
                      "Use id to navigate to GET /api/v1/districts/{districtId}/cities.")
public record DistrictResponse(

        @Schema(description = "Canonical district identifier.")
        UUID id,

        @Schema(description = "District display name.")
        String name
) {}

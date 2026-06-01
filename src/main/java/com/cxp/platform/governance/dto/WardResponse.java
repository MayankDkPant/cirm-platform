package com.cxp.platform.governance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ward lookup item for the governance discovery hierarchy. " +
                      "Wards are the atomic civic unit for announcement targeting, service request routing, " +
                      "and operator tenancy. Use id as the wardId in provisioning and service request submission.")
public record WardResponse(

        @Schema(description = "Canonical ward identifier. Use as wardId in UserProvisionRequest and ServiceRequestCreateRequest.")
        UUID id,

        @Schema(description = "Ward display name.")
        String name
) {}

package com.cxp.platform.governance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Governing body lookup item for the governance discovery hierarchy. " +
                      "Use id to navigate to GET /api/v1/governing-bodies/{governingBodyId}/wards " +
                      "and GET /api/v1/governing-bodies/{governingBodyId}/departments. " +
                      "The governingBodyId is the canonical tenant identifier for all subsequent API calls.")
public record GoverningBodyResponse(

        @Schema(description = "Canonical governing authority identifier. " +
                              "This is the tenantId carried in operator JWTs and scopes all service requests.")
        UUID id,

        @Schema(description = "Governing body display name (e.g. 'Pune Municipal Corporation').")
        String name
) {}

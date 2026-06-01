package com.cxp.platform.governance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Department lookup item scoped to a governing body. " +
                      "Departments are governing-body-scoped — the same department code may exist under " +
                      "multiple governing bodies and must not be treated as globally unique. " +
                      "Use id as the departmentId in ServiceRequestCreateRequest.")
public record DepartmentResponse(

        @Schema(description = "Canonical department identifier within the governing body's scope.")
        UUID id,

        @Schema(description = "Department display name (e.g. 'Water Supply', 'Roads & Infrastructure').")
        String name
) {}

package com.cxp.platform.servicerequest.dto;

import com.cxp.platform.servicerequest.entity.ServiceRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Operator request to transition a service request to a new lifecycle status.")
public record ServiceRequestStatusUpdateRequest(

        @Schema(description = "Target lifecycle status. Must be a valid transition from the current status.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull ServiceRequestStatus status
) {
}

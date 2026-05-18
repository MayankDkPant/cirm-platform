package com.cxp.platform.servicerequest.dto;

import com.cxp.platform.servicerequest.entity.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;

public record ServiceRequestStatusUpdateRequest(
        @NotNull ServiceRequestStatus status
) {
}

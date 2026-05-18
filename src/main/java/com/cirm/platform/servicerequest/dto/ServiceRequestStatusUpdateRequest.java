package com.cirm.platform.servicerequest.dto;

import com.cirm.platform.servicerequest.entity.ServiceRequestStatus;
import jakarta.validation.constraints.NotNull;

public record ServiceRequestStatusUpdateRequest(
        @NotNull ServiceRequestStatus status
) {
}

package com.cirm.platform.servicerequest.dto;

import com.cirm.platform.servicerequest.entity.ServiceRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ServiceRequestCreateRequest(
        @NotNull ServiceRequestType type,
        @NotBlank String title,
        @NotBlank String description,
        String category,
        String priority,
        String addressText,
        Double latitude,
        Double longitude,
        UUID departmentId,
        UUID aiConversationId
) {
}

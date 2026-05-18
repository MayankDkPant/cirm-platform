package com.cirm.platform.servicerequest.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record LegacyComplaintCreateRequest(
        @NotBlank String title,
        @NotBlank String description,
        String category,
        String priority,
        String addressText,
        String reportedLocationText,
        Double latitude,
        Double longitude,
        UUID departmentId,
        UUID aiConversationId
) {
}

package com.cirm.platform.complaint.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateAIComplaintRequest(
        @NotBlank String title,
        @NotBlank String description,
        String reportedLocationText,
        @NotNull Double latitude,
        @NotNull Double longitude,
        @NotNull UUID aiConversationId
) {
}

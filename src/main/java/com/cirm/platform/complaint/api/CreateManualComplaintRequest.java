package com.cirm.platform.complaint.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateManualComplaintRequest(
        @NotBlank String title,
        @NotBlank String description,
        String reportedLocationText,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String departmentCode
) {
}

package com.cxp.platform.ai.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AIComplaintAnalyzeRequest(
        @NotBlank String text,
        @NotNull Double latitude,
        @NotNull Double longitude
) {
}

package com.cirm.platform.servicerequest.dto;

import jakarta.validation.constraints.NotBlank;

public record AiServiceRequestAnalyzeRequest(
        @NotBlank String text,
        Double latitude,
        Double longitude
) {
}

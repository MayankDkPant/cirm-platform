package com.cxp.platform.servicerequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Input for the AI enrichment preview step. Stateless — no data is persisted. " +
                      "Location coordinates are optional; their absence does not affect classification quality.")
public record AiServiceRequestAnalyzeRequest(

        @Schema(description = "Optional title prepended to the description for richer classification context.",
                nullable = true)
        String title,

        @Schema(description = "Citizen-authored description of the issue to classify. " +
                              "The AI sidecar classifies this text into category, priority, and type.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String description,

        @Schema(description = "GPS latitude for ward resolution. Optional — omit when location is unavailable. " +
                              "Classification is returned regardless; location will be null in the response.",
                nullable = true)
        Double latitude,

        @Schema(description = "GPS longitude for ward resolution. Optional — same semantics as latitude.",
                nullable = true)
        Double longitude
) {}

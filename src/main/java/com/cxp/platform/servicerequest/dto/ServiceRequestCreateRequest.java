package com.cxp.platform.servicerequest.dto;

import com.cxp.platform.servicerequest.entity.ServiceRequestCategory;
import com.cxp.platform.servicerequest.entity.ServiceRequestPriority;
import com.cxp.platform.servicerequest.entity.ServiceRequestType;
import com.cxp.platform.servicerequest.entity.ServiceRequestVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

@Schema(description = "Request body for submitting a new civic service request. " +
                      "AI enrichment is applied server-side — clients may optionally forward " +
                      "the confidence score from a preceding /analyze call.")
public record ServiceRequestCreateRequest(

        @Schema(description = "Service request classification. Optional — defaults to COMPLAINT when absent. " +
                              "Citizens submitting via the mobile app do not select a type explicitly.",
                nullable = true)
        ServiceRequestType type,

        @Schema(description = "Short human-readable title of the service request.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String title,

        @Schema(description = "Full description of the issue reported by the citizen.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String description,

        @Schema(description = "Issue category. Optional — defaults to OTHER when absent or when AI classification is inconclusive.",
                nullable = true)
        ServiceRequestCategory category,

        @Schema(description = "Priority level. Optional — defaults server-side when absent.", nullable = true)
        ServiceRequestPriority priority,

        @Schema(description = "Human-readable address text provided by the citizen. " +
                              "Null when the submission omits location.", nullable = true)
        String addressText,

        @Schema(description = "GPS latitude of the reported issue location. Null when location is unknown.", nullable = true)
        Double latitude,

        @Schema(description = "GPS longitude of the reported issue location. Null when location is unknown.", nullable = true)
        Double longitude,

        @Schema(description = "Optional explicit department assignment. Null until department routing is implemented.", nullable = true)
        UUID departmentId,

        @Schema(description = "AI classification confidence score [0.0–1.0] forwarded from the /analyze step. " +
                              "Stored as-is — the platform does not recompute it. " +
                              "Null when the submission bypasses the analyze flow.",
                nullable = true)
        Double aiConfidence,

        @Schema(description = "DEPRECATED — conversational AI session identifier. " +
                              "This field is a legacy remnant of a discontinued conversational AI flow. " +
                              "It has no replacement — standard submissions use the analyze→submit flow instead. " +
                              "The value is persisted as-is but has no downstream effect on routing or enrichment. " +
                              "Will be removed in a future release.",
                nullable = true,
                deprecated = true)
        UUID aiConversationId,

        @Schema(description = "Visibility scope for the service request. Optional — defaults to PRIVATE. " +
                              "The derived isPublic flag is computed server-side; clients must not send it.",
                nullable = true)
        ServiceRequestVisibility visibilityScope
) {
}

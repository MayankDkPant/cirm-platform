package com.cxp.platform.servicerequest.dto;

import com.cxp.platform.servicerequest.entity.ServiceRequestCategory;
import com.cxp.platform.servicerequest.entity.ServiceRequestPriority;
import com.cxp.platform.servicerequest.entity.ServiceRequestType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "AI enrichment preview result for the analyze step. " +
                      "Enrichment-only — advisory and non-authoritative. No data is persisted by this call. " +
                      "Clients may forward the confidence score to the submission endpoint.")
public record AiServiceRequestAnalyzeResponse(

        @Schema(description = "Suggested service request type. Always populated — falls back to COMPLAINT if the AI sidecar fails.")
        ServiceRequestType type,

        @Schema(description = "Suggested issue category. Always populated — falls back to OTHER if classification is inconclusive.")
        ServiceRequestCategory category,

        @Schema(description = "Suggested priority level. Always populated — falls back to a default if classification is inconclusive.")
        ServiceRequestPriority priority,

        @Schema(description = "Classification confidence score in [0.0–1.0]. Always present. " +
                              "Advisory — the platform does not enforce a minimum threshold for submission.")
        double confidence,

        @Schema(description = "Raw department hint returned by the AI sidecar. " +
                              "Informational only — not persisted or validated against the governance hierarchy. " +
                              "Null when the sidecar returns no department suggestion.",
                nullable = true)
        String suggestedDepartment,

        @Schema(description = "Resolved ward context for the supplied coordinates. " +
                              "Null when no GPS coordinates were provided in the request.",
                nullable = true)
        WardLocationContext location
) {
    @Schema(description = "Ward context resolved from GPS coordinates during the analyze step.")
    public record WardLocationContext(

            @Schema(description = "Numeric ward number. Null when the ward has no assigned number.",
                    nullable = true)
            Integer wardNo,

            @Schema(description = "Ward display name. Null when the ward-locator returns no name.",
                    nullable = true)
            String wardName
    ) {}
}

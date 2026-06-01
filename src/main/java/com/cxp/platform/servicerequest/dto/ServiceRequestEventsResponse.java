package com.cxp.platform.servicerequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Complete immutable audit timeline for a service request. " +
                      "Events are append-only and ordered chronologically.")
public record ServiceRequestEventsResponse(

        @Schema(description = "Identifier of the service request this timeline belongs to.")
        UUID serviceRequestId,

        @Schema(description = "External Salesforce case reference. Null until sync completes successfully. " +
                              "Deferred: currently recorded in the SYNCED_TO_SALESFORCE event notes field " +
                              "rather than as a first-class field here.",
                nullable = true)
        String referenceNumber,

        @Schema(description = "Ordered list of audit events, most recent last. Never null — may be empty.")
        List<ServiceRequestEventResponse> events
) {}

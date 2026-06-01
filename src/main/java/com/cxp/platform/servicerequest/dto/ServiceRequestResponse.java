package com.cxp.platform.servicerequest.dto;

import com.cxp.platform.servicerequest.entity.ServiceRequestCategory;
import com.cxp.platform.servicerequest.entity.ServiceRequestExternalSyncStatus;
import com.cxp.platform.servicerequest.entity.ServiceRequestPriority;
import com.cxp.platform.servicerequest.entity.ServiceRequestStatus;
import com.cxp.platform.servicerequest.entity.ServiceRequestType;
import com.cxp.platform.servicerequest.entity.ServiceRequestVisibility;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Canonical service request representation returned by GET and POST operations. " +
                      "Nullable fields are explicitly noted; media and mediaCount are placeholder fields " +
                      "reserved for a future media-upload feature.")
public record ServiceRequestResponse(

        @Schema(description = "Stable platform-generated identifier for this service request.")
        UUID id,

        @Schema(description = "Service request classification type. Always populated.")
        ServiceRequestType type,

        @Schema(description = "Short human-readable title of the service request. Always populated.")
        String title,

        @Schema(description = "Full citizen-authored description of the reported issue. Always populated.")
        String description,

        @Schema(description = "Issue category resolved by AI or defaulted to OTHER. Always populated.")
        ServiceRequestCategory category,

        @Schema(description = "Priority assigned by AI enrichment or defaulted server-side. Always populated.")
        ServiceRequestPriority priority,

        @Schema(description = "Current lifecycle status of the service request. Always populated.")
        ServiceRequestStatus status,

        @Schema(description = "Human-readable address text. Null when location routing found no formatted address.",
                nullable = true)
        String addressText,

        @Schema(description = "GPS latitude supplied at submission. Null when no coordinates were provided.",
                nullable = true)
        Double latitude,

        @Schema(description = "GPS longitude supplied at submission. Null when no coordinates were provided.",
                nullable = true)
        Double longitude,

        @Schema(description = "Assigned department identifier. Null until department routing is implemented.",
                nullable = true)
        UUID departmentId,

        @Schema(description = "Canonical ward identifier resolved by location routing. Always populated.")
        UUID wardId,

        @Schema(description = "Denormalized ward display label captured at routing time. " +
                              "Null when the ward-locator returned no name for the resolved ward.",
                nullable = true)
        String wardName,

        @Schema(description = "AI classification confidence score [0.0–1.0] forwarded from the citizen's analyze step. " +
                              "Advisory only — not authoritative. Null when submitted without AI enrichment.",
                nullable = true)
        Double aiConfidence,

        @Schema(description = "True when visibilityScope resolves to PUBLIC. " +
                              "Derived server-side from visibilityScope — clients must not send this field.")
        boolean isPublic,

        @Schema(description = "Visibility scope as submitted. Canonical authority on public/private semantics.")
        ServiceRequestVisibility visibilityScope,

        @Schema(description = "External sync state with the Salesforce case management system. Always populated.")
        ServiceRequestExternalSyncStatus externalSyncStatus,

        @Schema(description = "UTC timestamp when the service request was created. Always populated.")
        Instant createdAt,

        @Schema(description = "External Salesforce case reference. Null until external sync completes successfully. " +
                              "Deferred: populated via the event timeline until surfaced here as a first-class field.",
                nullable = true)
        String referenceNumber,

        @Schema(description = "Denormalized department display label. Always null — deferred to avoid N+1 queries " +
                              "on paginated lists. Resolve names client-side via GET /api/v1/governing-bodies/{id}/departments.",
                nullable = true)
        String department,

        @Schema(description = "Count of attached media files. Always 0 — media upload is not yet implemented. " +
                              "Guaranteed non-null to preserve FE numeric assumptions.")
        int mediaCount,

        @Schema(description = "UTC timestamp of the most recent update. Always populated.")
        Instant updatedAt,

        @Schema(description = "UTC timestamp when the request reached RESOLVED or CLOSED status. " +
                              "Null until the request is resolved.", nullable = true)
        Instant resolvedAt,

        @Schema(description = "Attached media files. Always an empty list — media upload is not yet implemented. " +
                              "Guaranteed non-null to prevent FE null-check regressions.")
        List<ServiceRequestMediaResponse> media
) {}

package com.cxp.platform.announcement.dto;

import com.cxp.platform.announcement.domain.AnnouncementPriority;
import com.cxp.platform.announcement.domain.AnnouncementStatus;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Operator request to create a new civic announcement. " +
                      "Exactly one geographic FK field must be provided to match the targetScope. " +
                      "The service validates this invariant and returns HTTP 400 on violation.")
public record AnnouncementCreateRequest(

        @Schema(description = "Announcement headline. Required.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 300, message = "Title must be 300 characters or fewer")
        String title,

        @Schema(description = "Full announcement body text. Required.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        String content,

        @Schema(description = "Short summary for list views and notifications. Optional.", nullable = true)
        @Size(max = 500, message = "Summary must be 500 characters or fewer")
        String summary,

        @Schema(description = "Operator-defined category label (e.g. 'infrastructure', 'health'). Optional.",
                nullable = true)
        @Size(max = 100)
        String category,

        @Schema(description = "Priority level. Optional — defaults to NORMAL.", nullable = true)
        AnnouncementPriority priority,

        @Schema(description = "Initial publication status. Optional — defaults to DRAFT. " +
                              "ARCHIVED and EXPIRED are system-managed and rejected on creation.",
                nullable = true)
        AnnouncementStatus status,

        @Schema(description = "Geographic scope of this announcement. Required. " +
                              "Determines which citizens receive the announcement in their feed.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "target_scope is required")
        AnnouncementTargetScope targetScope,

        @Schema(description = "Ward identifier. Required when targetScope is WARD; null otherwise.",
                nullable = true)
        UUID wardId,

        @Schema(description = "Zone identifier. Required when targetScope is ZONE; null otherwise.",
                nullable = true)
        UUID zoneId,

        @Schema(description = "City identifier. Required when targetScope is CITY; null otherwise.",
                nullable = true)
        UUID cityId,

        @Schema(description = "District identifier. Required when targetScope is DISTRICT; null otherwise.",
                nullable = true)
        UUID districtId,

        @Schema(description = "State identifier. Required when targetScope is STATE; null otherwise.",
                nullable = true)
        UUID stateId,

        @Schema(description = "UTC expiry timestamp. Null means the announcement never auto-expires.",
                nullable = true)
        Instant expiresAt,

        @Schema(description = "When true, the announcement is pinned to the top of citizen feeds. " +
                              "Optional — defaults to false.",
                nullable = true)
        Boolean pinned,

        @Schema(description = "Topic labels as a JSON array string (e.g. '[\"water\",\"road\"]'). " +
                              "Optional — stored verbatim.",
                nullable = true)
        String tags
) {}

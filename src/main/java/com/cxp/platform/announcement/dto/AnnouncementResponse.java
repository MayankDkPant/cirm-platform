package com.cxp.platform.announcement.dto;

import com.cxp.platform.announcement.domain.AnnouncementPriority;
import com.cxp.platform.announcement.domain.AnnouncementStatus;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Full operator-facing announcement representation. " +
                      "Includes internal routing fields (geographic FKs, governingBodyId, status) " +
                      "not exposed in the citizen feed view.")
public record AnnouncementResponse(

        @Schema(description = "Stable platform-generated identifier for this announcement.")
        UUID id,

        @Schema(description = "Announcement headline.")
        String title,

        @Schema(description = "Full announcement body text.")
        String content,

        @Schema(description = "Short summary for list views and notifications.", nullable = true)
        String summary,

        @Schema(description = "Operator-defined category label.", nullable = true)
        String category,

        @Schema(description = "Priority level.")
        AnnouncementPriority priority,

        @Schema(description = "Current publication status. ARCHIVED and EXPIRED are system-managed transitions.")
        AnnouncementStatus status,

        @Schema(description = "Canonical governing authority that owns this announcement.")
        UUID governingBodyId,

        @Schema(description = "Geographic scope determining which citizens receive this announcement.")
        AnnouncementTargetScope targetScope,

        @Schema(description = "Ward identifier. Non-null only when targetScope is WARD.", nullable = true)
        UUID wardId,

        @Schema(description = "Zone identifier. Non-null only when targetScope is ZONE.", nullable = true)
        UUID zoneId,

        @Schema(description = "City identifier. Non-null only when targetScope is CITY.", nullable = true)
        UUID cityId,

        @Schema(description = "District identifier. Non-null only when targetScope is DISTRICT.", nullable = true)
        UUID districtId,

        @Schema(description = "State identifier. Non-null only when targetScope is STATE.", nullable = true)
        UUID stateId,

        @Schema(description = "UTC timestamp when the announcement was published. Null when status is DRAFT.",
                nullable = true)
        Instant publishedAt,

        @Schema(description = "UTC timestamp when the announcement auto-expires. Null means no automatic expiry.",
                nullable = true)
        Instant expiresAt,

        @Schema(description = "When true, the announcement is pinned to the top of citizen feeds.")
        boolean pinned,

        @Schema(description = "Topic labels stored as a JSON array string (e.g. '[\"water\",\"road\"]'). " +
                              "Null when no tags were supplied.",
                nullable = true)
        String tags,

        @Schema(description = "UTC creation timestamp.")
        Instant createdAt,

        @Schema(description = "UTC last-updated timestamp.")
        Instant updatedAt
) {}

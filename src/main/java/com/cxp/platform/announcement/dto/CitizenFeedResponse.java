package com.cxp.platform.announcement.dto;

import com.cxp.platform.announcement.domain.AnnouncementPriority;
import com.cxp.platform.announcement.domain.AnnouncementTargetScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Citizen-facing announcement shape. " +
                      "Returned by GET /api/v1/announcements/feed (public) and GET /api/v1/announcements/my-feed (authenticated). " +
                      "Internal routing fields (geographic FKs, governingBodyId, status) are intentionally excluded. " +
                      "The targetScope label is retained so the mobile UI can render scope badges (e.g. 'Ward notice').")
public record CitizenFeedResponse(

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

        @Schema(description = "Priority level for visual prominence in the citizen feed.")
        AnnouncementPriority priority,

        @Schema(description = "Geographic scope of this announcement. " +
                              "Use for rendering scope badges (e.g. WARD → 'Ward notice', CITY → 'City alert'). " +
                              "Does not expose the underlying geographic FK identifiers.")
        AnnouncementTargetScope targetScope,

        @Schema(description = "UTC timestamp when the announcement was published.")
        Instant publishedAt,

        @Schema(description = "UTC timestamp when the announcement expires. Null means no automatic expiry.",
                nullable = true)
        Instant expiresAt,

        @Schema(description = "When true, this announcement is pinned and should appear at the top of the feed.")
        boolean pinned,

        @Schema(description = "Topic labels as a JSON array string (e.g. '[\"water\",\"road\"]'). " +
                              "Null when no tags were supplied.",
                nullable = true)
        String tags,

        @Schema(description = "UTC creation timestamp.")
        Instant createdAt
) {}

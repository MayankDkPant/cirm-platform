package com.cxp.platform.announcement.dto;

import com.cxp.platform.announcement.domain.AnnouncementPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Operator request to update a DRAFT announcement. " +
                      "Only mutable content fields are accepted — targeting fields " +
                      "(targetScope, wardId, zoneId, cityId, districtId, stateId) are immutable after creation. " +
                      "Null fields are ignored; the existing value is preserved. " +
                      "Returns HTTP 409 if the announcement is not in DRAFT status.")
public record AnnouncementUpdateRequest(

        @Schema(description = "Replacement headline. Null preserves existing value.", nullable = true)
        @Size(max = 300, message = "Title must be 300 characters or fewer")
        String title,

        @Schema(description = "Replacement body text. Null preserves existing value.", nullable = true)
        String content,

        @Schema(description = "Replacement short summary. Null preserves existing value.", nullable = true)
        @Size(max = 500, message = "Summary must be 500 characters or fewer")
        String summary,

        @Schema(description = "Replacement category label. Null preserves existing value.", nullable = true)
        @Size(max = 100)
        String category,

        @Schema(description = "Replacement priority level. Null preserves existing value.", nullable = true)
        AnnouncementPriority priority,

        @Schema(description = "Replacement UTC expiry timestamp. Null preserves existing value. " +
                              "Must be a future timestamp when supplied.",
                nullable = true)
        Instant expiresAt,

        @Schema(description = "Replacement pinned flag. Null preserves existing value.", nullable = true)
        Boolean pinned,

        @Schema(description = "Replacement topic labels as a JSON array string. Null preserves existing value.",
                nullable = true)
        String tags
) {}
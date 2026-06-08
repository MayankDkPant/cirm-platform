package com.cxp.platform.announcement.dto;

import com.cxp.platform.announcement.domain.AnnouncementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Operator request to transition an announcement's lifecycle status. " +
                      "Valid API-driven transitions: DRAFT → PUBLISHED, PUBLISHED → ARCHIVED. " +
                      "EXPIRED is system-managed (expiry sweep) and cannot be set via this endpoint.")
public record AnnouncementStatusUpdateRequest(

        @Schema(description = "Target status. Accepted values: PUBLISHED (from DRAFT), ARCHIVED (from PUBLISHED). " +
                              "EXPIRED is rejected — it is set by the platform expiry sweep, not by operators.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "status is required")
        AnnouncementStatus status
) {}

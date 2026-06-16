package com.cxp.platform.servicerequest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Attached media file descriptor. Placeholder — media upload is not yet implemented. " +
                      "This type is reserved in the contract; the media list in ServiceRequestResponse is always empty.")
public record ServiceRequestMediaResponse(

        @Schema(description = "Stable identifier for this media attachment.")
        UUID id,

        @Schema(description = "Cloudflare R2 object key for the stored media file.")
        String r2ObjectKey,

        @Schema(description = "MIME content type of the file (e.g. image/jpeg).")
        String contentType,

        @Schema(description = "Ordering position for display; lower values appear first.")
        int displayOrder
) {}

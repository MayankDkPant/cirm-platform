package com.cxp.platform.api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard API error response used across the entire platform.
 *
 * fields — present only on VALIDATION_FAILED responses; maps each failing field
 *          name to its constraint message. Null for all other error types.
 */
@Schema(description = "Standard platform error response. The 'fields' map is present only on " +
                      "VALIDATION_FAILED (400) responses — it maps each failing field name to " +
                      "its constraint message. All other error types omit 'fields'.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        @Schema(description = "UTC timestamp of the error") LocalDateTime timestamp,
        @Schema(description = "HTTP status code", example = "400") int status,
        @Schema(description = "Machine-readable error code, e.g. VALIDATION_FAILED, NOT_FOUND, BAD_REQUEST",
                example = "VALIDATION_FAILED") String error,
        @Schema(description = "Human-readable error message") String message,
        @Schema(description = "Request path that produced the error", example = "/api/v1/service-requests") String path,
        @Schema(description = "Field-level validation errors; present only on VALIDATION_FAILED responses",
                nullable = true) Map<String, String> fields
) {
    /** Convenience constructor for non-validation errors (no fields map). */
    public ApiError(LocalDateTime timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}

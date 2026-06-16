package com.cxp.platform.common.openapi;

import com.cxp.platform.api.error.ApiError;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Reusable composed annotation declaring the two response codes common to every
 * authenticated platform endpoint:
 *
 *   401 — Authentication required. Body: {"error":"Unauthorized","message":"..."}
 *         (produced by RestAuthenticationEntryPoint — not ApiError shape).
 *
 *   500 — Unexpected platform error. Body: ApiError with a generic message.
 *
 * Usage: place @StandardApiErrors on a controller method alongside any
 * endpoint-specific @ApiResponse annotations (400, 403, 404, etc.).
 * Do not duplicate 401/500 individually on methods that already carry this annotation.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description  = "Authentication required — Bearer JWT is missing, expired, or invalid. " +
                               "Response body: {\"error\":\"Unauthorized\",\"message\":\"Invalid or missing token\"}",
                content      = @Content
        ),
        @ApiResponse(
                responseCode = "500",
                description  = "Unexpected platform error.",
                content      = @Content(
                        mediaType = "application/json",
                        schema    = @Schema(implementation = ApiError.class)
                )
        )
})
public @interface StandardApiErrors {
}

package com.cxp.platform.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT token pair issued after successful OTP verification. " +
                      "The access token is short-lived (30 min); the refresh token is long-lived (30 days).")
public record AuthTokenResponse(

        @Schema(description = "Short-lived JWT access token (30 min). " +
                              "Include in the Authorization: Bearer header for all authenticated requests.")
        String accessToken,

        @Schema(description = "Long-lived refresh token (30 days). " +
                              "Use to obtain a new access token without re-authenticating via OTP.")
        String refreshToken,

        @Schema(description = "Token type. Always 'Bearer'.", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds.", example = "1800")
        long expiresIn
) {}

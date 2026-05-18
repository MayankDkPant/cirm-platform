package com.cxp.platform.auth.web.dto;

/**
 * Response DTO returned after successful OTP verification.
 *
 * Contains the issued access and refresh tokens that the client
 * will use to call secured APIs.
 */
public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {}
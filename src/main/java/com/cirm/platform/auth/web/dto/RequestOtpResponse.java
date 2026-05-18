package com.cirm.platform.auth.web.dto;

/**
 * Response returned after OTP generation.
 *
 * NOTE:
 * OTP value will be removed once SMS integration is added.
 */
public record RequestOtpResponse(
        String otpReference,
        String otp   // temporary for development
) {}

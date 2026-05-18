package com.cirm.platform.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO used when the client submits OTP for login.
 *
 * This is the second step of the mobile authentication flow.
 * The client provides the OTP reference received from the
 * request-otp endpoint along with the OTP code.
 */
public record VerifyOtpRequest(

        @NotNull(message = "OTP reference is required")
        UUID otpReference,

        @NotBlank(message = "OTP is required")
        String otp
) {}
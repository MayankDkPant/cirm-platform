package com.cxp.platform.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Second step of the mobile OTP authentication flow. " +
                      "Submitting the correct OTP code issues a JWT access token and refresh token.")
public record VerifyOtpRequest(

        @Schema(description = "OTP session reference received from the request-OTP response.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "OTP reference is required")
        UUID otpReference,

        @Schema(description = "One-time password code delivered to the citizen's mobile number.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "OTP is required")
        String otp
) {}

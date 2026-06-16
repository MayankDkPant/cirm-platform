package com.cxp.platform.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "First step of the mobile OTP authentication flow. " +
                      "Submitting this request generates a time-limited OTP sent to the citizen's mobile number.")
public record RequestOtpRequest(

        @Schema(description = "Valid Indian mobile number (10 digits, starting with 6–9).",
                example = "9876543210",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[6-9]\\d{9}$",
                message = "Mobile number must be a valid Indian mobile number"
        )
        String mobileNumber,

        @Schema(description = "Unique device identifier used to bind the OTP session to a specific device.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Device ID is required")
        String deviceId,

        @Schema(description = "Human-readable device name for session auditing.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Device name is required")
        String deviceName
) {}

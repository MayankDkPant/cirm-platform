package com.cxp.platform.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for requesting OTP.
 *
 * This represents the HTTP contract.
 * It must never expose domain objects directly.
 */
public record RequestOtpRequest(

        @NotBlank(message = "Mobile number is required")
        @Pattern(
                regexp = "^[6-9]\\d{9}$",
                message = "Mobile number must be a valid Indian mobile number"
        )
        String mobileNumber,

        @NotBlank(message = "Device ID is required")
        String deviceId,

        @NotBlank(message = "Device name is required")
        String deviceName
) {}

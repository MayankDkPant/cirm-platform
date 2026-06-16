package com.cxp.platform.auth.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response after OTP generation. The otpReference must be forwarded to the verify step.")
public record RequestOtpResponse(

        @Schema(description = "Opaque reference token identifying this OTP session. " +
                              "Must be included in the subsequent verify-OTP request.")
        String otpReference,

        @Schema(description = "DEVELOPMENT-ONLY — OTP plaintext value. " +
                              "This field is present only because SMS delivery is not yet integrated. " +
                              "It exposes the OTP directly for local development and testing. " +
                              "This field will be REMOVED (not deprecated-then-removed) when SMS delivery is wired — " +
                              "it is not a backward-compatibility alias and should never be relied upon in production.",
                deprecated = true)
        String otp
) {}

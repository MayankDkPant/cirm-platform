package com.cxp.platform.auth.web;

import com.cxp.platform.auth.web.dto.RequestOtpRequest;
import com.cxp.platform.auth.web.dto.RequestOtpResponse;
import com.cxp.platform.auth.application.OtpApplicationService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * OTP auth endpoints — local/dev profile only.
 *
 * The OTP flow is incomplete for production:
 *   - Step 1 (request-otp) returns OTP plaintext — no SMS delivery integrated.
 *   - Step 2 (verify-otp) has no HTTP endpoint; JWT issuance from OTP is unimplemented.
 *
 * The mobile app authenticates via Supabase Google OAuth; OTP is not in the active path.
 * Domain logic (OtpApplicationService, OtpRepositoryImpl, RefreshTokenService) is intact
 * and can be wired up when SMS delivery and verify-otp are implemented.
 *
 * Remove @Profile("local") and complete verify-otp before enabling in production.
 */
@Profile("local")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApi {

    private final OtpApplicationService otpService;

    public AuthApi(OtpApplicationService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/request-otp")
    public RequestOtpResponse requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        var result = otpService.requestOtp(request.mobileNumber());
        return new RequestOtpResponse(result.otpReference(), result.otp());
    }
}

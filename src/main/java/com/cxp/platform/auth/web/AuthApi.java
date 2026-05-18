package com.cxp.platform.auth.web;

import com.cxp.platform.auth.web.dto.RequestOtpRequest;
import com.cxp.platform.auth.web.dto.RequestOtpResponse;
import com.cxp.platform.auth.application.OtpApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Auth API exposes authentication endpoints.
 *
 * Base path is versioned from day one to avoid breaking changes.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthApi {

    private final OtpApplicationService otpService;

    public AuthApi(OtpApplicationService otpService) {
        this.otpService = otpService;
    }

    /**
     * Endpoint: Request OTP for mobile login / onboarding.
     *
     * Flow:
     *  1. Validate request DTO
     *  2. Call application service
     *  3. Return OTP reference (and OTP for dev)
     */
    @PostMapping("/request-otp")
    public RequestOtpResponse requestOtp(
            @Valid @RequestBody RequestOtpRequest request) {

        var result = otpService.requestOtp(request.mobileNumber());

        return new RequestOtpResponse(
                result.otpReference(),
                result.otp()   // temporary until SMS integration
        );
    }
}

package com.cxp.platform.auth.domain.otp;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Domain Repository for OTP aggregate.
 * Pure domain interface — no Spring/JPA here.
 */
public interface OtpRepository {

    Otp save(Otp otp);

    Optional<Otp> findByReferenceAndMobile(String otpReference, String mobileNumber);

    Optional<Otp> findLatestByMobile(String mobileNumber);

    void invalidateActiveOtps(String mobileNumber, LocalDateTime now);
}

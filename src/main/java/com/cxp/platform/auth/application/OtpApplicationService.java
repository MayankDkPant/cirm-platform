package com.cxp.platform.auth.application;

import com.cxp.platform.auth.domain.otp.Otp;
import com.cxp.platform.auth.domain.otp.OtpRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service orchestrating use cases for the module.
 *
 * Application Service responsible for OTP lifecycle.
 *
 * This is the USE-CASE layer.
 * It orchestrates domain + infrastructure to execute business flows.
 *
 * Responsibilities:
 *  - Enforce resend cooldown (30 sec)
 *  - Invalidate previous OTPs
 *  - Generate secure OTP
 *  - Hash OTP
 *  - Persist OTP
 */
@Service
public class OtpApplicationService {

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 2;
    private static final int RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_ATTEMPTS = 5;

    private final OtpRepository otpRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpApplicationService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    /**
     * Main use-case: Request OTP for a mobile number.
     *
     * Returns:
     *  - OTP reference (public id)
     *  - Generated OTP (temporarily returned for SMS integration later)
     */
    public OtpResponse requestOtp(String mobileNumber) {

        LocalDateTime now = LocalDateTime.now();

        // 1️⃣ Check resend cooldown
        Optional<Otp> latestOtpOpt = otpRepository.findLatestByMobile(mobileNumber);

        if (latestOtpOpt.isPresent()) {
            Otp latestOtp = latestOtpOpt.get();

            long secondsSinceLastOtp =
                    java.time.Duration.between(
                            latestOtp.getExpiresAt().minusMinutes(OTP_EXPIRY_MINUTES),
                            now
                    ).getSeconds();

            if (secondsSinceLastOtp < RESEND_COOLDOWN_SECONDS) {
                throw new RuntimeException("OTP requested too frequently. Please wait.");
            }
        }

        // 2️⃣ Invalidate previous active OTPs
        otpRepository.invalidateActiveOtps(mobileNumber, now);

        // 3️⃣ Generate new OTP
        String otpPlain = generateOtp();

        // 4️⃣ Hash OTP before storing
        String otpHash = passwordEncoder.encode(otpPlain);

        // 5️⃣ Create domain object
        Otp otp = new Otp(
                UUID.randomUUID(),
                generateOtpReference(),
                mobileNumber,
                otpHash,
                now.plusMinutes(OTP_EXPIRY_MINUTES),
                0,
                MAX_ATTEMPTS
        );

        // 6️⃣ Persist OTP
        otpRepository.save(otp);

        return new OtpResponse(otp.getOtpReference(), otpPlain);
    }

    /**
     * Generate secure 6-digit numeric OTP.
     */
    private String generateOtp() {
        int number = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(number);
    }

    /**
     * Public reference exposed to API instead of DB id.
     */
    private String generateOtpReference() {
        return UUID.randomUUID().toString();
    }

    /**
     * Temporary response DTO.
     * Later OTP will be sent via SMS and removed from response.
     */
    public record OtpResponse(String otpReference, String otp) {}
}

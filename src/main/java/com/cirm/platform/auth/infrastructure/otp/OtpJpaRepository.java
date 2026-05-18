package com.cirm.platform.auth.infrastructure.otp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for OTP persistence.
 *
 * This is an INFRASTRUCTURE component.
 * It exposes low-level DB queries using JPA.
 *
 * IMPORTANT:
 * This repository should NOT be used outside the infrastructure layer.
 * The application/domain must interact only via the Domain Repository interface.
 */
public interface OtpJpaRepository extends JpaRepository<OtpJpaEntity, UUID> {

    /**
     * Find OTP by public reference and mobile number.
     *
     * Used during OTP verification flow.
     */
    Optional<OtpJpaEntity> findByOtpReferenceAndMobileNumber(
            String otpReference,
            String mobileNumber
    );

    /**
     * Find the most recent OTP requested for a mobile number.
     *
     * Used to:
     * - Enforce resend cooldown (30 seconds)
     * - Identify active OTP to invalidate
     */
    Optional<OtpJpaEntity> findTopByMobileNumberOrderByCreatedAtDesc(
            String mobileNumber
    );

    /**
     * Invalidate active OTPs for a mobile number.
     *
     * Instead of deleting rows, we mark them expired by setting expires_at = now.
     * This preserves full audit trail (important for government systems).
     */
    void deleteByMobileNumberAndExpiresAtAfter(
            String mobileNumber,
            LocalDateTime now
    );
}

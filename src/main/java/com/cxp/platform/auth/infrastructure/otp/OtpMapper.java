package com.cxp.platform.auth.infrastructure.otp;

import com.cxp.platform.auth.domain.otp.Otp;

import java.util.UUID;

/**
 * Mapper responsible for converting between:
 *  - Domain model (Otp)
 *  - JPA entity (OtpJpaEntity)
 *
 * Why we need this:
 * Domain must remain independent from JPA/database concerns.
 */
public class OtpMapper {

    /**
     * Convert Domain → JPA entity.
     * Used when saving OTP to database.
     */
    public OtpJpaEntity toJpaEntity(Otp otp) {
        return new OtpJpaEntity(
                otp.getId(),
                otp.getOtpReference(),
                otp.getMobileNumber(),
                otp.getOtpHash(),
                otp.getExpiresAt(),
                otp.getAttemptCount(),
                otp.getMaxAttempts()
        );
    }

    /**
     * Convert JPA entity → Domain.
     * Used when loading OTP from database.
     */
    public Otp toDomain(OtpJpaEntity entity) {
        return new Otp(
                entity.getId(),
                entity.getOtpReference(),
                entity.getMobileNumber(),
                entity.getOtpHash(),
                entity.getExpiresAt(),
                entity.getAttemptCount(),
                entity.getMaxAttempts()
        );
    }
}

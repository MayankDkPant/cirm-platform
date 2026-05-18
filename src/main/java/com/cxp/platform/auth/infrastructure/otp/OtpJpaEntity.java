package com.cxp.platform.auth.infrastructure.otp;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA Entity representing the auth_otp table.
 *
 * IMPORTANT:
 * This class belongs to the INFRASTRUCTURE layer.
 * It is intentionally separate from the Domain model (Otp).
 *
 * Reason:
 * - Contains JPA annotations (technology concern)
 * - Mirrors database schema exactly
 * - Can evolve independently from domain model
 */
@Entity
@Table(name = "auth_otp")
public class OtpJpaEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "otp_reference", nullable = false, unique = true)
    private String otpReference;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    // DB manages timestamps (DEFAULT CURRENT_TIMESTAMP)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // Required by JPA
    protected OtpJpaEntity() {}

    public OtpJpaEntity(UUID id,
                        String otpReference,
                        String mobileNumber,
                        String otpHash,
                        LocalDateTime expiresAt,
                        int attemptCount,
                        int maxAttempts) {
        this.id = id;
        this.otpReference = otpReference;
        this.mobileNumber = mobileNumber;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
    }

    // Getters & setters (needed for JPA)
    public UUID getId() { return id; }
    public String getOtpReference() { return otpReference; }
    public String getMobileNumber() { return mobileNumber; }
    public String getOtpHash() { return otpHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}

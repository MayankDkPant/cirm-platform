package com.cirm.platform.auth.domain.otp;

import java.time.LocalDateTime;
import java.util.UUID;

public class Otp {

    private UUID id;
    private String otpReference;
    private String mobileNumber;
    private String otpHash;
    private LocalDateTime expiresAt;
    private LocalDateTime verifiedAt;
    private int attemptCount;
    private int maxAttempts;

    public Otp(UUID id,
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

    // ---------- Business rules ----------

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public boolean hasAttemptsRemaining() {
        return attemptCount < maxAttempts;
    }

    public void markVerified() {
        this.verifiedAt = LocalDateTime.now();
    }

    public void incrementAttempts() {
        this.attemptCount++;
    }

    // ---------- Getters ----------

    public UUID getId() { return id; }
    public String getOtpReference() { return otpReference; }
    public String getMobileNumber() { return mobileNumber; }
    public String getOtpHash() { return otpHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
}

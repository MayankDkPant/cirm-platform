-- =========================================================
-- V19__auth_otp_table.sql
-- Citizen Authentication - OTP Foundation
-- =========================================================

CREATE TABLE auth_otp (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Public reference used by APIs (never expose DB id)
    otp_reference VARCHAR(50) NOT NULL UNIQUE,

    -- Login identifier
    mobile_number VARCHAR(20) NOT NULL,

    -- Security
    otp_hash VARCHAR(255) NOT NULL,

    -- Lifecycle
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,

    -- Brute force protection
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,

    -- Observability & fraud detection
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id VARCHAR(100),

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- Indexes (VERY IMPORTANT)
-- =========================================================

-- Most frequent lookup: mobile + reference
CREATE INDEX idx_auth_otp_mobile_reference
ON auth_otp (mobile_number, otp_reference);

-- Expiry cleanup job
CREATE INDEX idx_auth_otp_expires_at
ON auth_otp (expires_at);

-- Security analytics later
CREATE INDEX idx_auth_otp_mobile_created
ON auth_otp (mobile_number, created_at);

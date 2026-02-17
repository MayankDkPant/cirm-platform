-- ============================================================
-- V18 : Identity Foundation (Multi Login Support)
-- Adds enterprise authentication model
-- ============================================================

-- ============================================================
-- 1. USERS TABLE ENHANCEMENTS
-- ============================================================

ALTER TABLE users
ADD COLUMN last_login_at TIMESTAMP,
ADD COLUMN failed_login_attempts INT DEFAULT 0,
ADD COLUMN account_locked_until TIMESTAMP;

-- ============================================================
-- 2. USER PROFILE ENHANCEMENTS
-- ============================================================

ALTER TABLE user_profile
ADD COLUMN is_phone_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN is_email_verified BOOLEAN DEFAULT FALSE;

-- ============================================================
-- 3. REFRESH TOKEN ENHANCEMENTS (device awareness)
-- ============================================================

ALTER TABLE refresh_tokens
ADD COLUMN device_info VARCHAR(255),
ADD COLUMN ip_address VARCHAR(50);

-- ============================================================
-- 4. IDENTITY TYPE LOOKUP TABLE
-- ============================================================

CREATE TABLE identity_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Seed identity types
INSERT INTO identity_type (code, description) VALUES
('MOBILE', 'Mobile OTP Login'),
('GOOGLE', 'Google OAuth Login'),
('AADHAAR', 'Aadhaar Verified Identity'),
('EMAIL', 'Email Password Login');

-- ============================================================
-- 5. USER IDENTITY TABLE (MOST IMPORTANT TABLE)
-- ============================================================

CREATE TABLE user_identity (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    user_id UUID NOT NULL,
    identity_type_id UUID NOT NULL,

    identity_value VARCHAR(255) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_identity_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    CONSTRAINT fk_identity_type
        FOREIGN KEY (identity_type_id) REFERENCES identity_type(id),

    CONSTRAINT uq_identity UNIQUE(identity_type_id, identity_value)
);

-- ============================================================
-- 6. INDEXES FOR PERFORMANCE
-- ============================================================

CREATE INDEX idx_user_identity_user ON user_identity(user_id);
CREATE INDEX idx_user_identity_value ON user_identity(identity_value);

-- =====================================================
-- CORE DOMAIN TABLES
-- Users, Auth Tokens, Complaints, Complaint Events
-- =====================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_email_lowercase CHECK (email = LOWER(email))
);

CREATE INDEX idx_users_email ON users(email);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    token_hash VARCHAR(500) NOT NULL,
    user_id UUID NOT NULL,

    expiry_date TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_expiry ON refresh_tokens(expiry_date);

CREATE TABLE complaints (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    title VARCHAR(255) NOT NULL,
    description TEXT,

    department VARCHAR(150),
    priority VARCHAR(50),

    status VARCHAR(50) NOT NULL,

    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,

    external_system VARCHAR(100),
    external_reference_id VARCHAR(150),
    external_sync_status VARCHAR(50),

    resolved_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100)
);

CREATE INDEX idx_complaint_status ON complaints(status);
CREATE INDEX idx_complaint_department ON complaints(department);


CREATE TABLE complaint_event (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    complaint_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,

    old_value TEXT,
    new_value TEXT,

    triggered_by VARCHAR(100) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_complaint
        FOREIGN KEY (complaint_id)
        REFERENCES complaints(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_event_complaint ON complaint_event(complaint_id);



-- =====================================================
-- Enable UUID extension
-- =====================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- GOVERNANCE TYPE
-- Top level classification of governing bodies
-- =====================================================
CREATE TABLE governance_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,

    country_code VARCHAR(10) NOT NULL DEFAULT 'IN',

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),

    CONSTRAINT uq_governance_type_code UNIQUE (code, country_code),
    CONSTRAINT chk_governance_type_code_uppercase CHECK (code = UPPER(code))
);

CREATE INDEX idx_governance_type_active
ON governance_type(is_active)
WHERE is_deleted = false;

-- =====================================================
-- GOVERNANCE SUB TYPE
-- Second level classification
-- =====================================================
CREATE TABLE governance_sub_type (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    governance_type_id UUID NOT NULL,

    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,

    country_code VARCHAR(10) NOT NULL DEFAULT 'IN',

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),

    CONSTRAINT fk_gst_governance_type
        FOREIGN KEY (governance_type_id)
        REFERENCES governance_type(id),

    CONSTRAINT uq_gst_code
        UNIQUE (governance_type_id, code, country_code),

    CONSTRAINT chk_gst_code_uppercase
        CHECK (code = UPPER(code))
);

CREATE INDEX idx_gst_type_lookup
ON governance_sub_type(governance_type_id)
WHERE is_deleted = false;

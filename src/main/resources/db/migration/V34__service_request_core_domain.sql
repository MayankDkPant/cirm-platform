-- ============================================================================
-- CXP service request core domain
-- Generalizes complaint-centric persistence into service_request.
-- Existing complaints remain intact for compatibility while callers migrate.
-- ============================================================================

CREATE TABLE IF NOT EXISTS service_request (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(150),
    priority VARCHAR(50),
    status VARCHAR(50) NOT NULL,

    address_text TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,

    department_id UUID,
    governing_body_id UUID NOT NULL,
    ward_id UUID,
    ward_name VARCHAR(255),
    formatted_address VARCHAR(500),

    ai_conversation_id UUID,
    ai_confidence DOUBLE PRECISION,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    resolved_at TIMESTAMP WITH TIME ZONE,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_service_request_governing_body
        FOREIGN KEY (governing_body_id)
        REFERENCES governing_body(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_service_request_ai_conversation
        FOREIGN KEY (ai_conversation_id)
        REFERENCES ai_conversation(id)
        ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_service_request_type
ON service_request(type);

CREATE INDEX IF NOT EXISTS idx_service_request_status
ON service_request(status);

CREATE INDEX IF NOT EXISTS idx_service_request_tenant_created
ON service_request(governing_body_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_service_request_department
ON service_request(department_id);

CREATE INDEX IF NOT EXISTS idx_service_request_ward
ON service_request(ward_id);

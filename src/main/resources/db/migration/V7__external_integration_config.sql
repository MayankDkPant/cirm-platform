-- =====================================================
-- EXTERNAL INTEGRATION ROUTING CONFIGURATION
-- =====================================================
-- Determines which external systems should be triggered
-- for a given governance scope, entity and event.
-- Supports GLOBAL → STATE → MUNICIPALITY inheritance.
-- =====================================================

CREATE TABLE external_integration_config (
    id UUID PRIMARY KEY,

    -- Scope of configuration
    -- GLOBAL | STATE | MUNICIPALITY
    governing_scope VARCHAR(30) NOT NULL,

    -- UUID of state or municipality (NULL when GLOBAL)
    governing_uuid UUID,

    -- Entity triggering integration (COMPLAINT, PAYMENT, etc.)
    entity_type VARCHAR(100) NOT NULL,

    -- Event triggering integration (CREATED, UPDATED, CLOSED)
    event_type VARCHAR(50) NOT NULL,

    -- External system to invoke
    external_system_id UUID NOT NULL,

    execution_order INT NOT NULL DEFAULT 1,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(255),

    CONSTRAINT fk_ext_integration_system
        FOREIGN KEY (external_system_id)
        REFERENCES external_system(id)
        ON DELETE CASCADE
);

-- Prevent duplicate rules
CREATE UNIQUE INDEX uq_external_integration_rule
ON external_integration_config(
    governing_scope,
    governing_uuid,
    entity_type,
    event_type,
    external_system_id
);

-- Lookup index for orchestration engine
CREATE INDEX idx_ext_integration_lookup
ON external_integration_config(
    governing_scope,
    governing_uuid,
    entity_type,
    event_type,
    is_active
);

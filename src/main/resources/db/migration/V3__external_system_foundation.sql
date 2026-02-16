-- =====================================================
-- EXTERNAL SYSTEM
-- =====================================================

CREATE TABLE external_system (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    scope VARCHAR(20) NOT NULL, -- GLOBAL | STATE
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX uq_external_system_code
ON external_system(code);



-- =====================================================
-- EXTERNAL SYSTEM STATE MAPPING
-- =====================================================

CREATE TABLE external_system_state (
    id UUID PRIMARY KEY,
    external_system_id UUID NOT NULL,
    state_uuid UUID NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255),

    CONSTRAINT fk_ext_state_system
        FOREIGN KEY (external_system_id)
        REFERENCES external_system(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_ext_system_state
ON external_system_state(external_system_id, state_uuid);



-- =====================================================
-- EXTERNAL REFERENCE MAPPING (ID MAPPING ENGINE)
-- =====================================================

CREATE TABLE external_reference (
    id UUID PRIMARY KEY,
    external_system_id UUID NOT NULL,
    state_uuid UUID,
    entity_type VARCHAR(100) NOT NULL,
    entity_uuid UUID NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    master_system VARCHAR(20), -- INTERNAL | EXTERNAL
    last_synced_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(255),

    CONSTRAINT fk_external_reference_system
        FOREIGN KEY (external_system_id)
        REFERENCES external_system(id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX uq_external_reference_unique
ON external_reference(
    external_system_id,
    state_uuid,
    entity_type,
    external_id
);

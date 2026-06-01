-- ============================================================================
-- Immutable audit/event log for ServiceRequest lifecycle transitions.
-- Records are append-only — never updated or deleted.
-- ============================================================================

CREATE TABLE IF NOT EXISTS service_request_event (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_request_id  UUID         NOT NULL,
    event_type          VARCHAR(50)  NOT NULL,
    old_status          VARCHAR(50),
    new_status          VARCHAR(50),
    actor_type          VARCHAR(30),
    actor_user_id       UUID,
    notes               TEXT,
    metadata_json       TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sre_service_request
        FOREIGN KEY (service_request_id)
        REFERENCES service_request(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sre_service_request_id
    ON service_request_event(service_request_id);

CREATE INDEX IF NOT EXISTS idx_sre_created_at
    ON service_request_event(created_at DESC);

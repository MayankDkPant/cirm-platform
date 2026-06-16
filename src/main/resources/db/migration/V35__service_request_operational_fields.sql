-- ============================================================================
-- Add operational fields to service_request for full lifecycle support:
-- external sync tracking, idempotency, and actor audit.
-- ============================================================================

ALTER TABLE service_request
    ADD COLUMN IF NOT EXISTS external_sync_status      VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS external_sync_attempts    INT          NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS external_sync_last_attempt_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS external_sync_error       TEXT,
    ADD COLUMN IF NOT EXISTS idempotency_key           VARCHAR(255),
    ADD COLUMN IF NOT EXISTS created_by_user_id        UUID,
    ADD COLUMN IF NOT EXISTS updated_by_user_id        UUID;

CREATE INDEX IF NOT EXISTS idx_sr_sync_status_attempts
    ON service_request(external_sync_status, external_sync_attempts)
    WHERE external_sync_status IN ('PENDING', 'RETRYING');

CREATE INDEX IF NOT EXISTS idx_sr_idempotency
    ON service_request(idempotency_key, governing_body_id)
    WHERE idempotency_key IS NOT NULL;

-- Make idempotency_request tenant scoped for multi-tenant safety.

ALTER TABLE idempotency_request
    ADD COLUMN IF NOT EXISTS governing_body_id UUID,
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS response_payload TEXT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_idempotency_request_governing_body'
    ) THEN
        ALTER TABLE idempotency_request
            ADD CONSTRAINT fk_idempotency_request_governing_body
            FOREIGN KEY (governing_body_id)
            REFERENCES governing_body(id)
            ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_idempotency_key'
    ) THEN
        ALTER TABLE idempotency_request
            DROP CONSTRAINT uq_idempotency_key;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_idempotency_request_tenant_key'
    ) THEN
        ALTER TABLE idempotency_request
            ADD CONSTRAINT uq_idempotency_request_tenant_key
            UNIQUE (governing_body_id, idempotency_key);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_idempotency_request_tenant_expires
    ON idempotency_request(governing_body_id, expires_at);

-- V26: Multi-tenant governing body foundation
-- Purpose: Introduce governing_body as tenant scope across core domain tables.

-- -----------------------------------------------------------------------------
-- 1) AI conversations: rename municipality_id -> governing_body_id
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ai_conversation'
          AND column_name = 'municipality_id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'ai_conversation'
          AND column_name = 'governing_body_id'
    ) THEN
        ALTER TABLE ai_conversation
            RENAME COLUMN municipality_id TO governing_body_id;
    END IF;
END $$;

-- Ensure ai_conversation has tenant foreign key to governing_body(id)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_ai_conversation_governing_body'
    ) THEN
        ALTER TABLE ai_conversation
            ADD CONSTRAINT fk_ai_conversation_governing_body
            FOREIGN KEY (governing_body_id)
            REFERENCES governing_body(id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ai_conversation_governing_body
    ON ai_conversation(governing_body_id);

-- -----------------------------------------------------------------------------
-- 2) Complaints: add governing_body_id (NOT NULL) + FK + tenant indexes
-- -----------------------------------------------------------------------------
ALTER TABLE complaints
    ADD COLUMN IF NOT EXISTS governing_body_id UUID;

ALTER TABLE complaints
    ALTER COLUMN governing_body_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_complaints_governing_body'
    ) THEN
        ALTER TABLE complaints
            ADD CONSTRAINT fk_complaints_governing_body
            FOREIGN KEY (governing_body_id)
            REFERENCES governing_body(id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_complaints_gb_created
    ON complaints(governing_body_id, created_at);

CREATE INDEX IF NOT EXISTS idx_complaints_gb_status
    ON complaints(governing_body_id, status);

CREATE INDEX IF NOT EXISTS idx_complaints_gb_ward
    ON complaints(governing_body_id, ward_id);

-- -----------------------------------------------------------------------------
-- 3) Complaint events: add governing_body_id (NOT NULL) + FK + index
-- -----------------------------------------------------------------------------
ALTER TABLE complaint_event
    ADD COLUMN IF NOT EXISTS governing_body_id UUID;

ALTER TABLE complaint_event
    ALTER COLUMN governing_body_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_complaint_event_governing_body'
    ) THEN
        ALTER TABLE complaint_event
            ADD CONSTRAINT fk_complaint_event_governing_body
            FOREIGN KEY (governing_body_id)
            REFERENCES governing_body(id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_complaint_event_governing_body
    ON complaint_event(governing_body_id);

-- -----------------------------------------------------------------------------
-- 4) External idempotency tables: add optional governing_body_id + FK + indexes
-- -----------------------------------------------------------------------------
ALTER TABLE external_reference
    ADD COLUMN IF NOT EXISTS governing_body_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_external_reference_governing_body'
    ) THEN
        ALTER TABLE external_reference
            ADD CONSTRAINT fk_external_reference_governing_body
            FOREIGN KEY (governing_body_id)
            REFERENCES governing_body(id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_external_reference_governing_body
    ON external_reference(governing_body_id);

ALTER TABLE external_system_state
    ADD COLUMN IF NOT EXISTS governing_body_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_external_system_state_governing_body'
    ) THEN
        ALTER TABLE external_system_state
            ADD CONSTRAINT fk_external_system_state_governing_body
            FOREIGN KEY (governing_body_id)
            REFERENCES governing_body(id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_external_system_state_governing_body
    ON external_system_state(governing_body_id);

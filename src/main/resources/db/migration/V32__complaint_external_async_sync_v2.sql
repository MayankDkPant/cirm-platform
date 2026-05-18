-- ============================================================================
-- Complaint external sync v2 foundation
-- Adds async sync tracking columns for Salesforce case synchronization.
--
-- This migration is idempotent-safe and supports upgraded deployments where
-- earlier external sync columns may already exist under previous names.
-- ============================================================================

-- 1) Ensure canonical external reference column exists.
--    If legacy external_reference_id exists, rename it to external_reference.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'complaints'
          AND column_name = 'external_reference_id'
    )
    AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'complaints'
          AND column_name = 'external_reference'
    ) THEN
        EXECUTE 'ALTER TABLE complaints RENAME COLUMN external_reference_id TO external_reference';
    END IF;
END $$;

-- 2) Add async sync tracking columns used by the scheduled worker.
ALTER TABLE complaints
    ADD COLUMN IF NOT EXISTS external_sync_status VARCHAR(30),
    ADD COLUMN IF NOT EXISTS external_sync_attempts INT,
    ADD COLUMN IF NOT EXISTS external_last_sync_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS external_reference VARCHAR(50);

-- 3) Backfill and enforce defaults/constraints for stable retry behavior.
UPDATE complaints
SET external_sync_status = 'PENDING'
WHERE external_sync_status IS NULL;

UPDATE complaints
SET external_sync_attempts = 0
WHERE external_sync_attempts IS NULL;

ALTER TABLE complaints
    ALTER COLUMN external_sync_status SET DEFAULT 'PENDING',
    ALTER COLUMN external_sync_status SET NOT NULL,
    ALTER COLUMN external_sync_attempts SET DEFAULT 0,
    ALTER COLUMN external_sync_attempts SET NOT NULL;

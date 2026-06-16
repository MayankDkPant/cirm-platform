-- ============================================================================
-- V53: Harden service_request idempotency key with a unique constraint.
--
-- BACKGROUND:
--   V35 added idempotency_key + governing_body_id as a non-unique composite
--   index (idx_sr_idempotency). The application-level idempotency guard in
--   ServiceRequestService.create() performs a check-before-insert against
--   idempotency_request, but there is a TOCTOU (time-of-check/time-of-use)
--   race window:
--
--     T1: check → not found
--     T2: check → not found
--     T1: save service_request → succeeds
--     T2: save service_request → succeeds (DUPLICATE!)
--     T1: save idempotency_request → succeeds
--     T2: save idempotency_request → fails on uq_idempotency_request_tenant_key
--
--   Without a unique constraint on service_request, the duplicate row already
--   exists by the time the idempotency_request constraint fires. The result is
--   two service_request rows for the same idempotency key, plus a 500 error.
--
-- FIX:
--   1. Drop the plain non-unique index from V35.
--   2. Add a partial unique index on (governing_body_id, idempotency_key)
--      where idempotency_key IS NOT NULL.
--
--   This makes the DB the last line of defense: even if both transactions pass
--   the application-level check simultaneously, the second INSERT into
--   service_request will fail on this unique constraint — preventing the
--   duplicate row before any further damage can occur.
--
--   NULL idempotency keys (requests submitted without the header) are excluded
--   from the uniqueness check, preserving the ability to submit without a key.
--
-- ROLLBACK:
--   DROP INDEX IF EXISTS uq_sr_idempotency;
--   CREATE INDEX IF NOT EXISTS idx_sr_idempotency
--       ON service_request(idempotency_key, governing_body_id)
--       WHERE idempotency_key IS NOT NULL;
-- ============================================================================

-- Step 1: drop the old non-unique index
DROP INDEX IF EXISTS idx_sr_idempotency;

-- Step 2: add partial unique index — only non-null idempotency keys are constrained
CREATE UNIQUE INDEX uq_sr_idempotency
    ON service_request(governing_body_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

COMMENT ON INDEX uq_sr_idempotency IS
    'Enforces that a given idempotency_key can be used at most once per governing body. '
    'Partial (WHERE NOT NULL): requests submitted without a key are excluded. '
    'Seals the TOCTOU window left by the application-level check-before-insert guard. '
    'See V35 for the non-unique predecessor and ServiceRequestService.create() for the '
    'application-layer idempotency logic.';

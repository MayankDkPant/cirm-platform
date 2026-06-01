-- ============================================================================
-- V52: Harden service_request_event FK to RESTRICT — preserve audit immutability.
--
-- BACKGROUND:
--   V36 created service_request_event with:
--       FOREIGN KEY (service_request_id) REFERENCES service_request(id) ON DELETE CASCADE
--
--   This contradicts the table's own invariant: records are append-only and must
--   never be deleted. A CASCADE here would silently erase the entire audit trail
--   of a service request if that request were ever deleted — violating the
--   platform's audit preservation mandate.
--
-- FIX:
--   Replace CASCADE with RESTRICT.
--   RESTRICT causes the service_request DELETE to fail with a FK violation if
--   any events exist, making the audit trail an active guard against data loss.
--   If a service request must be retired, the application layer must archive or
--   tombstone it — direct deletion is blocked.
--
-- ROLLBACK:
--   ALTER TABLE service_request_event DROP CONSTRAINT fk_sre_service_request;
--   ALTER TABLE service_request_event
--       ADD CONSTRAINT fk_sre_service_request
--           FOREIGN KEY (service_request_id)
--           REFERENCES service_request(id)
--           ON DELETE CASCADE;
-- ============================================================================

-- Step 1: drop the cascade constraint
ALTER TABLE service_request_event
    DROP CONSTRAINT fk_sre_service_request;

-- Step 2: re-add with RESTRICT — deletion of a service_request is blocked while events exist
ALTER TABLE service_request_event
    ADD CONSTRAINT fk_sre_service_request
        FOREIGN KEY (service_request_id)
        REFERENCES service_request(id)
        ON DELETE RESTRICT;

COMMENT ON COLUMN service_request_event.service_request_id IS
    'FK to service_request(id). ON DELETE RESTRICT — a service request cannot be '
    'deleted while audit events reference it. This is intentional: events are '
    'append-only and must outlive the request they document.';

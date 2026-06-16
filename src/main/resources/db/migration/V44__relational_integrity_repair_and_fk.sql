-- ============================================================================
-- Relational integrity repair and FK constraint enforcement.
--
-- BACKGROUND:
--   Several UUID audit/reference columns were added without FK constraints.
--   LocalDevContextFilter previously injected a synthetic UUID
--   (00000000-0000-0000-0000-000000000099) as the "dev user" — this UUID
--   never existed in users, creating orphaned references in
--   created_by_user_id / updated_by_user_id / actor_user_id.
--   That filter now provisions a real dev user (dev-local@cxp.internal) via
--   INSERT ... ON CONFLICT DO NOTHING before any request is processed.
--
-- STRATEGY (safe migration order):
--   1. Repair: NULL-out every orphaned reference so no row violates an FK.
--   2. Constrain: add FK constraints now that data is clean.
--   3. Index: cover the new FK columns for common lookup patterns.
--
-- All FK columns are nullable → NULL is a valid, integrity-safe fallback for
-- rows where the actor/reference is unknown or predates the constraint.
--
-- Pre-migration validation queries (run manually if needed):
--   SELECT COUNT(*) FROM service_request
--     WHERE created_by_user_id IS NOT NULL
--       AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = service_request.created_by_user_id);
--   SELECT COUNT(*) FROM service_request
--     WHERE ward_id IS NOT NULL
--       AND NOT EXISTS (SELECT 1 FROM ward w WHERE w.id = service_request.ward_id);
--   SELECT COUNT(*) FROM announcement
--     WHERE created_by_user_id IS NOT NULL
--       AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = announcement.created_by_user_id);
--   Expected result: all queries return 0 rows (clean data) before applying constraints.
--
-- Tables covered:
--   service_request       created_by_user_id, updated_by_user_id, ward_id, department_id
--   service_request_event actor_user_id
--   announcement          created_by_user_id, updated_by_user_id
-- ============================================================================


-- ── STEP 1: Data repair — service_request user audit ─────────────────────────
-- NULL-out created_by_user_id / updated_by_user_id where the UUID is not in users.
-- Covers the synthetic dev UUID and any other orphaned value stored before real
-- user provisioning was in place.

UPDATE service_request
SET created_by_user_id = NULL
WHERE created_by_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = service_request.created_by_user_id);

UPDATE service_request
SET updated_by_user_id = NULL
WHERE updated_by_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = service_request.updated_by_user_id);


-- ── STEP 2: Data repair — service_request_event actor ────────────────────────

UPDATE service_request_event
SET actor_user_id = NULL
WHERE actor_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = service_request_event.actor_user_id);


-- ── STEP 3: Data repair — announcement user audit ─────────────────────────────

UPDATE announcement
SET created_by_user_id = NULL
WHERE created_by_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = announcement.created_by_user_id);

UPDATE announcement
SET updated_by_user_id = NULL
WHERE updated_by_user_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = announcement.updated_by_user_id);


-- ── STEP 4: Data repair — service_request geographic references ───────────────
-- NULL-out ward_id / department_id where the UUID is not in the referenced table.
-- In practice these should be clean (ward comes from GPS→ward lookup), but we
-- validate before constraining to guarantee the migration is always safe.

UPDATE service_request
SET ward_id = NULL
WHERE ward_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM ward w WHERE w.id = service_request.ward_id);

UPDATE service_request
SET department_id = NULL
WHERE department_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM department d WHERE d.id = service_request.department_id);


-- ── STEP 5: FK constraints — service_request user audit ──────────────────────
-- ON DELETE SET NULL: preserve the service request row if a user is ever deleted;
-- the audit link is lost but the citizen's record is retained.

ALTER TABLE service_request
    ADD CONSTRAINT fk_service_request_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL,

    ADD CONSTRAINT fk_service_request_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL;


-- ── STEP 6: FK constraints — service_request geographic references ────────────
-- ON DELETE SET NULL: preserve service requests if geographic master data is ever
-- reorganised; ward/department FK becomes null (record is not lost).

ALTER TABLE service_request
    ADD CONSTRAINT fk_service_request_ward
        FOREIGN KEY (ward_id) REFERENCES ward(id)
        ON DELETE SET NULL,

    ADD CONSTRAINT fk_service_request_department
        FOREIGN KEY (department_id) REFERENCES department(id)
        ON DELETE SET NULL;


-- ── STEP 7: FK constraint — service_request_event actor ──────────────────────
-- Event records are immutable append-only rows. ON DELETE SET NULL keeps the
-- event row but clears the actor reference if the user is ever removed, so that
-- audit history is preserved without a dangling FK.

ALTER TABLE service_request_event
    ADD CONSTRAINT fk_service_request_event_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id)
        ON DELETE SET NULL;


-- ── STEP 8: FK constraints — announcement user audit ─────────────────────────
-- ON DELETE SET NULL: retain announcements if the authoring operator account is
-- ever deactivated or deleted.

ALTER TABLE announcement
    ADD CONSTRAINT fk_announcement_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL,

    ADD CONSTRAINT fk_announcement_updated_by
        FOREIGN KEY (updated_by_user_id) REFERENCES users(id)
        ON DELETE SET NULL;


-- ── STEP 9: Indexes on new FK columns ────────────────────────────────────────

-- "My Requests" citizen feed: service requests submitted by a specific user.
CREATE INDEX IF NOT EXISTS idx_sr_created_by_user
    ON service_request(created_by_user_id)
    WHERE created_by_user_id IS NOT NULL;

-- Operator activity: service requests last-updated by a specific operator.
CREATE INDEX IF NOT EXISTS idx_sr_updated_by_user
    ON service_request(updated_by_user_id)
    WHERE updated_by_user_id IS NOT NULL;

-- Audit timeline: all events attributed to a specific actor.
CREATE INDEX IF NOT EXISTS idx_sre_actor_user
    ON service_request_event(actor_user_id)
    WHERE actor_user_id IS NOT NULL;

-- Announcement authorship lookup.
CREATE INDEX IF NOT EXISTS idx_ann_created_by_user
    ON announcement(created_by_user_id)
    WHERE created_by_user_id IS NOT NULL;


-- ── Column comments ───────────────────────────────────────────────────────────

COMMENT ON COLUMN service_request.created_by_user_id IS
    'Citizen who submitted this request. FK → users(id) ON DELETE SET NULL. '
    'NULL for requests submitted before real user provisioning was in place.';

COMMENT ON COLUMN service_request.updated_by_user_id IS
    'Operator or system actor who last modified this record. FK → users(id) ON DELETE SET NULL. '
    'NULL when no authenticated update has occurred.';

COMMENT ON COLUMN service_request.ward_id IS
    'Ward resolved from GPS coordinates via the ward-locator chain. FK → ward(id) ON DELETE SET NULL. '
    'NULL when location could not be resolved to a known ward.';

COMMENT ON COLUMN service_request.department_id IS
    'Department assigned to handle this request. FK → department(id) ON DELETE SET NULL. '
    'NULL until an operator assigns a department.';

COMMENT ON COLUMN service_request_event.actor_user_id IS
    'User who triggered this event. FK → users(id) ON DELETE SET NULL. '
    'NULL for system-generated events (scheduled expiry, external sync callbacks).';

COMMENT ON COLUMN announcement.created_by_user_id IS
    'Operator who created this announcement. FK → users(id) ON DELETE SET NULL. '
    'NULL for announcements created before user audit linkage was enforced.';

COMMENT ON COLUMN announcement.updated_by_user_id IS
    'Operator who last modified this announcement. FK → users(id) ON DELETE SET NULL. '
    'NULL when no authenticated update has occurred after constraint enforcement.';

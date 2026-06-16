-- ============================================================================
-- V51: Make user_profile audit timestamps non-nullable and add DB defaults.
--
-- BACKGROUND:
--   created_at and updated_at were added in V8 as nullable TIMESTAMP WITH TIME
--   ZONE columns. The UserProfile entity had no Java mapping for them, so they
--   were silently ignored on every write — leaving all historical rows with NULL.
--
--   This migration:
--     1. Backfills any NULL rows with the current timestamp (migration-time instant).
--        This is a one-shot approximation; the "real" values are unknown for legacy
--        records, but a non-null sentinel is needed before adding NOT NULL.
--     2. Adds DEFAULT CURRENT_TIMESTAMP so DB-level inserts (e.g., direct SQL,
--        seed scripts) also get a timestamp without requiring application logic.
--     3. Adds NOT NULL constraints so the application-managed @PrePersist /
--        @PreUpdate lifecycle callbacks become the canonical write path, enforced
--        at both the JPA and DB layers.
--
-- ROLLBACK:
--   ALTER TABLE user_profile ALTER COLUMN created_at DROP NOT NULL;
--   ALTER TABLE user_profile ALTER COLUMN updated_at  DROP NOT NULL;
--   ALTER TABLE user_profile ALTER COLUMN created_at DROP DEFAULT;
--   ALTER TABLE user_profile ALTER COLUMN updated_at  DROP DEFAULT;
-- ============================================================================

-- Step 1: backfill existing NULL values before adding NOT NULL constraint
UPDATE user_profile
SET created_at = CURRENT_TIMESTAMP
WHERE created_at IS NULL;

UPDATE user_profile
SET updated_at = CURRENT_TIMESTAMP
WHERE updated_at IS NULL;

-- Step 2: add DB-level defaults so direct SQL inserts are always stamped
ALTER TABLE user_profile
    ALTER COLUMN created_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN updated_at  SET DEFAULT CURRENT_TIMESTAMP;

-- Step 3: add NOT NULL constraint — safe after Step 1 has backfilled all rows
ALTER TABLE user_profile
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at  SET NOT NULL;

COMMENT ON COLUMN user_profile.created_at IS
    'Profile record creation timestamp. Set once by @PrePersist; never updated. '
    'Application-managed — do not write directly. Backfilled from migration-time '
    'for records created before V51.';

COMMENT ON COLUMN user_profile.updated_at IS
    'Last profile mutation timestamp. Refreshed by @PreUpdate on every save. '
    'Application-managed — do not write directly.';

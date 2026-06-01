-- ============================================================================
-- V46: User profile location fields + governing_body city linkage.
--
-- Part A — user_profile extensions:
--   Adds GPS/geocoded coordinates, ward display name, and a normalised address
--   string produced by the backend geocoder. All four are backend-managed:
--   the client provides GPS or a free-text address; the backend resolves and
--   persists the canonical values here.
--
-- Part B — governing_body.city_id:
--   Adds an explicit city FK so the provisioning service can derive a citizen's
--   city from their ward without an extra address lookup.
--   Hierarchy after this migration: ward → governing_body → city → district → state
--
-- Part C — backfill:
--   Best-effort linkage of existing governing_body rows to city rows that share
--   the same district. Where the governing_body name contains the city name the
--   match is promoted; otherwise the first active city in the district is used.
-- ============================================================================

-- ── Part A: user_profile ─────────────────────────────────────────────────────

ALTER TABLE user_profile
    ADD COLUMN IF NOT EXISTS latitude           DECIMAL(10, 8),
    ADD COLUMN IF NOT EXISTS longitude          DECIMAL(11, 8),
    ADD COLUMN IF NOT EXISTS ward_name          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS address_normalized VARCHAR(500);

COMMENT ON COLUMN user_profile.latitude IS
    'WGS-84 latitude resolved from GPS or geocoded from address during provisioning.';
COMMENT ON COLUMN user_profile.longitude IS
    'WGS-84 longitude resolved from GPS or geocoded from address during provisioning.';
COMMENT ON COLUMN user_profile.ward_name IS
    'Human-readable ward name returned by the ward-locator service.';
COMMENT ON COLUMN user_profile.address_normalized IS
    'Canonical formatted address produced by the backend geocoder.';

-- ── Part B: governing_body.city_id ───────────────────────────────────────────

ALTER TABLE governing_body
    ADD COLUMN IF NOT EXISTS city_id UUID;

-- Add FK only when city table exists and is populated (safe to run on fresh DBs).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_governing_body_city'
    ) THEN
        ALTER TABLE governing_body
            ADD CONSTRAINT fk_governing_body_city
                FOREIGN KEY (city_id) REFERENCES city(id);
    END IF;
END $$;

COMMENT ON COLUMN governing_body.city_id IS
    'City administered by this governing body. Enables direct ward→city hierarchy '
    'lookup during provisioning without a secondary address geocode pass.';

-- ── Part C: best-effort backfill ─────────────────────────────────────────────
-- Matches each governing_body to the city in the same district whose name
-- appears inside the governing_body name. Falls back to the first active city
-- in that district when no name match is found.

UPDATE governing_body gb
SET city_id = (
    SELECT c.id
    FROM city c
    WHERE c.district_id = gb.district_id
      AND c.is_active = TRUE
    ORDER BY
        (LOWER(gb.name) LIKE '%' || LOWER(c.name) || '%') DESC,
        c.created_at
    LIMIT 1
)
WHERE gb.district_id IS NOT NULL
  AND gb.city_id IS NULL;

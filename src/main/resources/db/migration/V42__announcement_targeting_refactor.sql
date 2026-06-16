-- ============================================================================
-- Announcement targeting refactor.
--
-- BEFORE: announcements used a visibility model (PRIVATE / CITY_PUBLIC / WARD_PUBLIC)
--         borrowed from ServiceRequest — conceptually wrong.
--
-- AFTER:  announcements use a geographic targeting model (WARD / ZONE / CITY /
--         DISTRICT / STATE). All announcements are public civic communications
--         by design. There is no PRIVATE or INTERNAL announcement concept.
--
-- Changes:
--   1. Add target_scope column (replaces visibility_scope).
--   2. Migrate existing visibility_scope values to target_scope.
--   3. Add geo FK columns: zone_id, city_id, district_id, state_id.
--      (ward_id already exists — kept as-is.)
--   4. Drop old columns: is_public, visibility_scope, locality_id.
--   5. Add FK constraints on new geo columns.
--   6. Rebuild indexes that referenced is_public.
-- ============================================================================

-- ── Step 1: add target_scope as nullable, populate, then enforce NOT NULL ─────

ALTER TABLE announcement ADD COLUMN IF NOT EXISTS target_scope VARCHAR(30);

UPDATE announcement
SET target_scope = CASE
    WHEN visibility_scope = 'WARD_PUBLIC' THEN 'WARD'
    ELSE 'CITY'
END;

ALTER TABLE announcement ALTER COLUMN target_scope SET NOT NULL;

-- ── Step 2: add geographic reference columns ──────────────────────────────────

ALTER TABLE announcement
    ADD COLUMN IF NOT EXISTS zone_id     UUID,
    ADD COLUMN IF NOT EXISTS city_id     UUID,
    ADD COLUMN IF NOT EXISTS district_id UUID,
    ADD COLUMN IF NOT EXISTS state_id    UUID;

-- ── Step 3: drop old columns ─────────────────────────────────────────────────

ALTER TABLE announcement
    DROP COLUMN IF EXISTS is_public,
    DROP COLUMN IF EXISTS visibility_scope,
    DROP COLUMN IF EXISTS locality_id;

-- ── Step 4: FK constraints on new geo columns ────────────────────────────────

ALTER TABLE announcement
    ADD CONSTRAINT fk_announcement_zone
        FOREIGN KEY (zone_id)     REFERENCES zone(id),
    ADD CONSTRAINT fk_announcement_city
        FOREIGN KEY (city_id)     REFERENCES city(id),
    ADD CONSTRAINT fk_announcement_district
        FOREIGN KEY (district_id) REFERENCES district(id),
    ADD CONSTRAINT fk_announcement_state
        FOREIGN KEY (state_id)    REFERENCES state(id);

-- ward_id FK was not explicitly constrained in V39; add it now.
ALTER TABLE announcement
    ADD CONSTRAINT fk_announcement_ward
        FOREIGN KEY (ward_id) REFERENCES ward(id);

-- ── Step 5: rebuild indexes ───────────────────────────────────────────────────

-- Drop old partial indexes that filtered on is_public (column no longer exists).
DROP INDEX IF EXISTS idx_ann_public_feed;
DROP INDEX IF EXISTS idx_ann_ward;

-- Civic feed: all published, active, non-expired announcements, pinned first.
-- No is_public filter — all announcements are public by definition.
CREATE INDEX IF NOT EXISTS idx_ann_feed
    ON announcement(governing_body_id, published_at DESC, pinned DESC)
    WHERE status = 'PUBLISHED' AND is_active = TRUE;

-- Target-scope filter: lets the audience resolver quickly find announcements
-- at a given geographic scope (CITY, DISTRICT, STATE, etc.).
CREATE INDEX IF NOT EXISTS idx_ann_target_scope
    ON announcement(governing_body_id, target_scope)
    WHERE status = 'PUBLISHED' AND is_active = TRUE;

-- Ward-scoped feed sub-filter.
CREATE INDEX IF NOT EXISTS idx_ann_ward
    ON announcement(ward_id)
    WHERE ward_id IS NOT NULL AND status = 'PUBLISHED';

-- ── Update column comments ────────────────────────────────────────────────────

COMMENT ON COLUMN announcement.target_scope IS
    'Geographic audience of this announcement: WARD | ZONE | CITY | DISTRICT | STATE. '
    'Exactly one corresponding FK (ward_id, zone_id, city_id, district_id, state_id) '
    'must be populated. All announcements are public civic communications — no PRIVATE scope.';

COMMENT ON COLUMN announcement.ward_id IS
    'Populated when target_scope = WARD. Targets citizens of a specific ward.';

COMMENT ON COLUMN announcement.zone_id IS
    'Populated when target_scope = ZONE. Targets citizens of all wards in a zone.';

COMMENT ON COLUMN announcement.city_id IS
    'Populated when target_scope = CITY. Targets all citizens of the city.';

COMMENT ON COLUMN announcement.district_id IS
    'Populated when target_scope = DISTRICT. Targets all citizens in an administrative district.';

COMMENT ON COLUMN announcement.state_id IS
    'Populated when target_scope = STATE. State-wide advisory (emergency, policy, etc.).';

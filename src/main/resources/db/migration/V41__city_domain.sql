-- ============================================================================
-- City domain — urban settlement unit in the governance hierarchy.
--
-- Hierarchy: state → district → city → governing_body → zone → ward
--
-- A city (e.g., Dehradun, Haridwar) is the geographic unit that a governing
-- body (e.g., Dehradun Municipal Corporation) administers. Announcements with
-- target_scope = CITY reference a city_id to target all citizens of that city.
--
-- Design note: city uses clean snake_case column names (code, not Code__c)
-- since it is a new platform-native entity, not a Salesforce migration.
-- ============================================================================

CREATE TABLE IF NOT EXISTS city (
    id          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(50),
    district_id UUID         NOT NULL,
    state_id    UUID         NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_city_district FOREIGN KEY (district_id) REFERENCES district(id),
    CONSTRAINT fk_city_state    FOREIGN KEY (state_id)    REFERENCES state(id),
    CONSTRAINT uq_city_district_name UNIQUE (district_id, name)
);

CREATE INDEX idx_city_district ON city(district_id);
CREATE INDEX idx_city_state    ON city(state_id);

COMMENT ON TABLE city IS
    'Urban settlement (city/town) within a district. '
    'Anchor for city-level announcement targeting and governing body association.';

COMMENT ON COLUMN city.code IS
    'Platform-native short code for the city (e.g. UK-DDN). '
    'snake_case — does not mirror Salesforce Code__c naming.';

-- ── Seed: cities matching existing governing bodies ───────────────────────────
-- Uses Code__c join on district/state which is the existing convention for those
-- legacy tables (introduced by V13).

INSERT INTO city (name, code, district_id, state_id)
SELECT v.name, v.code, d.id, s.id
FROM (VALUES
    ('Dehradun',  'UK-DDN', 'UK-DDN07', 'UK'),
    ('Rishikesh', 'UK-RSK', 'UK-DDN07', 'UK'),
    ('Haridwar',  'UK-HRW', 'UK-HRW08', 'UK'),
    ('Roorkee',   'UK-RKE', 'UK-HRW08', 'UK')
) AS v(name, code, district_code, state_code)
JOIN district d ON d."Code__c" = v.district_code
JOIN state    s ON s."Code__c" = v.state_code;

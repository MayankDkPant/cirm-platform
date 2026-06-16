-- ============================================================================
-- Announcement seed refresh — replaces V40 seed with geo-targeted samples.
--
-- Four announcements covering each major targeting scope:
--   CITY     — water outage advisory for Dehradun city
--   WARD     — road repair notice for a specific ward
--   DISTRICT — health advisory for Dehradun district
--   STATE    — weather emergency for Uttarakhand
-- ============================================================================

-- Clear old seed data before re-inserting with new targeting model.
TRUNCATE TABLE announcement;

WITH
    gb   AS (SELECT id FROM governing_body ORDER BY created_at LIMIT 1),
    city AS (SELECT id FROM city WHERE name = 'Dehradun' LIMIT 1),
    ward AS (SELECT w.id FROM ward w
             JOIN governing_body gb2 ON w.governing_body_id = gb2.id
             ORDER BY w.created_at LIMIT 1),
    dist AS (SELECT id FROM district WHERE "Code__c" = 'UK-DDN07' LIMIT 1),
    st   AS (SELECT id FROM state   WHERE "Code__c" = 'UK'       LIMIT 1)

INSERT INTO announcement (
    id,
    title, content, summary, category, priority, status,
    governing_body_id,
    target_scope, ward_id, zone_id, city_id, district_id, state_id,
    published_at, expires_at, pinned,
    metadata, tags,
    created_by_user_id, updated_by_user_id,
    is_active, created_at, updated_at
)
SELECT
    uuid_generate_v4(),
    v.title, v.content, v.summary, v.category, v.priority, v.status,
    gb.id,
    v.target_scope,
    CASE WHEN v.target_scope = 'WARD'     THEN ward.id     ELSE NULL END,
    NULL,
    CASE WHEN v.target_scope = 'CITY'     THEN city.id     ELSE NULL END,
    CASE WHEN v.target_scope = 'DISTRICT' THEN dist.id     ELSE NULL END,
    CASE WHEN v.target_scope = 'STATE'    THEN st.id       ELSE NULL END,
    v.published_at, v.expires_at, v.pinned,
    v.metadata, v.tags,
    NULL, NULL,
    TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gb, city, ward, dist, st
CROSS JOIN (VALUES
    (
        'Water Supply Disruption — Dehradun City',
        'Scheduled maintenance will disrupt water supply across Dehradun city on 22 May 2026 between 09:00 and 17:00. Please store water in advance. Work is being carried out to upgrade the main pipeline feeding the east and west zones. Emergency water tankers will be deployed at Ward 3, Ward 7, and Ward 12. We regret any inconvenience caused.',
        'Water supply off across Dehradun on 22 May, 9 AM–5 PM. Emergency tankers deployed.',
        'WATER',
        'IMPORTANT',
        'PUBLISHED',
        'CITY',
        CURRENT_TIMESTAMP,
        (CURRENT_TIMESTAMP + INTERVAL '7 days'),
        TRUE,
        '{"contact": "1800-XXX-XXXX", "tanker_wards": ["Ward 3", "Ward 7", "Ward 12"]}',
        '["water", "maintenance", "city-wide"]'
    ),
    (
        'Road Resurfacing — MG Road Ward',
        'The Municipal Corporation will carry out road resurfacing work on MG Road from 25 May to 2 June 2026. Residents of this ward should expect traffic diversions during work hours (08:00–18:00). Use alternative routes via Clock Tower and Rajpur Road. Emergency vehicles will not be affected.',
        'MG Road closed for resurfacing 25 May – 2 June. Use Clock Tower alternate route.',
        'ROADS',
        'NORMAL',
        'PUBLISHED',
        'WARD',
        CURRENT_TIMESTAMP,
        (CURRENT_TIMESTAMP + INTERVAL '14 days'),
        FALSE,
        NULL,
        '["road", "traffic", "construction", "ward-notice"]'
    ),
    (
        'Dengue Prevention Drive — Dehradun District',
        'The District Health Authority is conducting a dengue prevention drive across all wards in Dehradun district from 1 to 15 June 2026. Health workers will visit every household for inspection of stagnant water and free distribution of larvicide. Citizens are requested to cooperate and ensure containers are emptied before the inspection teams arrive.',
        'District-wide dengue prevention drive 1–15 June. Health teams visiting all households.',
        'HEALTH',
        'IMPORTANT',
        'PUBLISHED',
        'DISTRICT',
        CURRENT_TIMESTAMP,
        (CURRENT_TIMESTAMP + INTERVAL '30 days'),
        FALSE,
        '{"helpline": "104", "authority": "District Health Authority"}',
        '["health", "dengue", "district", "prevention"]'
    ),
    (
        'Red Alert — Heavy Rainfall Warning, Uttarakhand',
        'The Uttarakhand Disaster Management Authority has issued a Red Alert for heavy to very heavy rainfall across the state from 20–23 May 2026. Citizens in low-lying areas and near rivers are advised to move to higher ground. All schools and colleges will remain closed on 21 and 22 May. Emergency helpline: 1077.',
        'Red Alert: Heavy rainfall 20–23 May. Low-lying residents advised to evacuate. Schools closed 21–22 May.',
        'EMERGENCY',
        'URGENT',
        'PUBLISHED',
        'STATE',
        CURRENT_TIMESTAMP,
        (CURRENT_TIMESTAMP + INTERVAL '5 days'),
        TRUE,
        '{"helpline": "1077", "authority": "UKDMA", "alert_level": "RED"}',
        '["emergency", "weather", "flood", "state-alert"]'
    )
) AS v(
    title, content, summary, category, priority, status,
    target_scope,
    published_at, expires_at, pinned,
    metadata, tags
);

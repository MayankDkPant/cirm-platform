-- ============================================================================
-- Seed: sample announcements for local development.
-- References the first governing body in the DB so the seed works regardless
-- of which UUID was assigned during the governance migration.
-- ============================================================================

WITH gb AS (
    SELECT id FROM governing_body LIMIT 1
)
INSERT INTO announcement (
    id, title, content, summary, category, priority, status,
    governing_body_id, ward_id, locality_id,
    is_public, visibility_scope,
    published_at, expires_at, pinned,
    metadata, tags,
    created_by_user_id, updated_by_user_id,
    is_active, created_at, updated_at
)
SELECT
    uuid_generate_v4(),
    v.title, v.content, v.summary, v.category, v.priority, v.status,
    gb.id, NULL, NULL,
    v.is_public, v.visibility_scope,
    v.published_at, v.expires_at, v.pinned,
    v.metadata, v.tags,
    NULL, NULL,
    TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM gb
CROSS JOIN (
    VALUES
    (
        'Water Supply Disruption — Rajpur Area',
        'Scheduled maintenance will disrupt water supply in Rajpur ward on 22 May 2026 between 09:00 and 17:00. Please store water in advance. Work is being carried out to upgrade the main pipeline. We regret any inconvenience caused.',
        'Water supply off in Rajpur on 22 May, 9 AM–5 PM.',
        'WATER', 'IMPORTANT', 'PUBLISHED',
        TRUE, 'CITY_PUBLIC',
        CURRENT_TIMESTAMP, (CURRENT_TIMESTAMP + INTERVAL '7 days'), TRUE,
        '{"contact": "1800-XXX-XXXX"}', '["water", "maintenance", "rajpur"]'
    ),
    (
        'Road Resurfacing — MG Road',
        'The Municipal Corporation will carry out road resurfacing work on MG Road from 25 May to 2 June 2026. Expect traffic diversions. Use alternative routes via Clock Tower. Emergency vehicles will not be affected.',
        'MG Road closed for resurfacing 25 May – 2 June.',
        'ROADS', 'NORMAL', 'PUBLISHED',
        TRUE, 'CITY_PUBLIC',
        CURRENT_TIMESTAMP, (CURRENT_TIMESTAMP + INTERVAL '14 days'), FALSE,
        NULL, '["road", "traffic", "construction"]'
    ),
    (
        'New Solid Waste Collection Schedule',
        'Effective 1 June 2026, solid waste collection timings in all wards will change to 07:00–09:00 (morning) and 18:00–20:00 (evening). Citizens are requested to keep their bins outside during these windows. Door-to-door collection continues.',
        'Waste pickup now 7–9 AM and 6–8 PM from 1 June.',
        'SANITATION', 'NORMAL', 'PUBLISHED',
        TRUE, 'CITY_PUBLIC',
        CURRENT_TIMESTAMP, NULL, FALSE,
        NULL, '["waste", "sanitation", "schedule"]'
    ),
    (
        'Internal: Budget Review Meeting',
        'Senior officers are requested to attend the annual budget review meeting scheduled for 28 May 2026 at 10:00 in the Corporation Hall. Please bring department expenditure summaries.',
        'Internal budget review meeting on 28 May at 10 AM.',
        'INTERNAL', 'IMPORTANT', 'PUBLISHED',
        FALSE, 'PRIVATE',
        CURRENT_TIMESTAMP, NULL, FALSE,
        NULL, NULL
    )
) AS v(
    title, content, summary, category, priority, status,
    is_public, visibility_scope,
    published_at, expires_at, pinned,
    metadata, tags
);

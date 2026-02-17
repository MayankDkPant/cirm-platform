-- ============================================================
-- V16 : Backfill Zone Code__c
-- Generates canonical Zone codes from Governing Body + Zone number
-- Example: UK-DDN07-Z01
-- ============================================================

UPDATE zone z
SET "Code__c" =
    LEFT(gb."Code__c", 8)           -- remove -MC / -RMC suffix
    || '-Z' ||
    LPAD(REGEXP_REPLACE(z.name, '\D','','g'), 2, '0')
FROM governing_body gb
WHERE z.governing_body_id = gb.id
AND z."Code__c" IS NULL;

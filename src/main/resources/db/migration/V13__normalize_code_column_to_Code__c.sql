-- ============================================================
-- V13: Normalize code → "Code__c" on governance/admin tables
-- ============================================================
-- V2 and V5 created these tables with a lowercase `code` column.
-- V15 (masterdata seed) and all subsequent seed migrations expect
-- the Salesforce-aligned "Code__c" column name. This migration
-- bridges the gap so V15+ can execute.
--
-- PostgreSQL automatically updates all constraint and index
-- references when a column is renamed (pg_constraint is OID-based).
-- No constraint recreation is needed.
-- ============================================================


-- 1. governance_type (created V2)
ALTER TABLE governance_type RENAME COLUMN code TO "Code__c";

-- 2. governance_sub_type (created V2)
ALTER TABLE governance_sub_type RENAME COLUMN code TO "Code__c";

-- 3. state (created V5)
ALTER TABLE state RENAME COLUMN code TO "Code__c";

-- 4. governing_body (created V5)
ALTER TABLE governing_body RENAME COLUMN code TO "Code__c";

-- 5. department (created V5)
ALTER TABLE department RENAME COLUMN code TO "Code__c";

-- 6. department_unit (created V5)
ALTER TABLE department_unit RENAME COLUMN code TO "Code__c";

-- 7. district — add "Code__c" column
--    V12 created district with only (id, state_id, name, created_at).
--    V15 inserts district rows with "Code__c" and later joins on it.
ALTER TABLE district ADD COLUMN "Code__c" VARCHAR(100);

-- 8. zone — add "Code__c" column
--    V12 created zone with only (id, governing_body_id, name, created_at).
--    V16 fills this column via UPDATE; V17 joins on it.
ALTER TABLE zone ADD COLUMN "Code__c" VARCHAR(100);

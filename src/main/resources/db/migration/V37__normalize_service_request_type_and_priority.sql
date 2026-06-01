-- ============================================================================
-- Rename legacy ServiceRequestType values to canonical CXP taxonomy.
-- SERVICE  → ISSUE    (actionable problem needing resolution)
-- QUERY    → INQUIRY  (information request, no field action required)
--
-- Also upper-cases any stray lowercase priority values for enum alignment.
-- ============================================================================

UPDATE service_request SET type = 'ISSUE'   WHERE type = 'SERVICE';
UPDATE service_request SET type = 'INQUIRY' WHERE type = 'QUERY';

UPDATE service_request SET priority = UPPER(priority) WHERE priority IS NOT NULL;

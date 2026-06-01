-- ============================================================================
-- Drop legacy complaint tables and associated database objects.
--
-- The Complaint domain has been superseded by the ServiceRequest domain.
-- All new citizen intake flows use the service_request and service_request_event
-- tables. This migration safely removes the obsolete complaint schema.
--
-- Tables dropped:
--   complaint_event  (child — must be dropped before complaints)
--   complaints       (parent)
--
-- Associated objects dropped:
--   trg_complaint_event_immutable  (trigger on complaint_event)
--   enforce_complaint_event_immutable()  (trigger function)
--   Indexes are dropped automatically with their parent table.
-- ============================================================================

-- 1. Drop immutability trigger and function on complaint_event.
DROP TRIGGER IF EXISTS trg_complaint_event_immutable ON complaint_event;
DROP FUNCTION IF EXISTS enforce_complaint_event_immutable();

-- 2. Drop child table first (FK constraint from complaint_event → complaints).
DROP TABLE IF EXISTS complaint_event CASCADE;

-- 3. Drop parent table.
DROP TABLE IF EXISTS complaints CASCADE;

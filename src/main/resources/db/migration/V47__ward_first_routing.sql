-- V47: Ward-first routing — governing_body becomes optional metadata in service_request
--
-- Architecture evolution: ward_id + department_id are now the primary routing concepts.
-- governing_body_id is preserved as optional metadata (derived from the ward FK chain when available)
-- but is no longer a mandatory runtime requirement for service request submission.
--
-- This supports two operational modes without schema branching:
--   Mode A (ward-led pilot): ward resolves, governing_body may be absent
--   Mode B (city-wide utility): governing_body present as before

-- 1. Make governing_body_id nullable in service_request
ALTER TABLE service_request ALTER COLUMN governing_body_id DROP NOT NULL;

-- 2. Add ward-primary index — ward_id is now the primary routing and lookup column
CREATE INDEX IF NOT EXISTS idx_service_request_ward_created
    ON service_request (ward_id, created_at DESC)
    WHERE ward_id IS NOT NULL;

-- 3. Add department-primary index — supports future department-scoped operator queries
CREATE INDEX IF NOT EXISTS idx_service_request_dept_created
    ON service_request (department_id, created_at DESC)
    WHERE department_id IS NOT NULL;

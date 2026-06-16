-- ============================================================================
-- Add citizen-facing visibility controls to service_request.
--
-- is_public        : derived convenience flag — true when visibility_scope != PRIVATE.
--                    Allows cheap boolean filtering without parsing the enum string.
-- visibility_scope : persisted citizen intent for audience scope. Actual viewer
--                    resolution (who concretely sees the request) is deferred to
--                    runtime logic that combines ward_id, governing_body_id, city
--                    context, and citizen profile. This column is intent, not ACL.
--
-- Both columns default to the PRIVATE / false state so existing rows are
-- unaffected and public visibility must be explicitly opted into.
-- ============================================================================

ALTER TABLE service_request
    ADD COLUMN IF NOT EXISTS is_public        BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS visibility_scope VARCHAR(50) NOT NULL DEFAULT 'PRIVATE';

COMMENT ON COLUMN service_request.is_public IS
    'Derived convenience flag: true when visibility_scope is not PRIVATE.';

COMMENT ON COLUMN service_request.visibility_scope IS
    'Persisted citizen intent for audience scope (PRIVATE | CITY_PUBLIC | WARD_PUBLIC …). '
    'Actual viewer resolution happens at runtime via the audience-resolver service.';

-- Partial index — only public requests participate in civic-feed queries.
CREATE INDEX IF NOT EXISTS idx_sr_visibility_scope
    ON service_request(visibility_scope, governing_body_id)
    WHERE is_public = TRUE;

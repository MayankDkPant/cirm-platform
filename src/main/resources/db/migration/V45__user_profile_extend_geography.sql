-- ============================================================================
-- Extend user_profile with city and district geography columns.
--
-- BACKGROUND:
--   user_profile already carries ward_uuid, zone_uuid, state_uuid, and
--   municipality_uuid from V8. The provisioning response contract includes
--   cityId and districtId, so we add the two missing columns here.
--
-- These columns follow the same nullable UUID pattern as the existing geo
-- columns — populated by the provisioning service when resolvable, null when
-- the address cannot be mapped to the governance hierarchy.
-- ============================================================================

ALTER TABLE user_profile
    ADD COLUMN IF NOT EXISTS city_uuid     UUID,
    ADD COLUMN IF NOT EXISTS district_uuid UUID;

COMMENT ON COLUMN user_profile.city_uuid IS
    'City the citizen is associated with. Populated during provisioning when '
    'address resolves to a known city in the governance hierarchy. Nullable.';

COMMENT ON COLUMN user_profile.district_uuid IS
    'District the citizen is associated with. Populated during provisioning when '
    'address resolves to a known district. Nullable.';

-- ============================================================================
-- V54: Correct misleading geo field comments and document canonical semantics.
--
-- BACKGROUND:
--   Several geo columns carry comments that describe geocoder output, but the
--   current implementation stores user-supplied input or carries null values.
--   This migration replaces those comments with accurate descriptions of what
--   each field actually stores today, and calls out pending geocoding work
--   explicitly so future engineers do not inherit silent misunderstandings.
--
-- GEO FIELD TAXONOMY (applies to both user_profile and service_request):
--
--   CANONICAL ID       — authoritative FK reference into the governance master data.
--                        These are the truth: wardId, governingBodyId, cityId,
--                        districtId, stateId. Derived from the ward-locator chain
--                        or explicit ward selection. Never client-supplied.
--
--   GPS COORDINATE     — raw lat/lon from the device at time of submission.
--                        Optional: null when no coordinates were provided.
--                        Not re-derived or verified after storage.
--
--   DENORMALIZED LABEL — convenience display name cached at write time from the
--                        ward-locator service or governance master data.
--                        Examples: wardName, formattedAddress, city, district.
--                        May become stale if governance data is renamed via migration.
--                        Do not use for authoritative lookups — join canonical IDs instead.
--
--   USER-SUPPLIED TEXT — raw address input from the citizen. Never geocoded or
--                        normalized by the backend. Examples: address_line1,
--                        address_normalized (despite the misleading name),
--                        address_text in service_request.
--
-- ROLLBACK: COMMENT ON is idempotent and non-destructive — rollback is unnecessary.
-- ============================================================================

-- ── user_profile corrections ─────────────────────────────────────────────────

COMMENT ON COLUMN user_profile.address_normalized IS
    'USER-SUPPLIED TEXT (not geocoded). Stores the citizen''s free-text address as '
    'submitted during onboarding — raw input from req.address(). The name is '
    'aspirational: it will become geocoder-normalized output once a geocoding '
    'integration is added. Do not treat as a canonical or searchable address.';

COMMENT ON COLUMN user_profile.ward_name IS
    'DENORMALIZED LABEL. Ward display name returned by the ward-locator service at '
    'provisioning time. Cached for UI convenience — not updated if the ward''s name '
    'changes in master data. For authoritative name, join via ward_uuid → ward.name.';

COMMENT ON COLUMN user_profile.latitude IS
    'GPS COORDINATE (WGS-84). Latitude supplied by the mobile device at provisioning '
    'time. Precision: DECIMAL(10,8) ≈ ~11 mm accuracy at 8 decimal places. Null when '
    'no GPS was available. Not re-verified after storage.';

COMMENT ON COLUMN user_profile.longitude IS
    'GPS COORDINATE (WGS-84). Longitude supplied by the mobile device at provisioning '
    'time. Precision: DECIMAL(11,8) ≈ ~11 mm accuracy at 8 decimal places. Null when '
    'no GPS was available. Not re-verified after storage.';

-- ── service_request corrections ──────────────────────────────────────────────

COMMENT ON COLUMN service_request.formatted_address IS
    'DENORMALIZED LABEL (currently always null). Intended to receive a geocoder-formatted '
    'address string from the ward-locator service. The ward-locator currently returns '
    'wardNo + wardName only — it does not return a formatted address. This column will '
    'remain null until a geocoding pass is added to the location intelligence service. '
    'See DefaultLocationIntelligenceService and WardLocatorResponse.';

COMMENT ON COLUMN service_request.ward_name IS
    'DENORMALIZED LABEL. Ward display name resolved from GPS coordinates at creation '
    'time via the ward-locator service. Cached for query convenience — not updated if '
    'the ward''s name changes in master data. For authoritative name, join via '
    'ward_id → ward.name. Null when no ward was resolved.';

COMMENT ON COLUMN service_request.address_text IS
    'USER-SUPPLIED TEXT. Free-text address provided by the citizen at submission time. '
    'Not geocoded or normalized. Used for display only. Falls back to formatted_address '
    'when absent — which is currently always null (see formatted_address comment).';

COMMENT ON COLUMN service_request.latitude IS
    'GPS COORDINATE (WGS-84). Latitude supplied by the mobile device at submission '
    'time. Precision: DOUBLE PRECISION (≈ 15 significant digits). Null when no GPS '
    'was provided. Note: user_profile stores coordinates as DECIMAL(10,8) — the types '
    'differ but precision is equivalent for real-world GPS accuracy.';

COMMENT ON COLUMN service_request.longitude IS
    'GPS COORDINATE (WGS-84). Longitude supplied by the mobile device at submission '
    'time. Precision: DOUBLE PRECISION (≈ 15 significant digits). Null when no GPS '
    'was provided. Note: user_profile stores coordinates as DECIMAL(11,8).';

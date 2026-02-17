/* ============================================================
   CIRM PLATFORM — GOVERNANCE & IDEMPOTENCY FOUNDATION
   Version: V12
   Author: CIRM Backend
   Purpose:
     1. Introduce District layer in governance hierarchy
     2. Introduce Zone & Ward administrative areas
     3. Add global complaint reference number sequence
     4. Add idempotency request table for API safety
   ============================================================ */


/* ============================================================
   1. DISTRICT TABLE
   ------------------------------------------------------------
   Every Urban Local Body and Gram Panchayat belongs to a district.
   This enables:
     - District dashboards
     - Disaster workflows
     - State-level rollout
   ============================================================ */

CREATE TABLE district (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    state_id UUID NOT NULL REFERENCES state(id),

    name VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_district_state_name UNIQUE (state_id, name)
);

COMMENT ON TABLE district IS 'Administrative district. Parent of all local governing bodies.';
COMMENT ON COLUMN district.state_id IS 'State to which the district belongs.';



/* ============================================================
   2. LINK GOVERNING BODY / MUNICIPALITY TO DISTRICT
   ------------------------------------------------------------
   Existing table assumed: governing_body
   (Your governance foundation already created it)
   ============================================================ */

ALTER TABLE governing_body
ADD COLUMN district_id UUID;

ALTER TABLE governing_body
ADD CONSTRAINT fk_governing_body_district
FOREIGN KEY (district_id) REFERENCES district(id);

COMMENT ON COLUMN governing_body.district_id IS
'District in which the governing body operates.';



/* ============================================================
   3. ZONE TABLE (URBAN ADMINISTRATIVE UNIT)
   ------------------------------------------------------------
   Zones are OPTIONAL and used only by larger municipalities.
   Example: Delhi, Mumbai, Bangalore, Dehradun (future).
   One Zone -> Many Wards
   ============================================================ */

CREATE TABLE zone (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    governing_body_id UUID NOT NULL REFERENCES governing_body(id),

    name VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_zone_body_name UNIQUE (governing_body_id, name)
);

COMMENT ON TABLE zone IS
'Administrative grouping of wards (primarily urban).';



/* ============================================================
   4. WARD TABLE (UNIVERSAL LOCAL REPRESENTATION UNIT)
   ------------------------------------------------------------
   Wards exist in BOTH:
     - Urban Local Bodies
     - Gram Panchayats

   zone_id is NULL for rural governing bodies.
   ============================================================ */

CREATE TABLE ward (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    governing_body_id UUID NOT NULL REFERENCES governing_body(id),
    zone_id UUID REFERENCES zone(id),

    name VARCHAR(255) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ward_body_name UNIQUE (governing_body_id, name)
);

COMMENT ON TABLE ward IS
'Electoral and administrative ward. Universal unit for rural and urban areas.';
COMMENT ON COLUMN ward.zone_id IS
'Nullable. Present only when wards belong to an urban zone.';



/* ============================================================
   5. GLOBAL COMPLAINT REFERENCE NUMBER SEQUENCE
   ------------------------------------------------------------
   Generates human-readable complaint numbers like:
   CIRM-2026-00000001

   Global (NOT per state).
   ============================================================ */

CREATE SEQUENCE complaint_reference_seq
START WITH 1
INCREMENT BY 1;

COMMENT ON SEQUENCE complaint_reference_seq IS
'Global sequence for complaint reference numbers.';



/* ============================================================
   6. IDEMPOTENCY REQUEST TABLE
   ------------------------------------------------------------
   Prevents duplicate complaint creation caused by retries.

   Flow:
     - Mobile sends Idempotency-Key header (UUID)
     - Backend stores response for 24 hours
     - Same key -> same response returned

   expires_at = created_at + 24 hours
   Old keys cleaned via scheduled job.
   ============================================================ */

CREATE TABLE idempotency_request (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    idempotency_key VARCHAR(255) NOT NULL,
    request_path VARCHAR(255) NOT NULL,

    response_status INT NOT NULL,
    response_body TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,

    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key)
);

COMMENT ON TABLE idempotency_request IS
'Stores idempotent API responses to prevent duplicate complaint creation.';
COMMENT ON COLUMN idempotency_request.expires_at IS
'Record validity window (typically 24 hours).';

CREATE INDEX idx_idempotency_expires_at
ON idempotency_request (expires_at);

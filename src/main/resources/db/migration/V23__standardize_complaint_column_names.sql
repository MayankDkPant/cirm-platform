-- Repair migration: fixes incorrect columns created in V22.
-- Ensures complaints table matches JPA entity and naming strategy.

-- 1️⃣ Drop wrongly created camelCase column if present
ALTER TABLE complaints
DROP COLUMN IF EXISTS "reportedLocationText";

-- 2️⃣ Add missing correct columns (safe to run multiple times)

ALTER TABLE complaints
ADD COLUMN IF NOT EXISTS reported_location_text TEXT;

ALTER TABLE complaints
ADD COLUMN IF NOT EXISTS municipality_id UUID;

ALTER TABLE complaints
ADD COLUMN IF NOT EXISTS municipality_name VARCHAR(255);

ALTER TABLE complaints
ADD COLUMN IF NOT EXISTS ward_id UUID;

ALTER TABLE complaints
ADD COLUMN IF NOT EXISTS ward_name VARCHAR(255);

ALTER TABLE complaints
ADD COLUMN IF NOT EXISTS formatted_address VARCHAR(500);
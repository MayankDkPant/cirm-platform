/* ============================================================
   CIRM Seed Data — Governance Foundation
   State: Uttarakhand
   Purpose: Minimal but realistic demo dataset
   ============================================================ */

-- ------------------------------------------------------------
-- Enable UUID extension (safe if already enabled)
-- ------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";


/* ============================================================
   1️⃣ STATE
   ============================================================ */

INSERT INTO state (id, name, code)
VALUES (
    uuid_generate_v4(),
    'Uttarakhand',
    'UK'
);


/* ============================================================
   2️⃣ DISTRICTS
   ============================================================ */

-- Dehradun (Urban focus district)
INSERT INTO district (id, state_id, name)
SELECT uuid_generate_v4(), id, 'Dehradun'
FROM state WHERE code = 'UK';

-- Haridwar (Mixed rural district)
INSERT INTO district (id, state_id, name)
SELECT uuid_generate_v4(), id, 'Haridwar'
FROM state WHERE code = 'UK';


/* ============================================================
   3️⃣ GOVERNING BODIES
   ============================================================ */

-- 🏙️ Dehradun Municipal Corporation (Urban)
INSERT INTO governing_body (id, district_id, name, type, subtype)
SELECT uuid_generate_v4(), id, 'Dehradun Municipal Corporation', 'URBAN', 'MUNICIPAL_CORPORATION'
FROM district WHERE name = 'Dehradun';


-- 🏙️ Rishikesh Municipal Council (Smaller urban body)
INSERT INTO governing_body (id, district_id, name, type, subtype)
SELECT uuid_generate_v4(), id, 'Rishikesh Municipal Council', 'URBAN', 'MUNICIPAL_COUNCIL'
FROM district WHERE name = 'Dehradun';


-- 🌾 Bahadrabad Gram Panchayat (Rural)
INSERT INTO governing_body (id, district_id, name, type, subtype)
SELECT uuid_generate_v4(), id, 'Bahadrabad Gram Panchayat', 'RURAL', 'GRAM_PANCHAYAT'
FROM district WHERE name = 'Haridwar';


/* ============================================================
   4️⃣ ZONES (Urban only — Dehradun MC)
   ============================================================ */

INSERT INTO zone (id, governing_body_id, name)
SELECT uuid_generate_v4(), id, 'North Zone'
FROM governing_body WHERE name = 'Dehradun Municipal Corporation';

INSERT INTO zone (id, governing_body_id, name)
SELECT uuid_generate_v4(), id, 'South Zone'
FROM governing_body WHERE name = 'Dehradun Municipal Corporation';


/* ============================================================
   5️⃣ WARDS
   ============================================================ */

-- ---- Dehradun MC Wards (with zones) ----

-- North Zone Wards
INSERT INTO ward (id, governing_body_id, zone_id, name, ward_number)
SELECT uuid_generate_v4(), gb.id, z.id, 'Rajpur Road', 1
FROM governing_body gb
JOIN zone z ON gb.id = z.governing_body_id
WHERE gb.name='Dehradun Municipal Corporation' AND z.name='North Zone';

INSERT INTO ward (id, governing_body_id, zone_id, name, ward_number)
SELECT uuid_generate_v4(), gb.id, z.id, 'Jakhan', 2
FROM governing_body gb
JOIN zone z ON gb.id = z.governing_body_id
WHERE gb.name='Dehradun Municipal Corporation' AND z.name='North Zone';

INSERT INTO ward (id, governing_body_id, zone_id, name, ward_number)
SELECT uuid_generate_v4(), gb.id, z.id, 'Balliwala', 3
FROM governing_body gb
JOIN zone z ON gb.id = z.governing_body_id
WHERE gb.name='Dehradun Municipal Corporation' AND z.name='North Zone';


-- South Zone Wards
INSERT INTO ward (id, governing_body_id, zone_id, name, ward_number)
SELECT uuid_generate_v4(), gb.id, z.id, 'Prem Nagar', 4
FROM governing_body gb
JOIN zone z ON gb.id = z.governing_body_id
WHERE gb.name='Dehradun Municipal Corporation' AND z.name='South Zone';

INSERT INTO ward (id, governing_body_id, zone_id, name, ward_number)
SELECT uuid_generate_v4(), gb.id, z.id, 'ISBT Area', 5
FROM governing_body gb
JOIN zone z ON gb.id = z.governing_body_id
WHERE gb.name='Dehradun Municipal Corporation' AND z.name='South Zone';


-- ---- Rishikesh Municipal Council Wards (no zones for simplicity) ----
INSERT INTO ward (id, governing_body_id, zone_id, name, ward_number)
SELECT uuid_generate_v4(), id, NULL, 'Triveni Ghat', 1
FROM governing_body WHERE name='Rishikesh Municipal Council';

INSERT INTO ward (id, governing_body_id, zone_id, name, ward_number)
SELECT uuid_generate_v4(), id, NULL, 'AIIMS Area', 2
FROM governing_body WHERE name='Rishikesh Municipal Council';


-- ---- Gram Panchayat Wards (rural → no zones) ----
INSERT INTO ward (id, governing_body_id, zone_id, name, ward_number)
SELECT uuid_generate_v4(), id, NULL, 'Bahadrabad Ward 1', 1
FROM governing_body WHERE name='Bahadrabad Gram Panchayat';


/* ============================================================
   6️⃣ DEPARTMENTS (Typical Municipal Functions)
   ============================================================ */

INSERT INTO department (id, name, description) VALUES
(uuid_generate_v4(), 'Sanitation', 'Waste collection and cleanliness'),
(uuid_generate_v4(), 'Water Supply', 'Drinking water and pipelines'),
(uuid_generate_v4(), 'Roads & Infrastructure', 'Roads, potholes and civil works'),
(uuid_generate_v4(), 'Street Lighting', 'Street light maintenance'),
(uuid_generate_v4(), 'Public Health', 'Mosquito control and public hygiene');


/* ============================================================
   7️⃣ DEPARTMENT UNITS
   ============================================================ */

-- Sanitation Units
INSERT INTO department_unit (id, department_id, name)
SELECT uuid_generate_v4(), id, 'Garbage Collection Unit'
FROM department WHERE name='Sanitation';

INSERT INTO department_unit (id, department_id, name)
SELECT uuid_generate_v4(), id, 'Drain Cleaning Unit'
FROM department WHERE name='Sanitation';


-- Water Supply Units
INSERT INTO department_unit (id, department_id, name)
SELECT uuid_generate_v4(), id, 'Pipeline Maintenance Unit'
FROM department WHERE name='Water Supply';


-- Roads Units
INSERT INTO department_unit (id, department_id, name)
SELECT uuid_generate_v4(), id, 'Pothole Repair Crew'
FROM department WHERE name='Roads & Infrastructure';


-- Street Lighting Units
INSERT INTO department_unit (id, department_id, name)
SELECT uuid_generate_v4(), id, 'Electrical Maintenance Unit'
FROM department WHERE name='Street Lighting';


/* ============================================================
   ✅ Seed data complete
   Dataset intentionally small and realistic for dev/demo.
   ============================================================ */

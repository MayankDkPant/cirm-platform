-- ============================================
-- Seed Departments for Governing Body
-- ============================================

WITH gb AS (
    SELECT id FROM governing_body WHERE "Code__c" = 'UK-DDN07-MC'
)
INSERT INTO department (
    id,
    "Code__c",
    name,
    description,
    governing_body_id,
    is_active,
    is_deleted,
    created_at,
    created_by
)
SELECT gen_random_uuid(), d.code, d.name, d.category, gb.id, true, false, now(), 'SYSTEM'
FROM gb,
(
    VALUES
    ('GAS','PNG Gas Services','Gas'),
    ('TEL','Telecom Infrastructure','Telecommunications'),
    ('SWM','Solid Waste Management','Waste Management'),
    ('WTR','Water Supply Department','Water'),
    ('SEW','Sewerage Department','Sewage'),
    ('FER','Fire & Emergency Response','Public Works Emergency Services'),
    ('DMC','Disaster Management Cell','Public Works Emergency Services'),
    ('REN','Solar & Renewable Energy','Renewables'),
    ('CSD','Citizen Helpdesk','Customer Service'),
    ('TRF','Traffic & Parking Management','Traffic'),
    ('HLT','Public Health Department','Health'),
    ('RDS','Roads & Infrastructure','Public Works'),
    ('SWD','Storm Water Drainage','Public Works Emergency Services'),
    ('ELE','Electricity Department','Electricity')
) AS d(code, name, category)
WHERE NOT EXISTS (
    SELECT 1 FROM department dep 
    WHERE dep."Code__c" = d.code 
    AND dep.governing_body_id = gb.id
);


-- ============================================
-- Seed Department Units with State/District/Governing Body
-- ============================================

INSERT INTO department_unit (
    id,
    department_id,
    "Code__c",
    name,
    description,
    state_id,
    district_id,
    governing_body_id,
    is_active,
    is_deleted,
    created_at,
    created_by
)
SELECT 
    gen_random_uuid(),
    d.id,
    u.code,
    u.name,
    u.scope,
    (SELECT id FROM state        WHERE "Code__c" = 'UK'),
    (SELECT id FROM district     WHERE "Code__c" = 'UK-DDN07'),
    (SELECT id FROM governing_body WHERE "Code__c" = 'UK-DDN07-MC'),
    true,
    false,
    now(),
    'SYSTEM'
FROM department d
JOIN (
    VALUES
    ('ELE','ELE-SLDC','State Load Dispatch Center','State'),
    ('ELE','ELE-DMC','Dehradun Street Light Cell','Governing Body'),
    ('ELE','ELE-ZEM','Zone Electrical Maintenance Team','Local'),
    ('WTR','WTR-SCC','State Water Command Center','State'),
    ('WTR','WTR-DWS','Dehradun Water Supply Division','Governing Body'),
    ('WTR','WTR-PMC','Pipeline Maintenance Crew','Local'),
    ('SWM','SWM-CCR','City Waste Control Room','Governing Body'),
    ('SWM','SWM-ZGC','Zone Garbage Collection Team','Local'),
    ('RDS','RDS-CRD','City Roads Division','Governing Body'),
    ('RDS','RDS-PRR','Pothole Rapid Response Team','Local'),
    ('FER','FER-FED','Fire Emergency Dispatch','State'),
    ('HLT','HLT-MCT','Mosquito Control Team','Local'),
    ('TRF','TRF-TSC','Traffic Signal Control Room','Governing Body'),
    ('CSD','CSD-CCC','Citizen Call Center','State')
) AS u(dept_code, code, name, scope)
ON d."Code__c" = u.dept_code
WHERE NOT EXISTS (
    SELECT 1 FROM department_unit du 
    WHERE du."Code__c" = u.code
);
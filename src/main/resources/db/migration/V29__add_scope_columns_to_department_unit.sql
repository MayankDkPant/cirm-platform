-- ============================================
-- Add State/District/GoverningBody scope to department_unit
-- ============================================

ALTER TABLE department_unit
ADD COLUMN state_id uuid,
ADD COLUMN district_id uuid,
ADD COLUMN governing_body_id uuid;

-- Add foreign keys (important for integrity)

ALTER TABLE department_unit
ADD CONSTRAINT fk_du_state
FOREIGN KEY (state_id) REFERENCES state(id);

ALTER TABLE department_unit
ADD CONSTRAINT fk_du_district
FOREIGN KEY (district_id) REFERENCES district(id);

ALTER TABLE department_unit
ADD CONSTRAINT fk_du_governing_body
FOREIGN KEY (governing_body_id) REFERENCES governing_body(id);

-- Add indexes (enterprise readiness)

CREATE INDEX idx_du_state ON department_unit(state_id);
CREATE INDEX idx_du_district ON department_unit(district_id);
CREATE INDEX idx_du_governing_body ON department_unit(governing_body_id);
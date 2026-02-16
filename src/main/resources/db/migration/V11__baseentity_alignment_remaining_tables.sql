-- =====================================================
-- FINAL BASEENTITY ALIGNMENT (SAFE VERSION)
-- =====================================================
-- Makes BaseEntity columns exist on remaining tables.
-- Uses IF NOT EXISTS so migration is re-runnable.
-- =====================================================


-- -----------------------------------------------------
-- COMPLAINT_EVENT
-- -----------------------------------------------------
ALTER TABLE complaint_event
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE complaint_event
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;


-- -----------------------------------------------------
-- COMPLAINTS
-- -----------------------------------------------------
ALTER TABLE complaints
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;


-- -----------------------------------------------------
-- USERS
-- -----------------------------------------------------
ALTER TABLE users
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE users
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;


-- -----------------------------------------------------
-- REFRESH TOKENS
-- -----------------------------------------------------
ALTER TABLE refresh_tokens
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

ALTER TABLE refresh_tokens
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

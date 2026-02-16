-- =====================================================
-- ALIGN AI_MESSAGE WITH BASEENTITY
-- =====================================================
-- All entities inherit from BaseEntity which contains:
--   id, created_at, updated_at, is_active
--
-- ai_message was created before BaseEntity standardization,
-- so we must align the table with the entity model.
-- =====================================================

ALTER TABLE ai_message
ADD COLUMN updated_at TIMESTAMP;

ALTER TABLE ai_message
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

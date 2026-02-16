-- =====================================================
-- ADD ACTIVE FLAG TO AI CONVERSATION
-- =====================================================
-- Hibernate validation detected mismatch between entity
-- and database schema.
--
-- This column allows soft lifecycle control of conversations
-- (active vs archived).
-- =====================================================

ALTER TABLE ai_conversation
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

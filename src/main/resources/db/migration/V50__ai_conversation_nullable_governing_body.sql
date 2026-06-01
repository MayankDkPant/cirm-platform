-- V50: Allow ai_conversation.governing_body_id to be null.
--
-- Rationale: citizen AI conversations carry no operator tenant context —
-- the user's Supabase JWT has no governing_body_id claim. Storing null
-- for citizen sessions is semantically correct; operator sessions continue
-- to populate the column and remain covered by the existing FK.
--
-- The FK constraint is preserved: when the column IS populated, referential
-- integrity to governing_body(id) is still enforced by the DB.

ALTER TABLE ai_conversation
    ALTER COLUMN governing_body_id DROP NOT NULL;

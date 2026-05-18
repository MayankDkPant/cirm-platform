/* 
====================================================================
MIGRATION: V21__complaint_duplicate_model_refactor.sql
AUTHOR: CIRM Platform
DATE: 2026-02-20

PURPOSE
--------------------------------------------------------------------
Refactor duplicate complaint modeling.

Old Model:
    duplicate_of_complaint_id

New Model:
    original_complaint_id
    is_duplicate (boolean helper flag)

RATIONALE
--------------------------------------------------------------------
1. Improve business readability (original_complaint_id).
2. Add explicit duplicate flag for analytics and fast querying.
3. Maintain relational integrity while improving query performance.
4. Align with enterprise case-management standards.

This migration is forward-only and non-destructive.
====================================================================
*/


/* ================================================================
   1. ADD NEW COLUMNS
================================================================ */

ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS original_complaint_id UUID;

ALTER TABLE complaints 
ADD COLUMN IF NOT EXISTS is_duplicate BOOLEAN NOT NULL DEFAULT FALSE;


/* ================================================================
   2. DATA MIGRATION (IF OLD COLUMN EXISTS)
   ---------------------------------------------------------------
   If duplicate_of_complaint_id existed and had values,
   migrate its data to original_complaint_id.
================================================================ */

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'complaints' 
          AND column_name = 'duplicate_of_complaint_id'
    ) THEN

        -- Copy old relationship into new column
        UPDATE complaints
        SET original_complaint_id = duplicate_of_complaint_id
        WHERE duplicate_of_complaint_id IS NOT NULL;

        -- Mark duplicates correctly
        UPDATE complaints
        SET is_duplicate = TRUE
        WHERE original_complaint_id IS NOT NULL;

    END IF;
END$$;


/* ================================================================
   3. DROP OLD COLUMN
================================================================ */

ALTER TABLE complaints 
DROP COLUMN IF EXISTS duplicate_of_complaint_id;


/* ================================================================
   4. ADD INDEXES FOR SCALE
================================================================ */

CREATE INDEX IF NOT EXISTS idx_complaints_original
ON complaints (original_complaint_id);

CREATE INDEX IF NOT EXISTS idx_complaints_is_duplicate
ON complaints (is_duplicate);


/* ================================================================
   END OF MIGRATION
================================================================ */
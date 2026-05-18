-- Cleanup migration:
-- Removes incorrectly created camelCase column from early development.

ALTER TABLE complaints
DROP COLUMN IF EXISTS reportedlocationtext;
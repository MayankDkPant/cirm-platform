-- Add public identifier column for hybrid refresh token design
ALTER TABLE refresh_tokens
ADD COLUMN token_id UUID;

-- Backfill existing rows so migration works on non-empty DB
UPDATE refresh_tokens
SET token_id = gen_random_uuid()
WHERE token_id IS NULL;

-- Make column mandatory
ALTER TABLE refresh_tokens
ALTER COLUMN token_id SET NOT NULL;

-- Enforce uniqueness for fast lookup
CREATE UNIQUE INDEX idx_refresh_tokens_token_id
ON refresh_tokens(token_id);
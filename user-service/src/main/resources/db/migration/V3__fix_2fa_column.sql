-- 1. Drop the incorrectly named and typed column
ALTER TABLE users DROP COLUMN isusing2fa;

-- 2. Add the correct column with the correct name and type
ALTER TABLE users ADD COLUMN is_using2fa BOOLEAN DEFAULT FALSE;
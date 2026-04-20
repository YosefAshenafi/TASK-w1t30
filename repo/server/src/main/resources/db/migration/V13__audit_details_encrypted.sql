-- Convert audit_events.details from jsonb to text to support AES-256-GCM ciphertext
-- Existing rows are migrated by casting jsonb to text (plaintext for now)
ALTER TABLE audit_events ALTER COLUMN details TYPE TEXT USING details::text;
ALTER TABLE audit_events ALTER COLUMN details SET DEFAULT '{}';

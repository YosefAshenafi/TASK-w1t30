-- Adds an AES-256-GCM encrypted email column to users.
-- Values are stored as base64(iv[12] || gcmCiphertext) via AesAttributeConverter.
ALTER TABLE users ADD COLUMN email TEXT;

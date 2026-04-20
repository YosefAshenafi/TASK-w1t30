-- Allow administrators to override the on-disk backup path via policy.
ALTER TABLE backup_policy
    ADD COLUMN IF NOT EXISTS backup_path VARCHAR(500);

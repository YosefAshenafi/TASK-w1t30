-- Persist backup policy to database instead of in-memory map
CREATE TABLE IF NOT EXISTS backup_policy (
    id              SERIAL PRIMARY KEY,
    retention_days  INTEGER NOT NULL DEFAULT 30,
    schedule_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    schedule_cron   VARCHAR(100) NOT NULL DEFAULT '0 0 2 * * *',
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed default policy
INSERT INTO backup_policy (retention_days, schedule_enabled, schedule_cron)
VALUES (30, TRUE, '0 0 2 * * *')
ON CONFLICT DO NOTHING;

CREATE TABLE backup_runs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type          VARCHAR(20) NOT NULL CHECK (type IN ('FULL','INCREMENTAL','SCHEMA')),
    status        VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
                      CHECK (status IN ('RUNNING','COMPLETED','FAILED')),
    file_path     TEXT,
    size_bytes    BIGINT,
    checksum_sha256 TEXT,
    error_message TEXT,
    initiated_by  UUID REFERENCES users(id),
    started_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at  TIMESTAMPTZ
);
CREATE INDEX idx_backup_status_time ON backup_runs(status, started_at);

CREATE TABLE recovery_drills (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backup_run_id   UUID NOT NULL REFERENCES backup_runs(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','RUNNING','PASSED','FAILED')),
    notes           TEXT,
    conducted_by    UUID REFERENCES users(id),
    scheduled_at    TIMESTAMPTZ NOT NULL,
    completed_at    TIMESTAMPTZ
);
CREATE INDEX idx_drills_scheduled ON recovery_drills(scheduled_at);

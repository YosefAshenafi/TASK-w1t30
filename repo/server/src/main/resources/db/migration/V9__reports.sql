CREATE TABLE report_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(40) NOT NULL,
    parameters      JSONB NOT NULL DEFAULT '{}',
    status          VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
                        CHECK (status IN ('QUEUED','RUNNING','COMPLETED','FAILED')),
    requested_by    UUID NOT NULL REFERENCES users(id),
    organization_id UUID REFERENCES organizations(id),
    file_path       TEXT,
    error_message   TEXT,
    queued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ
);
CREATE INDEX idx_reports_status_queued ON report_runs(status, queued_at);
CREATE INDEX idx_reports_requester     ON report_runs(requested_by, queued_at);
CREATE INDEX idx_reports_org           ON report_runs(organization_id, queued_at);

CREATE TABLE report_schedules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(40) NOT NULL,
    parameters      JSONB NOT NULL DEFAULT '{}',
    cron_expr       VARCHAR(100) NOT NULL,
    owner_id        UUID NOT NULL REFERENCES users(id),
    organization_id UUID REFERENCES organizations(id),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at     TIMESTAMPTZ,
    next_run_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_sched_next_run ON report_schedules(next_run_at) WHERE enabled = TRUE;

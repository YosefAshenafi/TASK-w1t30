-- Add missing columns to report_runs
ALTER TABLE report_runs ADD COLUMN IF NOT EXISTS row_count INTEGER;
ALTER TABLE report_runs ADD COLUMN IF NOT EXISTS approval_request_id UUID REFERENCES approval_requests(id);
ALTER TABLE report_runs ADD COLUMN IF NOT EXISTS classification VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';
ALTER TABLE report_runs DROP CONSTRAINT IF EXISTS report_runs_status_check;
ALTER TABLE report_runs ADD CONSTRAINT report_runs_status_check
    CHECK (status IN ('QUEUED','RUNNING','NEEDS_APPROVAL','SUCCEEDED','FAILED','CANCELLED'));

-- Add template management columns to notification_templates
ALTER TABLE notification_templates ADD COLUMN IF NOT EXISTS variables JSONB NOT NULL DEFAULT '[]';
ALTER TABLE notification_templates ADD COLUMN IF NOT EXISTS updated_by UUID REFERENCES users(id);
ALTER TABLE notification_templates ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Add severity to in_app_notifications
ALTER TABLE in_app_notifications ADD COLUMN IF NOT EXISTS severity VARCHAR(10) NOT NULL DEFAULT 'INFO'
    CHECK (severity IN ('INFO','WARN','CRITICAL'));

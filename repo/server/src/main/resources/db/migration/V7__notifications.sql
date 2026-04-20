CREATE TABLE notification_templates (
    key         TEXT PRIMARY KEY,
    title_tmpl  TEXT NOT NULL,
    body_tmpl   TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE in_app_notifications (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    template_key TEXT NOT NULL REFERENCES notification_templates(key),
    payload      JSONB NOT NULL DEFAULT '{}',
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notif_user_read ON in_app_notifications(user_id, read_at);

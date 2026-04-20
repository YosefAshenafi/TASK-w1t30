CREATE TABLE audit_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id     UUID REFERENCES users(id),
    action       VARCHAR(60) NOT NULL,
    target_type  VARCHAR(40),
    target_id    TEXT,
    ip_address   INET,
    request_id   TEXT,
    details      JSONB NOT NULL DEFAULT '{}',
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_audit_actor_time  ON audit_events(actor_id, occurred_at);
CREATE INDEX idx_audit_action_time ON audit_events(action, occurred_at);
CREATE INDEX idx_audit_occurred    ON audit_events(occurred_at);

CREATE TABLE anomaly_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id),
    type         VARCHAR(30) NOT NULL CHECK (type IN ('NEW_DEVICE','IP_OUT_OF_RANGE','EXPORT_BURST','RAPID_FIRE')),
    ip_address   INET,
    details      JSONB NOT NULL DEFAULT '{}',
    resolved_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_anomaly_user_type ON anomaly_events(user_id, type, created_at);

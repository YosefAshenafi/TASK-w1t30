CREATE TABLE idempotency_keys (
    key           TEXT PRIMARY KEY,
    user_id       UUID REFERENCES users(id),
    request_hash  TEXT NOT NULL,
    response_json JSONB NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_idem_created ON idempotency_keys(created_at);

CREATE TABLE approval_requests (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type          VARCHAR(30) NOT NULL CHECK (type IN ('PERMISSION_CHANGE','EXPORT')),
    payload       JSONB NOT NULL DEFAULT '{}',
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING','APPROVED','REJECTED','EXPIRED')),
    requested_by  UUID NOT NULL REFERENCES users(id),
    reviewed_by   UUID REFERENCES users(id),
    reason        TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMPTZ NOT NULL,
    decided_at    TIMESTAMPTZ
);
CREATE INDEX idx_appr_status_expires ON approval_requests(status, expires_at);

CREATE TABLE organizations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         TEXT NOT NULL,
    code         TEXT NOT NULL UNIQUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_org_code ON organizations(code);

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username            VARCHAR(64) NOT NULL UNIQUE,
    display_name        TEXT NOT NULL,
    password_bcrypt     TEXT NOT NULL,
    role                VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT','CORPORATE_MENTOR','FACULTY_MENTOR','ADMIN')),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','ACTIVE','LOCKED','SUSPENDED','DELETED')),
    organization_id     UUID REFERENCES organizations(id),
    failed_login_count  INTEGER NOT NULL DEFAULT 0,
    locked_until        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ,
    deleted_by          UUID
);
CREATE INDEX idx_users_org     ON users(organization_id);
CREATE INDEX idx_users_status  ON users(status);
CREATE INDEX idx_users_deleted ON users(deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE user_device_fingerprints (
    user_id          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fingerprint_hash TEXT NOT NULL,
    first_seen_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    trusted          BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (user_id, fingerprint_hash)
);

CREATE TABLE allowed_ip_ranges (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cidr       CIDR NOT NULL,
    role_scope VARCHAR(20) CHECK (role_scope IN ('STUDENT','CORPORATE_MENTOR','FACULTY_MENTOR','ADMIN')),
    note       TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ipranges_role ON allowed_ip_ranges(role_scope);

CREATE TABLE refresh_tokens (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash           TEXT NOT NULL UNIQUE,
    family_id            UUID NOT NULL,
    idle_expires_at      TIMESTAMPTZ NOT NULL,
    absolute_expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_refresh_user_family ON refresh_tokens(user_id, family_id);

CREATE TABLE enrollments (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id       UUID NOT NULL REFERENCES users(id),
    cohort_id        UUID NOT NULL REFERENCES cohorts(id),
    organization_id  UUID REFERENCES organizations(id),
    enrolled_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    refunded_at      TIMESTAMPTZ,
    deleted_at       TIMESTAMPTZ,
    deleted_by       UUID
);
CREATE INDEX idx_enroll_org    ON enrollments(organization_id);
CREATE INDEX idx_enroll_cohort ON enrollments(cohort_id);

CREATE TABLE operational_transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type             VARCHAR(20) NOT NULL CHECK (type IN ('PURCHASE','REFUND','INVENTORY_ADJUST')),
    amount           NUMERIC(12,2),
    organization_id  UUID REFERENCES organizations(id),
    occurred_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata         JSONB NOT NULL DEFAULT '{}'
);
CREATE INDEX idx_optx_org_time_type ON operational_transactions(organization_id, occurred_at, type);

CREATE TABLE certifications (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID NOT NULL REFERENCES users(id),
    course_id   UUID NOT NULL REFERENCES courses(id),
    issued_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_certs_expires ON certifications(expires_at);

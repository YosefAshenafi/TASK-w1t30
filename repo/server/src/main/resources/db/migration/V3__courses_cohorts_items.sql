CREATE TABLE locations (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT NOT NULL,
    address    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE instructors (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT NOT NULL,
    email      TEXT,
    phone      TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE courses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            TEXT NOT NULL,
    title           TEXT NOT NULL,
    version         TEXT NOT NULL,
    location_id     UUID REFERENCES locations(id),
    instructor_id   UUID REFERENCES instructors(id),
    classification  VARCHAR(20) NOT NULL DEFAULT 'INTERNAL'
                        CHECK (classification IN ('PUBLIC','INTERNAL','CONFIDENTIAL','RESTRICTED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ,
    deleted_by      UUID
);
CREATE INDEX idx_courses_version  ON courses(version);
CREATE INDEX idx_courses_loc_instr ON courses(location_id, instructor_id);
CREATE INDEX idx_courses_deleted  ON courses(deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TABLE cohorts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    total_seats INTEGER NOT NULL,
    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_cohorts_course_dates ON cohorts(course_id, starts_at, ends_at);

CREATE TABLE knowledge_points (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE activities (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    name        TEXT NOT NULL,
    description TEXT,
    sort_order  INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE assessment_items (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id          UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    knowledge_point_id UUID REFERENCES knowledge_points(id),
    type               VARCHAR(10) NOT NULL CHECK (type IN ('SINGLE','MULTI','SHORT','CODE')),
    stem               TEXT NOT NULL,
    choices            JSONB,
    difficulty         NUMERIC(4,3) NOT NULL DEFAULT 0.500,
    discrimination     NUMERIC(4,3) NOT NULL DEFAULT 0.000,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at         TIMESTAMPTZ,
    deleted_by         UUID
);
CREATE INDEX idx_items_course_kp ON assessment_items(course_id, knowledge_point_id);

CREATE TABLE assessment_attempts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID NOT NULL REFERENCES users(id),
    item_id        UUID NOT NULL REFERENCES assessment_items(id),
    chosen_answer  JSONB,
    is_correct     BOOLEAN,
    attempted_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_attempts_student_item ON assessment_attempts(student_id, item_id);
CREATE INDEX idx_attempts_time         ON assessment_attempts(attempted_at);

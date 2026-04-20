CREATE TABLE training_sessions (
    id                    UUID PRIMARY KEY,  -- client-issued UUID v7
    student_id            UUID NOT NULL REFERENCES users(id),
    course_id             UUID NOT NULL REFERENCES courses(id),
    cohort_id             UUID REFERENCES cohorts(id),
    rest_seconds_default  INTEGER NOT NULL DEFAULT 60 CHECK (rest_seconds_default BETWEEN 15 AND 300),
    status                VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS'
                              CHECK (status IN ('IN_PROGRESS','PAUSED','COMPLETED','ABANDONED')),
    started_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at              TIMESTAMPTZ,
    client_updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at            TIMESTAMPTZ,
    deleted_by            UUID
);
CREATE INDEX idx_sessions_student_time ON training_sessions(student_id, started_at);

CREATE TABLE session_activity_sets (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id        UUID NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE,
    activity_id       UUID NOT NULL REFERENCES activities(id),
    set_index         INTEGER NOT NULL,
    rest_seconds      INTEGER NOT NULL DEFAULT 60 CHECK (rest_seconds BETWEEN 15 AND 300),
    completed_at      TIMESTAMPTZ,
    notes             TEXT,
    client_updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (session_id, activity_id, set_index)
);
CREATE INDEX idx_sets_session_activity ON session_activity_sets(session_id, activity_id);

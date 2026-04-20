-- Offline assessment draft storage
-- Captures in-progress answers that are synced from the client's IndexedDB.
CREATE TABLE IF NOT EXISTS attempt_drafts (
    id                  VARCHAR(128) PRIMARY KEY,
    session_id          UUID NOT NULL REFERENCES training_sessions(id) ON DELETE CASCADE,
    item_id             UUID NOT NULL REFERENCES assessment_items(id),
    student_id          UUID NOT NULL REFERENCES users(id),
    chosen_answer       TEXT,
    client_updated_at   TIMESTAMPTZ NOT NULL,
    server_updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_attempt_drafts_session  ON attempt_drafts(session_id);
CREATE INDEX IF NOT EXISTS idx_attempt_drafts_student  ON attempt_drafts(student_id);

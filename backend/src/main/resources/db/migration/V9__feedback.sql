-- In-app feedback (Plan 10 step 06). One row per message. user_id is
-- null for guests; signed-in rows are deleted with the account
-- (AccountDataDeleter). Not synced: write-only from the client.
CREATE TABLE feedback (
    id           UUID        PRIMARY KEY,
    user_id      TEXT,
    message      TEXT        NOT NULL CHECK (char_length(message) BETWEEN 1 AND 2000),
    app_version  TEXT,
    os_version   TEXT,
    device_model TEXT,
    created_at   TIMESTAMPTZ NOT NULL,
    received_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX feedback_user_id_idx ON feedback (user_id) WHERE user_id IS NOT NULL;
CREATE INDEX feedback_received_at_idx ON feedback (received_at DESC);

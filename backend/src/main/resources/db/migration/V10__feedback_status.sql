-- Feedback admin page (Plan 10 step 06d). The owner marks each message
-- open / solved / wont_fix / spam with an optional note. updated_at is
-- the last status or note change (received_at until the first one).
ALTER TABLE feedback
    ADD COLUMN status     TEXT        NOT NULL DEFAULT 'open'
        CHECK (status IN ('open', 'solved', 'wont_fix', 'spam')),
    ADD COLUMN note       TEXT        CHECK (char_length(note) <= 500),
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE feedback SET updated_at = received_at;

ALTER TABLE feedback
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN updated_at SET DEFAULT now();

-- Admin list: filter by status, newest first.
CREATE INDEX feedback_status_received_at_idx ON feedback (status, received_at DESC, id DESC);

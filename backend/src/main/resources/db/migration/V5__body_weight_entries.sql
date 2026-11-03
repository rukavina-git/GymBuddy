-- Plan 6 step 04: body weight history (api/openapi.yaml BodyWeightEntry).
-- Flat, user-owned, soft-deleted like workout_sessions: same sync
-- metadata, same owner index. Several entries per day are allowed.

CREATE TABLE body_weight_entries (
    id           UUID    NOT NULL PRIMARY KEY,
    owner_id     TEXT    NOT NULL,
    measured_at  BIGINT  NOT NULL,
    weight_kg    REAL    NOT NULL,
    updated_at   BIGINT  NOT NULL,
    deleted_at   BIGINT,
    revision     INT     NOT NULL DEFAULT 0,
    CONSTRAINT body_weight_entries_weight_ck CHECK (weight_kg > 0 AND weight_kg <= 700)
);

CREATE INDEX idx_body_weight_entries_owner_updated ON body_weight_entries (owner_id, updated_at);

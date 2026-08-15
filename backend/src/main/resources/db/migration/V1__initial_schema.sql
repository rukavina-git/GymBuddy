-- Initial baseline schema. Source of truth is the Room entities in
-- android/app/src/main/java/com/rukavina/gymbuddy/data/local/entity/.
--
-- Server-side deviations from the client schema, throughout this file:
--   - owner_id (Firebase uid) is mandatory on every user-owned row. On
--     the client it's implicit (the local database is wiped on logout);
--     on the server it is the primary isolation mechanism.
--   - sync_state does not exist here. It is client-local outbox
--     bookkeeping, deliberately absent from both the wire format and
--     the server.
--   - Every user-owned table gets updated_at/revision, server-assigned
--     on write - there is no column default for either, so a code path
--     that forgets to stamp them fails loudly (NOT NULL violation)
--     instead of silently persisting a wrong value.
--
-- Foreign keys are real, CASCADE-deleting, ONLY within an aggregate
-- (session -> performed exercise -> set, template -> template exercise).
-- Every reference that crosses aggregates - exercise_id, template_id,
-- derived_from_id - is a soft reference: a plain column with no FK
-- constraint, because it must tolerate the row it points to being
-- deprecated, deleted, or (for exercise_id) never having existed on this
-- server at all. A real FK would either destroy history on delete
-- (CASCADE) or make deletion impossible (RESTRICT); neither is
-- acceptable for a pointer that exists for provenance, not integrity.


-- ============================================================
-- exercises
-- ============================================================

CREATE TABLE exercises (
    id                  UUID    NOT NULL PRIMARY KEY,
    name                TEXT    NOT NULL,
    primary_muscles     JSONB   NOT NULL,
    secondary_muscles   JSONB   NOT NULL,
    description         TEXT,
    instructions        JSONB   NOT NULL,
    tips                JSONB   NOT NULL,
    difficulty          TEXT    NOT NULL,
    equipment_needed    JSONB   NOT NULL,
    category            TEXT    NOT NULL,
    exercise_type       TEXT    NOT NULL,
    tracking_type       TEXT    NOT NULL,
    video_url           TEXT,
    thumbnail_url       TEXT,
    source              TEXT    NOT NULL,
    owner_id            TEXT,
    -- Soft reference to the DEFAULT exercise this row was duplicated
    -- from. Not a foreign key - see file header.
    derived_from_id     UUID,
    deprecated          BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at          BIGINT  NOT NULL,
    deleted_at          BIGINT,
    revision            INT     NOT NULL DEFAULT 0,

    CONSTRAINT exercises_source_owner_ck CHECK (
        (source = 'DEFAULT' AND owner_id IS NULL) OR
        (source = 'CUSTOM'  AND owner_id IS NOT NULL)
    )
);

COMMENT ON COLUMN exercises.derived_from_id IS
    'Soft reference to exercises.id, not a foreign key: provenance only.';

CREATE INDEX idx_exercises_owner_updated ON exercises (owner_id, updated_at);
CREATE INDEX idx_exercises_source ON exercises (source);


-- ============================================================
-- workout_templates
-- ============================================================

CREATE TABLE workout_templates (
    id                  UUID    NOT NULL PRIMARY KEY,
    title               TEXT    NOT NULL,
    source              TEXT    NOT NULL DEFAULT 'CUSTOM',
    owner_id            TEXT,
    -- Soft reference to the DEFAULT template this row was duplicated
    -- from. Not a foreign key - see file header.
    derived_from_id     UUID,
    deprecated          BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at          BIGINT  NOT NULL,
    deleted_at          BIGINT,
    revision            INT     NOT NULL DEFAULT 0,

    CONSTRAINT workout_templates_source_owner_ck CHECK (
        (source = 'DEFAULT' AND owner_id IS NULL) OR
        (source = 'CUSTOM'  AND owner_id IS NOT NULL)
    )
);

COMMENT ON COLUMN workout_templates.derived_from_id IS
    'Soft reference to workout_templates.id, not a foreign key: provenance only.';

CREATE INDEX idx_workout_templates_owner_updated ON workout_templates (owner_id, updated_at);
CREATE INDEX idx_workout_templates_source ON workout_templates (source);


-- ============================================================
-- template_exercises - child of workout_templates aggregate.
-- No sync metadata: the parent's updated_at/deleted_at/revision governs
-- the whole tree.
-- ============================================================

CREATE TABLE template_exercises (
    id                          UUID    NOT NULL PRIMARY KEY,
    template_id                 UUID    NOT NULL REFERENCES workout_templates (id) ON DELETE CASCADE,
    -- Soft reference to exercises.id, not a foreign key - see file header.
    exercise_id                 UUID    NOT NULL,
    exercise_name               TEXT    NOT NULL,
    exercise_tracking_type      TEXT    NOT NULL,
    planned_sets                INT     NOT NULL,
    planned_reps                INT,
    order_index                 INT     NOT NULL,
    rest_seconds                INT,
    planned_duration_seconds    INT,
    planned_distance_meters     REAL,
    planned_weight_kg           REAL,
    notes                       TEXT
);

COMMENT ON COLUMN template_exercises.exercise_id IS
    'Soft reference to exercises.id, not a foreign key: must tolerate the exercise being removed. See exercise_name/exercise_tracking_type snapshot columns for display.';

CREATE INDEX idx_template_exercises_template_id ON template_exercises (template_id);


-- ============================================================
-- workout_sessions
-- ============================================================

CREATE TABLE workout_sessions (
    id                  UUID    NOT NULL PRIMARY KEY,
    owner_id            TEXT    NOT NULL,
    started_at          BIGINT  NOT NULL,
    ended_at            BIGINT,
    duration_seconds    INT     NOT NULL,
    title               TEXT    NOT NULL,
    notes               TEXT,
    -- Soft reference to workout_templates.id, not a foreign key - see
    -- file header. template_title is a snapshot, written once.
    template_id         UUID,
    template_title      TEXT,
    updated_at          BIGINT  NOT NULL,
    deleted_at          BIGINT,
    revision            INT     NOT NULL DEFAULT 0
);

COMMENT ON COLUMN workout_sessions.template_id IS
    'Soft reference to workout_templates.id, not a foreign key: must tolerate the template being deleted. See template_title for display.';

CREATE INDEX idx_workout_sessions_owner_updated ON workout_sessions (owner_id, updated_at);


-- ============================================================
-- performed_exercises - child of workout_sessions aggregate.
-- No sync metadata: a tombstoned session implicitly tombstones
-- everything under it.
-- ============================================================

CREATE TABLE performed_exercises (
    id                          UUID    NOT NULL PRIMARY KEY,
    workout_session_id          UUID    NOT NULL REFERENCES workout_sessions (id) ON DELETE CASCADE,
    -- Soft reference to exercises.id, not a foreign key - see file header.
    exercise_id                 UUID    NOT NULL,
    order_index                 INT     NOT NULL,
    -- Snapshot columns, written once at creation and never updated -
    -- history must remain readable even if exercise_id no longer
    -- resolves to a row.
    exercise_name                TEXT    NOT NULL,
    exercise_category             TEXT    NOT NULL,
    exercise_tracking_type        TEXT    NOT NULL,
    exercise_primary_muscles      JSONB   NOT NULL,
    superset_group               INT
);

COMMENT ON COLUMN performed_exercises.exercise_id IS
    'Soft reference to exercises.id, not a foreign key: must remain resolvable in history even if the exercise is later removed. See the exercise_name/exercise_category/exercise_tracking_type/exercise_primary_muscles snapshot columns for display, never a live join.';

CREATE INDEX idx_performed_exercises_session_id ON performed_exercises (workout_session_id);


-- ============================================================
-- workout_sets - child of performed_exercises, i.e. grandchild of the
-- workout_sessions aggregate. No sync metadata.
-- ============================================================

CREATE TABLE workout_sets (
    id                      UUID    NOT NULL PRIMARY KEY,
    performed_exercise_id   UUID    NOT NULL REFERENCES performed_exercises (id) ON DELETE CASCADE,
    weight_kg               REAL,
    reps                    INT,
    duration_seconds        INT,
    distance_meters         REAL,
    set_type                TEXT    NOT NULL DEFAULT 'WORKING',
    is_completed            BOOLEAN NOT NULL DEFAULT FALSE,
    rest_taken_seconds      INT,
    order_index             INT     NOT NULL
);

CREATE INDEX idx_workout_sets_performed_exercise_id ON workout_sets (performed_exercise_id);


-- ============================================================
-- user_profile
--
-- uid IS the owner id here (it's the Firebase uid, and the primary
-- key), so there is no separate owner_id column. A single-row-per-owner
-- table has no use for an (owner_id, updated_at) index either - the PK
-- lookup already narrows to at most one row.
-- ============================================================

CREATE TABLE user_profile (
    uid                 TEXT    NOT NULL PRIMARY KEY,
    name                TEXT    NOT NULL,
    email               TEXT    NOT NULL,
    profile_image_url   TEXT,
    birth_date          BIGINT,
    weight              REAL,
    height              REAL,
    gender              TEXT,
    fitness_goal        TEXT,
    activity_level      TEXT,
    target_weight       REAL,
    joined_date         BIGINT  NOT NULL,
    bio                 TEXT,
    updated_at          BIGINT  NOT NULL,
    deleted_at          BIGINT,
    revision            INT     NOT NULL DEFAULT 0
);


-- ============================================================
-- user_exercise_state - sparse per-user overlay on exercises.
-- Upsert-only: no deleted_at. "Cleared" means all flags false and note
-- null, per UserExerciseStateEntity's design - there is no delete
-- operation to tombstone.
-- ============================================================

CREATE TABLE user_exercise_state (
    owner_id                TEXT    NOT NULL,
    -- Soft reference to exercises.id, not a foreign key - see file
    -- header. An orphaned row is harmless: the preference just resumes
    -- if an exercise with that id reappears.
    exercise_id             UUID    NOT NULL,
    is_hidden               BOOLEAN NOT NULL DEFAULT FALSE,
    is_favorite             BOOLEAN NOT NULL DEFAULT FALSE,
    note                    TEXT,
    default_rest_seconds    INT,
    updated_at              BIGINT  NOT NULL,
    revision                INT     NOT NULL DEFAULT 0,

    PRIMARY KEY (owner_id, exercise_id)
);

CREATE INDEX idx_user_exercise_state_owner_updated ON user_exercise_state (owner_id, updated_at);


-- ============================================================
-- user_template_state - sparse per-user overlay on workout_templates.
-- Same upsert-only, no-deleted_at design as user_exercise_state.
-- ============================================================

CREATE TABLE user_template_state (
    owner_id        TEXT    NOT NULL,
    -- Soft reference to workout_templates.id, not a foreign key - see
    -- file header.
    template_id     UUID    NOT NULL,
    is_hidden       BOOLEAN NOT NULL DEFAULT FALSE,
    is_favorite     BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at      BIGINT  NOT NULL,
    revision        INT     NOT NULL DEFAULT 0,

    PRIMARY KEY (owner_id, template_id)
);

CREATE INDEX idx_user_template_state_owner_updated ON user_template_state (owner_id, updated_at);


-- ============================================================
-- change_log - drives pull. One row per applied mutation; the pull
-- cursor is an opaque position in (user_id, seq) order.
-- ============================================================

CREATE TABLE change_log (
    seq          BIGSERIAL PRIMARY KEY,
    user_id      TEXT        NOT NULL,
    entity_type  TEXT        NOT NULL,
    entity_id    UUID        NOT NULL,
    operation    TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_change_log_user_seq ON change_log (user_id, seq);


-- ============================================================
-- reference_version - single row holding the current default-library
-- versions, mirroring the client's ExerciseVersionEntity /
-- TemplateVersionEntity "always id = 1" pattern in one table instead of
-- two. No row is seeded here - that lands with the seed data in Group D.
-- ============================================================

CREATE TABLE reference_version (
    id                          INT     NOT NULL PRIMARY KEY DEFAULT 1,
    exercise_library_version    INT     NOT NULL DEFAULT 0,
    template_library_version    INT     NOT NULL DEFAULT 0,
    updated_at                  BIGINT  NOT NULL DEFAULT 0,

    CONSTRAINT reference_version_single_row_ck CHECK (id = 1)
);

-- change_log.entity_id was declared UUID in V1, matching every entity
-- type except USER_PROFILE: a profile's id is the Firebase uid, a
-- ~28-character provider-issued string, not a UUID. Group F (sync
-- push) must write exactly one change_log row per applied mutation,
-- including profile updates - inserting a uid into a UUID column would
-- fail outright with "invalid input syntax for type uuid".
--
-- Widening to TEXT is lossless for the five entity types that do use
-- real UUIDs: their string representation round-trips exactly, and
-- every existing/future join against a UUID primary key casts this
-- column explicitly (entity_id::uuid) rather than relying on the
-- column's declared type.

ALTER TABLE change_log ALTER COLUMN entity_id TYPE TEXT;

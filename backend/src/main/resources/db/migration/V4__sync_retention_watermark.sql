-- Group H: closes the cursor-expiry gap identified in Group G.
--
-- isCursorExpired previously inferred "oldest retained seq" from
-- MIN(seq) over change_log's current contents for a user. That's an
-- unsound proxy once real pruning exists: retention removes specific
-- tombstoned entities' rows, not a clean prefix of a user's seq range,
-- so MIN(seq) over what's left can jump past a cursor that was already
-- fully caught up, forcing an unnecessary full resync for a routine
-- pattern (a user inactive for 90+ days).
--
-- retention_floor_seq instead tracks the highest change_log.seq the
-- retention job has ever deleted for this user, written by that job at
-- prune time (see TombstoneRetentionService), monotonically
-- non-decreasing. isCursorExpired becomes a direct comparison:
-- cursorSeq < retention_floor_seq means the cursor predates a real,
-- confirmed deletion; anything else is safe. No row for a user means
-- retention has never touched their data - treated as never expired.

CREATE TABLE sync_retention_watermark (
    user_id             TEXT   NOT NULL PRIMARY KEY,
    retention_floor_seq BIGINT NOT NULL,
    updated_at          BIGINT NOT NULL
);

# 7. Delta sync is driven by a change log, not timestamps

Date: 2026-08-14
Status: Accepted

## Context

Clients need to ask "what has changed since I last synced". The obvious
implementation is a timestamp cursor: return everything with
`updated_at > cursor`.

That has a failure mode which is rare, silent, and permanent.

Transaction A begins at T1 and commits at T3. Transaction B begins at T2
and commits at T2. A client syncing at T2.5 reads everything up to T2.5
and advances its cursor. Transaction A's change, stamped T1, is now
behind the cursor and will never be delivered. The client is
permanently missing data, with no error and no way to detect it.

## Decision

A `change_log` table with a `BIGSERIAL` primary key. One row per
mutation, written in the same transaction as the mutation itself. The
cursor is a position in that sequence, encoded opaquely:

    base64url(json({"v": 1, "seq": 128374}))

Clients must never parse, construct, or compare cursors.

`BIGSERIAL` alone does not close the gap. Sequence values are allocated
at insert time, not commit time, so two concurrent transactions can
commit out of sequence order and a reader in that window can advance
past a row that has not yet become visible.

The mutation transaction therefore takes a Postgres advisory lock keyed
on the user id:

    SELECT pg_advisory_xact_lock(hashtext(:userId));

This serialises change-log inserts per user, making the sequence
strictly monotonic per user, which is all the cursor requires.
Contention is negligible: all writes for a user originate from that
user's own one or two devices.

## Consequences

- The commit-ordering race is closed for the only scope that matters.
- The cursor's internal representation can change later — to a composite
  cursor, or a different ordering key — without a breaking API change,
  because it is opaque and versioned.
- Deletions propagate, because a tombstone produces a change-log row.
- A client offline longer than the tombstone retention window cannot be
  reconciled from deltas. That case returns `410 CURSOR_EXPIRED` and the
  client performs a full resync.
- The change log grows and must be pruned alongside tombstones.
- Correct pruning requires a per-user watermark recording the oldest
  retained sequence. Without it, a user whose log is pruned entirely
  after a quiet period gets a spurious 410 and an unnecessary full
  resync.
- Testing requires real PostgreSQL. H2 does not reproduce sequence
  semantics, advisory locks or transaction visibility faithfully.

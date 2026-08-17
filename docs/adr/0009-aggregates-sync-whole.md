# 9. Aggregates sync as whole trees

Date: 2026-08-14
Status: Accepted

## Context

A workout session is a tree: session → performed exercises → sets. A
template is session → template exercises. Roughly twenty sets in a
typical session.

The tempting design makes each set independently syncable, with its own
revision and tombstone, so editing one rep count sends one small row.
That requires merging nested structures when two versions of a tree
disagree — reconciling added, removed and reordered children — which is
the genuinely hard part of any sync system.

## Decision

The session is the atomic sync unit. A pushed session arrives with its
full tree and the server replaces the entire tree: delete existing
children, insert the new ones, in one transaction. No child-level merge,
no diffing.

Nested children carry no sync metadata of their own. The parent's
`updatedAt`, `revision` and `deletedAt` govern the whole tree.

## Consequences

- Nested-tree merge is removed from the system entirely. This is the
  single largest simplification in the sync design, and it is what makes
  ADR 8's server-wins policy sufficient.
- Editing one set re-uploads the whole session — roughly 6 KB of JSON.
  At the expected write rate this is immaterial.
- A conflict on a session discards changes to the entire session rather
  than to the clashing field. Acceptable given the conflict surface
  described in ADR 8.
- The client's outbox coalesces repeated edits to one session into a
  single entry rather than accumulating one per set.
- Finer granularity could be added later if a use case demanded it. It
  could not easily be removed.

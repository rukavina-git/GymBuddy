# 6. Sync endpoints rather than per-entity CRUD

Date: 2026-08-14
Status: Accepted

## Context

The obvious API design is REST: `PUT /v1/exercises/{id}`,
`DELETE /v1/sessions/{id}`, and so on. It is familiar, browsable,
individually testable, and every developer recognises it.

But the client is offline-first. The UI reads from the local database
and never awaits a network call, because a write must succeed whether or
not a connection exists. Local writes queue in an outbox and drain
later.

That means no code path calls a write endpoint synchronously. Whether
the device happens to be online at the moment of the write does not
change what the client does.

## Decision

All user-data writes go through `POST /v1/sync/push`, which accepts a
batch of entities and returns a per-entity result. Reads come from
`GET /v1/sync/pull`. There are no per-entity write endpoints.

Reference data, profile bootstrap, health and account deletion remain
ordinary REST endpoints — they are not part of the sync path.

## Consequences

- The online and offline paths are the same path. There is no second
  code path to keep consistent, on either platform.
- Change-log writes, revision bumps and advisory locking are implemented
  once rather than repeated across every write endpoint, where one
  omission would silently break the cursor for that entity type.
- A batch drains in one round trip rather than N.
- The blast radius is wider: a bug in push affects every write in the
  app, where a bug in one REST handler would affect one resource. This
  is mitigated by splitting the implementation into per-entity-type
  services sharing single implementations of the cross-cutting
  components, so a fault is isolated by type without duplicating the
  invariants.
- The API surface is unfamiliar. Someone expecting REST sees two write
  endpoints and no resources. The specification's description has to
  carry the explanation.
- A future web client doing genuinely online-first editing would want
  CRUD endpoints. They could be added additively; the schema already
  supports them.

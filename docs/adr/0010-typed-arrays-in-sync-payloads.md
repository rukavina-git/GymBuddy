# 10. Sync payloads use one typed array per entity type

Date: 2026-08-15
Status: Accepted

## Context

A push carries a mixed batch of entities — some sessions, some
exercises, some overlay rows. Three encodings were considered.

A discriminated union: one `mutations` array, each item tagged with
`entityType`, using OpenAPI's `oneOf` and `discriminator` to select the
payload schema. Precise, and it preserves ordering across types.

An opaque payload: the same shape, but `payload` is an untyped object
validated server-side. Generates cleanly, but the contract stops
describing what a mutation contains, which defeats the point of having
one.

Separate typed arrays: `workoutSessions`, `exercises`, `templates` and
so on as distinct fields.

## Decision

Separate typed arrays, in both the push request and the pull response.
All fields optional; omit the types with nothing pending.

## Consequences

- Every field is fully typed in the contract, and the generated Kotlin
  and Swift are plain data classes.
- `oneOf` with a discriminator is avoided. Generator support for it
  varies, and the Kotlin output is awkward to consume.
- Ordering between types is lost. This costs nothing here: a session
  snapshots its exercise data, so it does not depend on the exercise
  existing server-side, and no other cross-type dependency exists.
- Adding an entity type means adding a field rather than extending a
  union.
- The request shape is a set of pending changes, not a set of
  collections. The specification says so explicitly, because the field
  names invite the opposite reading.

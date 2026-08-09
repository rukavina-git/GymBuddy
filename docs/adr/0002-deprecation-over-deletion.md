# 2. Deprecate reference data, never delete it

Date: 2026-08-09
Status: Accepted

## Context

Default exercises and templates are server-owned reference data,
shared across all users and refreshed by a versioned bulk pull.
Users build their own templates on top of them and accumulate
workout history referencing them.

Removing an entry from the library breaks those references. User
templates are the acute case: unlike history, a template is a plan
the user intends to execute, so a dangling reference is a broken
feature rather than a rendering problem.

## Decision

Reference data entries are never removed. Entries that should no
longer be offered carry `deprecated = true`: hidden from browsing
and search, still resolvable by existing references.

A default UUID, once shipped, is permanent and is never reused for
a different entity.

## Consequences

- The reference library grows monotonically. Acceptable at this
  scale — a few hundred entries.
- Existing user templates keep working when an exercise is retired.
- Every read path that lists exercises must filter on `deprecated`.
  Detail and reference lookups must not.
- Correcting a bad seed entry means deprecating it and adding a
  replacement, not editing in place, once it has shipped.
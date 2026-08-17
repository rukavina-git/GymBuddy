# 8. The server resolves all conflicts; clients never merge

Date: 2026-08-14
Status: Accepted

## Context

A user with two devices can edit the same entity offline on both. When
both reconcile, something has to decide what the result is.

The available strategies range from last-write-wins through field-level
merge to CRDTs. The right choice depends on how often conflicts occur
and how costly a lost edit is.

Two facts constrain the answer here. Every entity belongs to exactly one
user — there are no shared records and no concurrent writers other than
that user's own devices. And any client-side merge logic would be
implemented twice, in Kotlin and in Swift, with two opportunities to
diverge subtly.

## Decision

The server resolves every conflict and always wins. Clients never merge.

Each entity carries a `revision` assigned by the server. A client sends
the revision it last received:

    no stored row                 → insert at revision 1, APPLIED
    stored revision N, client N   → update to N+1, APPLIED
    stored revision N, client < N → CONFLICT
    stored revision N, client > N → CONFLICT

On `CONFLICT` the client discards its local change and takes the
server's version on the next pull. The conflict result carries no entity
data; pull is the only path a full entity travels to a client.

Revision equality alone cannot distinguish a client's own retry after a
lost response from a genuine concurrent edit that happened to land at
the same revision. Content equality is used as the tiebreaker: an
identical payload is an idempotent replay, a differing one is a
conflict.

## Consequences

- An edit can be lost silently, from the user's point of view. This is
  the accepted cost.
- The realistic conflict surface is small: profile fields and overlay
  flags. Workout logging is effectively append-only and device-local.
- Aggregate-level conflict is coarser — a conflict on a session discards
  that session's entire change, not just the clashing field.
- Every discarded conflict is logged with enough context to reconstruct
  what was lost. The conflict rate is the metric showing whether this
  policy causes real harm, and it cannot be measured retroactively.
- If the rate proves non-trivial, the escalation path is field-level
  merge on profile and overlay tables only, leaving aggregates on
  server-wins. That is a contained change, not a redesign.

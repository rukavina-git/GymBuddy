package com.rukavina.gymbuddy.sync

import org.springframework.stereotype.Component

/**
 * Outcome of comparing a pushed entity's client-declared revision
 * against what the server has stored.
 */
sealed class RevisionDecision {
    /** No stored row: insert fresh at revision 1, regardless of what the client sent. */
    data object Insert : RevisionDecision()

    /** Client sent exactly the stored revision: apply, bump to resultingRevision. */
    data class Update(val resultingRevision: Int) : RevisionDecision()

    /**
     * The stored row is already at what this push would have produced
     * (storedRevision == clientRevision + 1) *and* its content matches
     * what's being pushed now. This is a retried push whose earlier
     * response was lost - not a fresh mutation, not a conflict. Return
     * the already-stored state as APPLIED without touching anything.
     */
    data class IdempotentReplay(val storedRevision: Int) : RevisionDecision()

    /** Stale or ahead-of-server client revision (or a same-baseline race with different content): server wins. */
    data class Conflict(val storedRevision: Int) : RevisionDecision()
}

/**
 * The one implementation of the revision-check table every per-type
 * sync service delegates to - see api/openapi.yaml's push description
 * and Group F's design notes for the four base cases this encodes:
 *
 *   no stored row, any client revision  -> insert at revision 1, APPLIED
 *   stored revision N, client sends N   -> update to N+1, APPLIED
 *   stored revision N, client sends < N -> CONFLICT
 *   stored revision N, client sends > N -> CONFLICT
 *
 * plus the idempotency carve-out (point 4): re-pushing an entity
 * already applied at the same resulting revision must return APPLIED,
 * not a spurious CONFLICT, because a dropped connection can lose a
 * successful response and the client has no choice but to retry with
 * the same (now one-behind) revision it last saw.
 *
 * That carve-out only fires on exact content equality. Revision numbers
 * alone can't distinguish "this is my own successful push, retried"
 * from "someone else's concurrent edit landed at the revision I was
 * aiming for" - both present identically as storedRevision ==
 * clientRevision + 1. Content equality resolves the ambiguity safely
 * either way: if the content matches, nothing is lost by reporting
 * APPLIED (the stored row already is this push); if it doesn't, this
 * is a genuine conflict and must not be silently accepted as a
 * duplicate - accepting it would be exactly the kind of silent data
 * loss this endpoint exists to avoid.
 */
@Component
class RevisionChecker {

    fun decide(storedRevision: Int?, clientRevision: Int, storedContent: Any?, incomingContent: Any): RevisionDecision {
        if (storedRevision == null) return RevisionDecision.Insert

        return when {
            clientRevision == storedRevision ->
                RevisionDecision.Update(storedRevision + 1)

            storedRevision == clientRevision + 1 && storedContent == incomingContent ->
                RevisionDecision.IdempotentReplay(storedRevision)

            else ->
                RevisionDecision.Conflict(storedRevision)
        }
    }
}

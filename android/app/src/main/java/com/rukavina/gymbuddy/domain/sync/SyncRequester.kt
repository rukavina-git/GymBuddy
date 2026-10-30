package com.rukavina.gymbuddy.domain.sync

/** Why a sync was asked for. Kept for logging; every reason runs the same push-then-pull cycle. */
enum class SyncReason {
    APP_FOREGROUND,
    SESSION_COMPLETED,
    NETWORK_AVAILABLE,
    MANUAL,
    PERIODIC,
    SIGNED_IN
}

/** How a [SyncRequester.syncNow] call ended. */
enum class SyncOutcome {
    SYNCED,
    /** Nobody is signed in, so no cycle ran. Retrying won't help until someone signs in. */
    SIGNED_OUT,
    /** The cycle ran and failed (offline, server error). Worth retrying. */
    FAILED
}

/**
 * How the rest of the app asks for a sync, without depending on the
 * sync engine itself. Only the triggers in SyncTriggers, session
 * completion and pull-to-refresh call this. Ordinary local writes don't:
 * they only queue an outbox entry.
 */
interface SyncRequester {
    /** Fire and forget. Coalesced with any sync already running. Ignored when signed out. */
    fun requestSync(reason: SyncReason)

    /** Runs a sync and waits for it. Says whether it synced, was skipped because signed out, or failed. */
    suspend fun syncNow(reason: SyncReason): SyncOutcome
}

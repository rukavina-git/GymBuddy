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

/**
 * How the rest of the app asks for a sync, without depending on the
 * sync engine itself. Only the triggers in SyncTriggers, session
 * completion and pull-to-refresh call this. Ordinary local writes don't:
 * they only queue an outbox entry.
 */
interface SyncRequester {
    /** Fire and forget. Coalesced with any sync already running. Ignored when signed out. */
    fun requestSync(reason: SyncReason)

    /** Runs a sync and waits for it. Returns false if it couldn't complete (offline, signed out, server error). */
    suspend fun syncNow(reason: SyncReason): Boolean
}

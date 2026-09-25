package com.rukavina.gymbuddy.data.sync

import android.util.Log
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType

sealed class LogoutResult {
    /** Signed out and local user data wiped. */
    data object LoggedOut : LogoutResult()

    /**
     * Local changes couldn't be pushed (offline, or the server refused).
     * Nothing has happened yet. Warn the user with these counts, then call
     * [LogoutCoordinator.logOutAndDelete] only if they confirm.
     */
    data class UnsyncedChanges(val pendingByType: Map<SyncEntityType, Int>) : LogoutResult()
}

/**
 * Logout's flush-and-warn:
 * - empty outbox: log out immediately
 * - non-empty: try to push it
 *   - pushed: log out immediately
 *   - failed or offline: return [LogoutResult.UnsyncedChanges] for the
 *     warning dialog
 *
 * Logging out always wipes local user data. Everything this user owns is
 * on the server by then (or the user chose to discard it), and the next
 * account on this device must not see it or inherit its cursor.
 */
class LogoutCoordinator(
    private val store: SyncLocalStore,
    private val flushOutbox: suspend () -> Boolean,
    private val auth: AuthSession
) {
    suspend fun requestLogout(): LogoutResult {
        if (store.outboxCount() == 0) return logOutAndDelete()

        val flushed = runCatching { flushOutbox() }
            .onFailure { Log.w(TAG, "pre-logout flush failed", it) }
            .getOrDefault(false)
        if (flushed) return logOutAndDelete()

        return LogoutResult.UnsyncedChanges(store.pendingCountsByType())
    }

    /** The "Log out and delete" confirmation: discards unsynced changes along with everything else. */
    suspend fun logOutAndDelete(): LogoutResult.LoggedOut {
        auth.signOut()
        store.wipeEverythingForLogout()
        return LogoutResult.LoggedOut
    }

    private companion object {
        const val TAG = "LogoutCoordinator"
    }
}

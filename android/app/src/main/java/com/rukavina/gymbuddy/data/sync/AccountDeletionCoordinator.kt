package com.rukavina.gymbuddy.data.sync

import android.util.Log
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import java.io.IOException

sealed class AccountDeletionResult {
    /** Server account gone, local user data wiped, signed out. */
    data object Deleted : AccountDeletionResult()

    /** Offline or the server failed. Nothing was deleted anywhere; retrying is safe. */
    data object Retryable : AccountDeletionResult()

    /** The server rejected the token (401). The user has to sign in again before retrying. */
    data object SignInAgain : AccountDeletionResult()
}

/**
 * Account deletion: `DELETE /v1/account` first, local wipe only after the
 * server confirmed it (204). The server deletes the data and the Firebase
 * user; the client never deletes the Firebase user itself.
 *
 * Unsynced outbox entries are not flushed first: the account is going
 * away, so they are discarded with everything else.
 *
 * [deleteOnServer] returns the HTTP status and throws [IOException] when
 * the request never got an answer. [runExclusive] runs a block under the
 * sync engine's lock, so no cycle can push, pull or commit a cursor
 * between the server delete and the wipe.
 */
class AccountDeletionCoordinator(
    private val store: SyncLocalStore,
    private val deleteOnServer: suspend () -> Int,
    private val deleteProfileImages: () -> Unit,
    private val auth: AuthSession,
    private val runExclusive: suspend (suspend () -> Unit) -> Unit
) {
    /**
     * Set once the server answered 204. The account is then provably gone,
     * so a retry after a failed wipe skips the server (which would now
     * answer 401) and only wipes. Cleared once the wipe succeeds, so this
     * singleton never skips the server for a later account.
     */
    @Volatile
    private var serverDeleted = false

    /** Pending outbox entries by type, for the confirmation dialog's "these will be lost" line. */
    suspend fun unsyncedChanges(): Map<SyncEntityType, Int> = store.pendingCountsByType()

    /** Throws if the local wipe fails; the server delete is remembered, so calling again only wipes. */
    suspend fun deleteAccount(): AccountDeletionResult {
        var result: AccountDeletionResult = AccountDeletionResult.Deleted
        runExclusive {
            if (!serverDeleted) {
                val status = try {
                    deleteOnServer()
                } catch (e: IOException) {
                    Log.w(TAG, "account delete request failed", e)
                    result = AccountDeletionResult.Retryable
                    return@runExclusive
                }
                when (status) {
                    204 -> serverDeleted = true
                    401 -> {
                        result = AccountDeletionResult.SignInAgain
                        return@runExclusive
                    }
                    else -> {
                        Log.w(TAG, "account delete answered $status")
                        result = AccountDeletionResult.Retryable
                        return@runExclusive
                    }
                }
            }
            // Same order as logout: wipe, then sign out, so a failed wipe
            // leaves the user signed in and able to retry.
            store.wipeEverythingForLogout()
            deleteProfileImages()
            auth.signOut()
            // Done: the next account on this device starts from the server again.
            serverDeleted = false
        }
        return result
    }

    private companion object {
        const val TAG = "AccountDeletion"
    }
}

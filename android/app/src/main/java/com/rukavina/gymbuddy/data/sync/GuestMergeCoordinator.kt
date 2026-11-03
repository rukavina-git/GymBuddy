package com.rukavina.gymbuddy.data.sync

import android.util.Log
import com.rukavina.gymbuddy.domain.sync.SyncOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface GuestMergeState {
    data object Idle : GuestMergeState

    /** The account already has data (or we couldn't check): ask before adding the guest's. */
    data class Asking(
        val uid: String,
        val accountEmail: String?,
        val accountWorkouts: Int,
        val guest: LocalDataCounts
    ) : GuestMergeState

    data object Merging : GuestMergeState

    data class Done(val receipt: GuestMergeReceipt) : GuestMergeState

    /** The merge threw. Nothing moved; the question comes back on the next start. */
    data object Failed : GuestMergeState
}

/**
 * Guest → account (ADR 0014). Runs after every switch to an account's
 * file. If the guest has data:
 * - sync the account first, so its server data is on the phone;
 * - account empty after a good sync (always true for a new account):
 *   merge without asking;
 * - otherwise (account has data, or the sync failed so we can't tell):
 *   ask. Add merges; Cancel signs out, back to the guest.
 *
 * No guest data: just the usual sign-in sync.
 *
 * A sign-up and a sign-in look the same from here (a uid appears), which
 * is why the rule is "empty account", not "new account".
 */
class GuestMergeCoordinator(
    private val merge: GuestMerge,
    private val syncNow: suspend () -> SyncOutcome,
    private val requestSync: () -> Unit,
    private val currentUid: () -> String?,
    private val accountEmail: () -> String?,
    private val signOut: () -> Unit,
    private val setGuest: suspend (Boolean) -> Unit
) {
    private val _state = MutableStateFlow<GuestMergeState>(GuestMergeState.Idle)
    val state: StateFlow<GuestMergeState> = _state.asStateFlow()

    /** Call after the holder switched to [uid]'s file (null: the guest's). */
    suspend fun onOwnerChanged(uid: String?) {
        _state.value = GuestMergeState.Idle
        if (uid == null) return
        val guest = merge.guestData()
        if (guest == null) {
            requestSync()
            return
        }
        val synced = syncNow() == SyncOutcome.SYNCED
        if (currentUid() != uid) return // switched again meanwhile
        val account = merge.openCounts()
        if (synced && account.isEmpty) {
            mergeNow(uid)
        } else {
            _state.value = GuestMergeState.Asking(uid, accountEmail(), account.workouts, guest)
        }
    }

    /** "Add and sign in". */
    suspend fun add() {
        val asking = _state.value as? GuestMergeState.Asking ?: return
        mergeNow(asking.uid)
    }

    /** Cancel: stay a guest. Signing out switches back to the guest's file. */
    suspend fun cancel() {
        if (_state.value !is GuestMergeState.Asking) return
        setGuest(true)
        _state.value = GuestMergeState.Idle
        signOut()
    }

    /** The receipt or the failure was shown. */
    fun dismiss() {
        val current = _state.value
        if (current is GuestMergeState.Done || current is GuestMergeState.Failed) {
            _state.value = GuestMergeState.Idle
        }
    }

    private suspend fun mergeNow(uid: String) {
        _state.value = GuestMergeState.Merging
        try {
            val receipt = merge.mergeInto(uid)
            if (receipt == null) {
                _state.value = GuestMergeState.Idle
                return
            }
            setGuest(false)
            requestSync()
            _state.value = GuestMergeState.Done(receipt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "guest merge failed", e)
            _state.value = GuestMergeState.Failed
        }
    }

    private companion object {
        const val TAG = "GuestMergeCoordinator"
    }
}

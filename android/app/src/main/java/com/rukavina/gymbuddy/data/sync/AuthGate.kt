package com.rukavina.gymbuddy.data.sync

import android.util.Log
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthGateState {
    data object Guest : AuthGateState

    data class SignedIn(val uid: String) : AuthGateState

    /** The session died; the account's data stays open and changes wait. "Backup paused." */
    data class SessionExpired(val owner: String, val email: String?) : AuthGateState

    /** The account is gone (deleted or disabled elsewhere): keep the data as a guest, or erase it. */
    data class AccountDeleted(val owner: String, val email: String?) : AuthGateState
}

/**
 * The one place that decides whose data is open (ADR 0014, roadmap 6.3).
 * Firebase's uid and the data owner ([DataOwnerStore]) are kept apart:
 *
 * - A uid signs in: it becomes the owner, its file opens, then
 *   [afterSignIn] (the sign-in sync and guest merge).
 * - Firebase drops the user and the owner was released (a voluntary
 *   sign-out): the guest's file opens.
 * - Firebase drops the user without a release: the session died. The
 *   owner's file stays open, logging goes on, the outbox waits (the
 *   engine only syncs when Firebase's uid owns the open file).
 *   [lastFailure] tells expired from deleted.
 * - A sync 401 ([onUnauthorized]): [probe] forces a token refresh. Offline
 *   or fine: nothing. Expired or deleted: Firebase is signed out (without
 *   a release), which lands in the case above.
 *
 * No forced Login and no wipe, except Erase after a deletion.
 */
class AuthGate(
    private val holder: DatabaseHolder,
    private val owners: DataOwnerStore,
    private val merge: GuestMerge,
    private val firebaseUid: () -> String?,
    private val probe: suspend () -> AuthProbe,
    private val lastFailure: () -> AuthProbe?,
    private val clearFailure: () -> Unit,
    private val firebaseEmail: () -> String?,
    /** Firebase's own sign-out, without releasing the owner. */
    private val dropDeadSession: () -> Unit,
    private val afterSignIn: suspend (uid: String) -> Unit,
    private val setGuest: suspend (Boolean) -> Unit,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(
        owners.owner?.let { AuthGateState.SignedIn(it) } ?: AuthGateState.Guest
    )
    val state: StateFlow<AuthGateState> = _state.asStateFlow()

    /** Firebase's uid changed (or was restored at startup). Called one at a time. */
    suspend fun onFirebaseUid(uid: String?) {
        if (uid != null) {
            clearFailure()
            if (owners.owner != uid || owners.email == null) owners.set(uid, firebaseEmail())
            holder.switchTo(uid)
            setGuest(false)
            _state.value = AuthGateState.SignedIn(uid)
            afterSignIn(uid)
            return
        }
        val owner = owners.owner
        if (owner == null) {
            holder.switchTo(null)
            _state.value = AuthGateState.Guest
            return
        }
        // Nobody chose to leave: the account's data stays open.
        holder.switchTo(owner)
        _state.value = if (lastFailure() == AuthProbe.DELETED) {
            AuthGateState.AccountDeleted(owner, owners.email)
        } else {
            AuthGateState.SessionExpired(owner, owners.email)
        }
        Log.i(TAG, "session ended without a sign-out: ${_state.value::class.simpleName}")
    }

    /**
     * A voluntary exit releases the owner before Firebase signs out. When
     * Firebase had already dropped the user (logout while the session is
     * dead), its uid stays null and no change arrives, so the release
     * itself opens the guest's file. Call once at startup.
     */
    fun observeReleases() {
        scope.launch {
            owners.ownerFlow.collect { owner ->
                if (owner == null && firebaseUid() == null && holder.owner != null) onFirebaseUid(null)
            }
        }
    }

    /** A sync answered 401. Checks the session off the caller's thread. */
    fun onUnauthorized() {
        scope.launch { checkSession() }
    }

    suspend fun checkSession() {
        if (owners.owner == null) return
        when (probe()) {
            AuthProbe.VALID, AuthProbe.OFFLINE -> Unit
            // Firebase may still hold the dead user; dropping it sends the
            // listener a null uid, and onFirebaseUid takes it from there.
            AuthProbe.EXPIRED, AuthProbe.DELETED -> dropDeadSession()
        }
    }

    /** "Keep on this phone" after a deletion: the account's data becomes the guest's. */
    suspend fun keepAsGuest() {
        val deleted = _state.value as? AuthGateState.AccountDeleted ?: return
        merge.keepAccountAsGuest(deleted.owner)
        leave(deleted.owner)
        setGuest(true)
    }

    /** "Erase" after a deletion: the account's file goes, nothing is kept. */
    suspend fun eraseDeletedAccount() {
        val deleted = _state.value as? AuthGateState.AccountDeleted ?: return
        leave(deleted.owner)
        setGuest(false)
    }

    private suspend fun leave(owner: String) {
        owners.release()
        holder.switchTo(null)
        merge.deleteOwnerFile(owner)
        _state.value = AuthGateState.Guest
    }

    private companion object {
        const val TAG = "AuthGate"
    }
}

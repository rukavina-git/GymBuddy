package com.rukavina.gymbuddy.ui.guest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.sync.AuthGate
import com.rukavina.gymbuddy.data.sync.AuthGateState
import com.rukavina.gymbuddy.data.sync.GuestMerge
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.data.sync.SignedOutPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MainScreen's dialogs for a dead session (AuthGate): "You're signed out"
 * (Plan 9 step 04a, [SignedOutPrompt] decides when it opens by itself)
 * and "This account was deleted".
 */
@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val gate: AuthGate,
    private val merge: GuestMerge,
    private val prompt: SignedOutPrompt
) : ViewModel() {

    val state: StateFlow<AuthGateState> = gate.state

    private val _signedOutDialog = MutableStateFlow(false)
    /** The blocking "You're signed out" dialog is up. */
    val signedOutDialog: StateFlow<Boolean> = _signedOutDialog

    // Opened by the rule (counts toward the limit), not by a tap on a marker.
    private var openedByRule = false

    // Sign in was tapped; if the user comes back still signed out, the dialog returns.
    private var leftForSignIn = false

    init {
        viewModelScope.launch {
            gate.state.collect { s ->
                when (s) {
                    is AuthGateState.SessionExpired -> if (!_signedOutDialog.value && prompt.shouldShow()) {
                        openedByRule = true
                        _signedOutDialog.value = true
                    }
                    // The count starts over on a real Firebase sign-in (SyncModule
                    // afterSignIn), not here: at startup the gate briefly says
                    // SignedIn from the stored owner before Firebase answers.
                    else -> close()
                }
            }
        }
    }

    /** The Signed out chip on Home and the You card. */
    fun openSignedOutDialog() {
        if (state.value is AuthGateState.SessionExpired) _signedOutDialog.value = true
    }

    fun continueWithoutAccount() {
        if (openedByRule) prompt.onContinue()
        close()
    }

    /** Sign in leaves Main; the dialog must not sit over the sign-in screen. */
    fun leaveForSignIn() {
        leftForSignIn = true
        _signedOutDialog.value = false
    }

    /** Main is back on screen: no choice was made yet if sign-in was left without signing in. */
    fun onMainResumed() {
        if (leftForSignIn && state.value is AuthGateState.SessionExpired) _signedOutDialog.value = true
        leftForSignIn = false
    }

    private fun close() {
        _signedOutDialog.value = false
        openedByRule = false
        leftForSignIn = false
    }

    private val _counts = MutableStateFlow<LocalDataCounts?>(null)
    /** What's on the phone for the deleted account, for the dialog. */
    val counts: StateFlow<LocalDataCounts?> = _counts

    private val _erased = MutableSharedFlow<Unit>()
    /** After Erase: back to Login. */
    val erased: SharedFlow<Unit> = _erased.asSharedFlow()

    fun loadCounts() {
        viewModelScope.launch { _counts.value = merge.openCounts() }
    }

    fun keep() {
        viewModelScope.launch { gate.keepAsGuest() }
    }

    fun erase() {
        viewModelScope.launch {
            gate.eraseDeletedAccount()
            _erased.emit(Unit)
        }
    }
}

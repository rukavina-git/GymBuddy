package com.rukavina.gymbuddy.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.auth.AuthClient
import com.rukavina.gymbuddy.data.auth.AuthError
import com.rukavina.gymbuddy.data.auth.AuthException
import com.rukavina.gymbuddy.data.auth.SignInSupport
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** "Welcome back" (I have an account) or "Back up your workouts" (create account). */
enum class SignInMode { SIGN_IN, CREATE }

/** The line under the form. Wrong email and wrong password read the same (enumeration protection). */
enum class FormError { EMPTY, WRONG_CREDENTIALS, INVALID_EMAIL, WEAK_PASSWORD, EMAIL_IN_USE, NEED_EMAIL_FOR_RESET, OTHER }

sealed interface SignInDialog {
    /** The Google account has no GymBuddy account; the probe made one, not yet kept. */
    data class NoAccount(val email: String?) : SignInDialog

    /** Another account's changes haven't backed up (Figma "Changes from another account"). */
    data class OtherAccount(val ownerEmail: String, val newEmail: String, val changes: Int) : SignInDialog

    /** "Why?" next to the consent box. */
    data object WhyConsent : SignInDialog
}

data class SignInUiState(
    val mode: SignInMode = SignInMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val consent: Boolean = false,
    /** Tapped create without the box: the reason shows under it. */
    val consentMissing: Boolean = false,
    /** What moves into the new account (create mode). */
    val counts: LocalDataCounts? = null,
    val error: FormError? = null,
    /** "No connection. Signing in needs internet." with Retry. */
    val offline: Boolean = false,
    val busy: Boolean = false,
    val dialog: SignInDialog? = null,
)

sealed interface SignInEvent {
    /** Signed in or created: on to Main. */
    data object Done : SignInEvent

    /** "Create account" from "No account for this email yet". */
    data object OpenCreateAccount : SignInEvent

    /** "Use another account": open Google's sheet again, all accounts. */
    data object PickAnotherGoogleAccount : SignInEvent

    data class ResetSent(val email: String) : SignInEvent
}

/**
 * One screen for sign-in and create account (Figma 114:20520, Plan 7
 * step 04). Google's sheet runs in the UI (it needs an Activity) and
 * hands the token over with [onGoogleToken].
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val client: AuthClient,
    private val support: SignInSupport,
    private val preferences: AppPreferencesRepository,
    private val clock: Clock,
) : ViewModel() {

    private val mode = savedState.get<String>(ARG_MODE)?.let { runCatching { SignInMode.valueOf(it) }.getOrNull() }
        ?: SignInMode.SIGN_IN

    private val _state = MutableStateFlow(SignInUiState(mode = mode))
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SignInEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<SignInEvent> = _events.asSharedFlow()

    /** What Retry and "Continue as …" run again. */
    private var pending: (suspend () -> Unit)? = null

    init {
        viewModelScope.launch {
            if (mode == SignInMode.SIGN_IN) {
                preferences.lastSignInEmail.first()?.let { last ->
                    _state.update { if (it.email.isEmpty()) it.copy(email = last) else it }
                }
            } else {
                val counts = runCatching { support.openCounts() }.getOrNull()
                _state.update { it.copy(counts = counts) }
            }
        }
    }

    fun onEmail(value: String) = _state.update { it.copy(email = value.trim(), error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun toggleShowPassword() = _state.update { it.copy(showPassword = !it.showPassword) }
    fun onConsent(checked: Boolean) = _state.update { it.copy(consent = checked, consentMissing = false) }
    fun showWhy() = _state.update { it.copy(dialog = SignInDialog.WhyConsent) }
    fun dismissDialog() = _state.update { it.copy(dialog = null) }

    /** Create mode needs the box before Google's sheet opens. */
    fun canStartGoogle(): Boolean {
        if (mode == SignInMode.CREATE && !_state.value.consent) {
            _state.update { it.copy(consentMissing = true) }
            return false
        }
        return true
    }

    /** Google's sheet failed for a reason other than the user closing it. */
    fun onGoogleFailed(offline: Boolean) {
        if (offline) pending = null
        _state.update { if (offline) it.copy(offline = true) else it.copy(error = FormError.OTHER) }
    }

    fun onGoogleToken(idToken: String, email: String?) {
        if (!canStartGoogle()) return
        val action: suspend () -> Unit = if (mode == SignInMode.CREATE) {
            { createWithGoogle(idToken) }
        } else {
            { signInWithGoogle(idToken) }
        }
        start(email, action)
    }

    fun submitEmail() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isEmpty()) {
            _state.update { it.copy(error = FormError.EMPTY) }
            return
        }
        if (mode == SignInMode.CREATE) {
            if (!s.consent) {
                _state.update { it.copy(consentMissing = true) }
                return
            }
            if (s.password.length < MIN_PASSWORD) {
                _state.update { it.copy(error = FormError.WEAK_PASSWORD) }
                return
            }
            start(s.email) { createWithEmail(s.email, s.password) }
        } else {
            start(s.email) { signInWithEmail(s.email, s.password) }
        }
    }

    fun forgotPassword() {
        val email = _state.value.email
        if (email.isBlank()) {
            _state.update { it.copy(error = FormError.NEED_EMAIL_FOR_RESET) }
            return
        }
        run({
            try {
                client.sendPasswordReset(email)
            } catch (e: AuthException) {
                if (e.error == AuthError.OFFLINE) throw e
                // Unknown address or not: the answer is the same.
            }
            _events.emit(SignInEvent.ResetSent(email))
        })
    }

    fun retry() {
        _state.update { it.copy(offline = false) }
        val action = pending
        // Google's sheet itself failed offline: open it again.
        if (action == null) _events.tryEmit(SignInEvent.PickAnotherGoogleAccount) else run(action)
    }

    /** "Continue as …" on the other-account question. */
    fun continueAsNew() {
        _state.update { it.copy(dialog = null) }
        pending?.let { run(it) }
    }

    /** "Sign in as … instead": back to the form with the old account's email. */
    fun switchToOwner() {
        val dialog = _state.value.dialog as? SignInDialog.OtherAccount ?: return
        pending = null
        _state.update { it.copy(dialog = null, email = dialog.ownerEmail, password = "") }
    }

    /** "Create account" on "No account for this email yet". */
    fun noAccountCreate() = leaveProbe(SignInEvent.OpenCreateAccount)

    /** "Use another account". */
    fun noAccountOther() = leaveProbe(SignInEvent.PickAnotherGoogleAccount)

    private fun leaveProbe(event: SignInEvent) {
        _state.update { it.copy(dialog = null) }
        viewModelScope.launch {
            client.endProbe(delete = true)
            _events.emit(event)
        }
    }

    /**
     * Asks first when another account's changes are still waiting on this
     * phone (they stay apart and back up when that account returns).
     */
    private fun start(newEmail: String?, action: suspend () -> Unit) {
        pending = action
        viewModelScope.launch {
            val owner = support.ownerEmail
            val changes = if (owner != null && newEmail != null && !owner.equals(newEmail, ignoreCase = true)) {
                support.pendingChanges()
            } else 0
            if (owner != null && newEmail != null && changes > 0) {
                _state.update { it.copy(dialog = SignInDialog.OtherAccount(owner, newEmail, changes)) }
            } else {
                run(action)
            }
        }
    }

    private fun run(action: suspend () -> Unit) {
        if (_state.value.busy) return
        pending = action
        _state.update { it.copy(busy = true, error = null, offline = false) }
        viewModelScope.launch {
            try {
                action()
            } catch (e: AuthException) {
                _state.update {
                    when (e.error) {
                        AuthError.OFFLINE -> it.copy(offline = true)
                        AuthError.WRONG_CREDENTIALS -> it.copy(error = FormError.WRONG_CREDENTIALS)
                        AuthError.INVALID_EMAIL -> it.copy(error = FormError.INVALID_EMAIL)
                        AuthError.WEAK_PASSWORD -> it.copy(error = FormError.WEAK_PASSWORD)
                        AuthError.EMAIL_IN_USE -> it.copy(error = FormError.EMAIL_IN_USE)
                        AuthError.OTHER -> it.copy(error = FormError.OTHER)
                    }
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private suspend fun signInWithGoogle(idToken: String) {
        val probe = try {
            client.probeGoogle(idToken)
        } catch (e: AuthException) {
            client.endProbe(delete = false)
            throw e
        }
        if (probe.isNew) {
            // Firebase made the account on the probe; ask before keeping it.
            _state.update { it.copy(dialog = SignInDialog.NoAccount(probe.email)) }
            return
        }
        client.endProbe(delete = false)
        client.signInWithGoogle(idToken)
        _events.emit(SignInEvent.Done)
    }

    private suspend fun createWithGoogle(idToken: String) = withConsent { client.signInWithGoogle(idToken) }

    private suspend fun signInWithEmail(email: String, password: String) {
        client.signInWithEmail(email, password)
        preferences.setLastSignInEmail(email)
        _events.emit(SignInEvent.Done)
    }

    private suspend fun createWithEmail(email: String, password: String) = withConsent {
        client.createWithEmail(email, password)
        preferences.setLastSignInEmail(email)
    }

    /**
     * The consent must be pending before the account appears: AuthGate
     * hands it to ConsentRecorder on sign-in.
     */
    private suspend fun withConsent(signIn: suspend () -> Unit) {
        preferences.setPendingConsentAt(clock.millis())
        try {
            signIn()
        } catch (e: Exception) {
            preferences.setPendingConsentAt(null)
            throw e
        }
        _events.emit(SignInEvent.Done)
    }

    companion object {
        const val ARG_MODE = "mode"
        const val MIN_PASSWORD = 8
    }
}

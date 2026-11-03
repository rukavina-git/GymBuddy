package com.rukavina.gymbuddy.ui.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.auth.AuthClient
import com.rukavina.gymbuddy.data.auth.AuthError
import com.rukavina.gymbuddy.data.auth.AuthException
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Create account in three short screens (Figma R18-3 … R18-5). */
enum class CreateStep { METHOD, EMAIL, CONSENT }

/** Where the flow started: decides the method screen's chrome and where Done and Not now lead. */
enum class CreateFrom { WELCOME, SIGN_IN, SETUP, SETTINGS }

data class CreateAccountUiState(
    val from: CreateFrom = CreateFrom.WELCOME,
    val step: CreateStep = CreateStep.METHOD,
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    /** Google picked on the method screen; its token waits for the consent tap. */
    val withGoogle: Boolean = false,
    val error: FormError? = null,
    val offline: Boolean = false,
    val busy: Boolean = false,
)

sealed interface CreateAccountEvent {
    /** The account exists. [newAccount] false: Google found an existing one (sign-in, not setup). */
    data class Created(val newAccount: Boolean) : CreateAccountEvent

    /** "Not now": stay a guest. */
    data object NotNow : CreateAccountEvent

    /** Google's sheet failed offline; Retry opens it again. */
    data object RetryGoogle : CreateAccountEvent
}

/**
 * Method → (email) → consent. Nothing is created before "Agree and
 * create account": the tap is the health-data consent (amendment 1),
 * parked in preferences first so AuthGate records it on sign-in.
 */
@HiltViewModel
class CreateAccountViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val client: AuthClient,
    private val preferences: AppPreferencesRepository,
    private val clock: Clock,
) : ViewModel() {

    private val from = savedState.get<String>(ARG_FROM)?.let { runCatching { CreateFrom.valueOf(it) }.getOrNull() }
        ?: CreateFrom.WELCOME

    private val _state = MutableStateFlow(CreateAccountUiState(from = from))
    val state: StateFlow<CreateAccountUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<CreateAccountEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CreateAccountEvent> = _events.asSharedFlow()

    private var googleToken: String? = null

    fun chooseEmail() = _state.update { it.copy(step = CreateStep.EMAIL, withGoogle = false, error = null, offline = false) }

    fun onGoogleToken(idToken: String) {
        googleToken = idToken
        _state.update { it.copy(step = CreateStep.CONSENT, withGoogle = true, error = null, offline = false) }
    }

    /** Google's sheet failed for a reason other than the user closing it. */
    fun onGoogleFailed(offline: Boolean) =
        _state.update { if (offline) it.copy(offline = true) else it.copy(error = FormError.OTHER) }

    fun onEmail(value: String) = _state.update { it.copy(email = value.trim(), error = null) }
    fun onPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun toggleShowPassword() = _state.update { it.copy(showPassword = !it.showPassword) }

    /** Email screen's Continue: checks the form, then asks for consent. */
    fun continueWithEmail() {
        val s = _state.value
        val error = when {
            s.email.isBlank() || s.password.isEmpty() -> FormError.EMPTY
            !s.email.contains('@') -> FormError.INVALID_EMAIL
            s.password.length < MIN_PASSWORD -> FormError.WEAK_PASSWORD
            else -> null
        }
        _state.update { if (error != null) it.copy(error = error) else it.copy(step = CreateStep.CONSENT, error = null) }
    }

    /** "Agree and create account". */
    fun agree() {
        val s = _state.value
        if (s.busy || s.step != CreateStep.CONSENT) return
        val token = googleToken
        if (s.withGoogle && token == null) { // lost with the process: pick the account again
            _state.update { it.copy(step = CreateStep.METHOD, withGoogle = false) }
            return
        }
        _state.update { it.copy(busy = true, error = null, offline = false) }
        viewModelScope.launch {
            preferences.setPendingConsentAt(clock.millis())
            try {
                val newAccount = if (s.withGoogle) {
                    client.signInWithGoogle(checkNotNull(token))
                } else {
                    client.createWithEmail(s.email, s.password)
                    preferences.setLastSignInEmail(s.email)
                    true
                }
                _events.emit(CreateAccountEvent.Created(newAccount))
            } catch (e: AuthException) {
                preferences.setPendingConsentAt(null)
                _state.update {
                    when (e.error) {
                        AuthError.OFFLINE -> it.copy(offline = true)
                        // Fixable on the email screen: go back there with the reason.
                        AuthError.EMAIL_IN_USE, AuthError.INVALID_EMAIL, AuthError.WEAK_PASSWORD ->
                            it.copy(step = CreateStep.EMAIL, error = e.error.toFormError())
                        AuthError.WRONG_CREDENTIALS, AuthError.OTHER -> it.copy(error = FormError.OTHER)
                    }
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(offline = false) }
        if (_state.value.step == CreateStep.CONSENT) agree() else _events.tryEmit(CreateAccountEvent.RetryGoogle)
    }

    fun notNow() {
        _events.tryEmit(CreateAccountEvent.NotNow)
    }

    /** Back walks the steps; false on the method screen (the caller leaves the flow). */
    fun back(): Boolean {
        val s = _state.value
        if (s.busy) return true
        when (s.step) {
            CreateStep.METHOD -> return false
            CreateStep.EMAIL -> _state.update { it.copy(step = CreateStep.METHOD, error = null) }
            CreateStep.CONSENT -> {
                googleToken = null
                _state.update {
                    it.copy(step = if (it.withGoogle) CreateStep.METHOD else CreateStep.EMAIL, withGoogle = false, error = null, offline = false)
                }
            }
        }
        return true
    }

    private fun AuthError.toFormError() = when (this) {
        AuthError.EMAIL_IN_USE -> FormError.EMAIL_IN_USE
        AuthError.INVALID_EMAIL -> FormError.INVALID_EMAIL
        AuthError.WEAK_PASSWORD -> FormError.WEAK_PASSWORD
        else -> FormError.OTHER
    }

    companion object {
        const val ARG_FROM = "from"
        const val MIN_PASSWORD = 8
    }
}

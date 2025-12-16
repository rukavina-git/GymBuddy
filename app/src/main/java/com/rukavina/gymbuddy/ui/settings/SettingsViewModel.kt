package com.rukavina.gymbuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val userEmail: String = "",
    val profileImageUri: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

            uid?.let { userId ->
                val profile = repository.getProfile(userId)
                _uiState.value = if (profile != null) {
                    SettingsUiState(
                        userName = profile.name,
                        userEmail = profile.email.ifBlank { firebaseEmail },
                        profileImageUri = profile.profileImageUrl,
                        isLoading = false
                    )
                } else {
                    SettingsUiState(
                        userName = "",
                        userEmail = firebaseEmail,
                        profileImageUri = null,
                        isLoading = false
                    )
                }
            } ?: run {
                _uiState.value = SettingsUiState(
                    userName = "",
                    userEmail = firebaseEmail,
                    profileImageUri = null,
                    isLoading = false
                )
            }
        }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        viewModelScope.launch {
            _logoutEvent.emit(Unit)
        }
    }
}

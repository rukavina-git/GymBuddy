package com.rukavina.gymbuddy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.data.model.UserProfile
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            uid?.let {
                val profile = repository.getProfile(it)
                profile?.let { p ->
                    _uiState.value = _uiState.value.copy(
                        firstName = p.firstName,
                        lastName = p.lastName,
                        email = p.email,
                        age = p.age.toString(),
                        weight = p.weight.toString(),
                        height = p.height.toString(),
                        gender = p.gender.toString(),
                        goal = p.goal
                    )
                }
            }
        }
    }

    fun onFieldChanged(field: ProfileField, value: String) {
        _uiState.value = when (field) {
            ProfileField.FirstName -> _uiState.value.copy(firstName = value)
            ProfileField.LastName -> _uiState.value.copy(lastName = value)
            ProfileField.Email -> _uiState.value.copy(email = value)
            ProfileField.Age -> _uiState.value.copy(age = value)
            ProfileField.Weight -> _uiState.value.copy(weight = value)
            ProfileField.Height -> _uiState.value.copy(height = value)
            ProfileField.Gender -> _uiState.value.copy(gender = value)
            ProfileField.Goal -> _uiState.value.copy(goal = value)
        }
    }

    fun onSaveClick() {
        viewModelScope.launch {
            uid?.let {
                val profile = UserProfile(
                    uid = it,
                    firstName = _uiState.value.firstName,
                    lastName = _uiState.value.lastName,
                    email = _uiState.value.email,
                    age = _uiState.value.age.toIntOrNull() ?: 0,
                    weight = _uiState.value.weight.toFloatOrNull() ?: 0f,
                    height = _uiState.value.height.toFloatOrNull() ?: 0f,
                    gender = _uiState.value.gender,
                    goal = _uiState.value.goal
                )
                repository.saveProfile(profile)
                _uiState.value = _uiState.value.copy(message = "Profile saved.")
            }
        }
    }

    fun onImageClick() {
        // TODO: Launch image picker
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        viewModelScope.launch {
            _logoutEvent.emit(Unit)
        }
    }
}
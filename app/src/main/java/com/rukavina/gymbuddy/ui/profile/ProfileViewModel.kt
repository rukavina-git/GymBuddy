package com.rukavina.gymbuddy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.data.model.PreferredUnits
import com.rukavina.gymbuddy.data.model.UserProfile
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.utils.UnitConverter
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
            // Get email from Firebase Auth
            val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

            uid?.let { userId ->
                val profile = repository.getProfile(userId)
                profile?.let { p ->
                    // Convert from metric (database storage) to user's preferred display units
                    _uiState.value = _uiState.value.copy(
                        name = p.name,
                        email = p.email.ifBlank { firebaseEmail },
                        age = p.age?.toString() ?: "",
                        weight = UnitConverter.weightToDisplayUnit(p.weight, p.preferredUnits),
                        height = UnitConverter.heightToDisplayUnit(p.height, p.preferredUnits),
                        targetWeight = UnitConverter.weightToDisplayUnit(p.targetWeight, p.preferredUnits),
                        gender = p.gender,
                        fitnessGoal = p.fitnessGoal,
                        activityLevel = p.activityLevel,
                        preferredUnits = p.preferredUnits,
                        bio = p.bio ?: ""
                    )
                } ?: run {
                    // No profile exists, set email from Firebase
                    _uiState.value = _uiState.value.copy(email = firebaseEmail)
                }
            }
        }
    }

    fun onFieldChanged(field: ProfileField, value: String) {
        _uiState.value = when (field) {
            ProfileField.Name -> _uiState.value.copy(name = value)
            ProfileField.Age -> _uiState.value.copy(age = value)
            ProfileField.Weight -> _uiState.value.copy(weight = value)
            ProfileField.Height -> _uiState.value.copy(height = value)
            ProfileField.TargetWeight -> _uiState.value.copy(targetWeight = value)
            ProfileField.Bio -> _uiState.value.copy(bio = value)
            else -> _uiState.value
        }
    }

    fun onGenderChanged(gender: com.rukavina.gymbuddy.data.model.Gender?) {
        _uiState.value = _uiState.value.copy(gender = gender)
    }

    fun onFitnessGoalChanged(goal: com.rukavina.gymbuddy.data.model.FitnessGoal?) {
        _uiState.value = _uiState.value.copy(fitnessGoal = goal)
    }

    fun onActivityLevelChanged(level: com.rukavina.gymbuddy.data.model.ActivityLevel?) {
        _uiState.value = _uiState.value.copy(activityLevel = level)
    }

    fun onPreferredUnitsChanged(units: PreferredUnits) {
        val currentState = _uiState.value
        val oldUnits = currentState.preferredUnits

        // Convert currently displayed values from old units to new units
        val currentWeightInMetric = UnitConverter.weightToMetric(currentState.weight, oldUnits)
        val currentHeightInMetric = UnitConverter.heightToMetric(currentState.height, oldUnits)
        val currentTargetWeightInMetric = UnitConverter.weightToMetric(currentState.targetWeight, oldUnits)

        _uiState.value = currentState.copy(
            preferredUnits = units,
            weight = UnitConverter.weightToDisplayUnit(currentWeightInMetric, units),
            height = UnitConverter.heightToDisplayUnit(currentHeightInMetric, units),
            targetWeight = UnitConverter.weightToDisplayUnit(currentTargetWeightInMetric, units)
        )
    }

    fun onSaveClick() {
        viewModelScope.launch {
            uid?.let { userId ->
                val currentState = _uiState.value

                // Validate required fields
                if (currentState.name.isBlank()) {
                    _uiState.value = currentState.copy(message = "Name is required")
                    return@launch
                }
                if (currentState.fitnessGoal == null) {
                    _uiState.value = currentState.copy(message = "Please select a fitness goal")
                    return@launch
                }

                // Convert from display units to metric for database storage
                val weightInKg = UnitConverter.weightToMetric(currentState.weight, currentState.preferredUnits)
                val heightInCm = UnitConverter.heightToMetric(currentState.height, currentState.preferredUnits)
                val targetWeightInKg = UnitConverter.weightToMetric(currentState.targetWeight, currentState.preferredUnits)

                val profile = UserProfile(
                    uid = userId,
                    name = currentState.name,
                    email = currentState.email,
                    profileImageUrl = null, // TODO: Implement image upload
                    age = currentState.age.toIntOrNull(),
                    weight = weightInKg,
                    height = heightInCm,
                    targetWeight = targetWeightInKg,
                    gender = currentState.gender,
                    fitnessGoal = currentState.fitnessGoal!!,
                    activityLevel = currentState.activityLevel,
                    preferredUnits = currentState.preferredUnits,
                    joinedDate = System.currentTimeMillis(), // TODO: Store actual join date
                    bio = currentState.bio.ifBlank { null }
                )
                repository.saveProfile(profile)
                _uiState.value = currentState.copy(message = "Profile saved.")
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
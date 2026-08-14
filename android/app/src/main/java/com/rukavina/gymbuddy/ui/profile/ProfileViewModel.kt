package com.rukavina.gymbuddy.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.UserProfile
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.Gender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import javax.inject.Inject

private const val TAG = "ProfileViewModel"

/**
 * Weight/height/targetWeight in ProfileUiState are always metric (kg/cm) -
 * matching UserProfile storage. Display-unit conversion happens only in
 * ProfileScreen and the Edit*Screen composables, which already take
 * preferredUnits and convert at their own render/parse boundary. This
 * ViewModel never converts between metric and display units.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    private val appPreferencesRepository: AppPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    private val uid: String? = FirebaseAuth.getInstance().currentUser?.uid

    // Store the saved profile state for change tracking
    private var savedProfileState = ProfileUiState()

    init {
        loadProfile()
        viewModelScope.launch {
            appPreferencesRepository.preferredUnits.collect { units ->
                _uiState.value = _uiState.value.copy(preferredUnits = units)
            }
        }
    }

    fun refreshProfile() {
        loadProfile()
    }

    fun hasUnsavedChanges(): Boolean {
        val current = _uiState.value
        val saved = savedProfileState

        return current.name != saved.name ||
                current.birthDate != saved.birthDate ||
                current.weight != saved.weight ||
                current.height != saved.height ||
                current.targetWeight != saved.targetWeight ||
                current.gender != saved.gender ||
                current.fitnessGoal != saved.fitnessGoal ||
                current.activityLevel != saved.activityLevel ||
                current.preferredUnits != saved.preferredUnits ||
                current.bio != saved.bio ||
                current.profileImageUri != saved.profileImageUri
    }

    private fun loadProfile() {
        Log.d(TAG, "loadProfile called")
        viewModelScope.launch {
            // Get email from Firebase Auth
            val firebaseEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

            uid?.let { userId ->
                Log.d(TAG, "Loading profile for uid: $userId")
                val profile = repository.getProfile(userId)
                Log.d(TAG, "Loaded profile: $profile")
                profile?.let { p ->
                    Log.d(TAG, "Profile imageUrl from DB: ${p.profileImageUrl}")
                    _uiState.value = _uiState.value.copy(
                        name = p.name,
                        email = p.email.ifBlank { firebaseEmail },
                        birthDate = p.birthDate,
                        weight = p.weight,
                        height = p.height,
                        targetWeight = p.targetWeight,
                        gender = p.gender,
                        fitnessGoal = p.fitnessGoal,
                        activityLevel = p.activityLevel,
                        bio = p.bio ?: "",
                        profileImageUri = p.profileImageUrl
                    )
                    Log.d(TAG, "UI state updated with profileImageUri: ${_uiState.value.profileImageUri}")
                    // Save the loaded state as baseline for change tracking
                    savedProfileState = _uiState.value
                } ?: run {
                    Log.d(TAG, "No profile found, using default")
                    // No profile exists, set email from Firebase
                    _uiState.value = _uiState.value.copy(email = firebaseEmail)
                    savedProfileState = _uiState.value
                }
            } ?: run {
                Log.e(TAG, "Cannot load profile - uid is null")
            }
        }
    }

    fun onFieldChanged(field: ProfileField, value: String) {
        _uiState.value = when (field) {
            ProfileField.Name -> _uiState.value.copy(name = value)
            ProfileField.Weight -> _uiState.value.copy(weight = value.toFloatOrNull())
            ProfileField.Height -> _uiState.value.copy(height = value.toFloatOrNull())
            ProfileField.TargetWeight -> _uiState.value.copy(targetWeight = value.toFloatOrNull())
            ProfileField.Bio -> _uiState.value.copy(bio = value)
            else -> _uiState.value
        }
    }

    fun onGenderChanged(gender: Gender?) {
        _uiState.value = _uiState.value.copy(gender = gender)
    }

    fun onFitnessGoalChanged(goal: FitnessGoal?) {
        _uiState.value = _uiState.value.copy(fitnessGoal = goal)
    }

    fun onActivityLevelChanged(level: ActivityLevel?) {
        _uiState.value = _uiState.value.copy(activityLevel = level)
    }

    fun onSaveClick() {
        Log.d(TAG, "onSaveClick called")
        viewModelScope.launch {
            Log.d(TAG, "uid: $uid")
            uid?.let { userId ->
                var currentState = _uiState.value
                Log.d(TAG, "Current profileImageUri: ${currentState.profileImageUri}")

                // Use default name if blank
                if (currentState.name.isBlank()) {
                    Log.d(TAG, "Name is blank, using default 'User'")
                    currentState = currentState.copy(name = "User")
                    _uiState.value = currentState
                }

                // joinedDate must not be clobbered on every save - read the
                // existing profile first, same as saveProfileToDatabase.
                val existingProfile = repository.getProfile(userId)

                val profile = UserProfile(
                    uid = userId,
                    name = currentState.name,
                    email = currentState.email,
                    profileImageUrl = currentState.profileImageUri,
                    birthDate = currentState.birthDate,
                    weight = currentState.weight,
                    height = currentState.height,
                    targetWeight = currentState.targetWeight,
                    gender = currentState.gender,
                    fitnessGoal = currentState.fitnessGoal,
                    activityLevel = currentState.activityLevel,
                    joinedDate = existingProfile?.joinedDate ?: System.currentTimeMillis(),
                    bio = currentState.bio.ifBlank { null }
                )
                Log.d(TAG, "Saving profile with imageUrl: ${profile.profileImageUrl}")
                repository.saveProfile(profile)
                Log.d(TAG, "Profile saved successfully")
                _uiState.value = currentState.copy(message = "Profile saved.")
                // Update saved state after successful save
                savedProfileState = _uiState.value
            } ?: run {
                Log.e(TAG, "Cannot save - uid is null")
            }
        }
    }

    fun onCancelClick() {
        // Revert to the last saved state
        _uiState.value = savedProfileState.copy(
            message = null // Don't restore old messages
        )
    }

    fun onImageSelected(uri: String) {
        _uiState.value = _uiState.value.copy(profileImageUri = uri)
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        viewModelScope.launch {
            _logoutEvent.emit(Unit)
        }
    }

    // Individual field save methods
    fun onNameSaved(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
        saveProfileToDatabase()
    }

    fun onBioSaved(bio: String) {
        _uiState.value = _uiState.value.copy(bio = bio)
        saveProfileToDatabase()
    }

    fun onBirthDateSaved(birthDate: Long) {
        _uiState.value = _uiState.value.copy(birthDate = birthDate)
        saveProfileToDatabase()
    }

    fun calculateAge(birthDateMillis: Long?): Int? {
        if (birthDateMillis == null) return null
        val birthDate = Instant.ofEpochMilli(birthDateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val today = LocalDate.now()
        return Period.between(birthDate, today).years
    }

    fun onWeightSaved(weightKg: Float) {
        _uiState.value = _uiState.value.copy(weight = weightKg)
        saveProfileToDatabase()
    }

    fun onHeightSaved(heightCm: Float) {
        _uiState.value = _uiState.value.copy(height = heightCm)
        saveProfileToDatabase()
    }

    fun onTargetWeightSaved(weightKg: Float) {
        _uiState.value = _uiState.value.copy(targetWeight = weightKg)
        saveProfileToDatabase()
    }

    fun onGenderSaved(gender: Gender?) {
        _uiState.value = _uiState.value.copy(gender = gender)
        saveProfileToDatabase()
    }

    fun onFitnessGoalSaved(goal: FitnessGoal?) {
        _uiState.value = _uiState.value.copy(fitnessGoal = goal)
        saveProfileToDatabase()
    }

    fun onActivityLevelSaved(level: ActivityLevel?) {
        _uiState.value = _uiState.value.copy(activityLevel = level)
        saveProfileToDatabase()
    }

    fun onPreferredUnitsSaved(units: PreferredUnits) {
        viewModelScope.launch {
            appPreferencesRepository.setPreferredUnits(units)
        }
    }

    private fun saveProfileToDatabase() {
        viewModelScope.launch {
            uid?.let { userId ->
                val currentState = _uiState.value

                // Check if profile exists in database first, so joinedDate
                // isn't clobbered on every field save.
                val existingProfile = repository.getProfile(userId)

                val profile = UserProfile(
                    uid = userId,
                    name = currentState.name.ifBlank { "User" },
                    email = currentState.email,
                    profileImageUrl = currentState.profileImageUri,
                    birthDate = currentState.birthDate,
                    weight = currentState.weight,
                    height = currentState.height,
                    targetWeight = currentState.targetWeight,
                    gender = currentState.gender,
                    fitnessGoal = currentState.fitnessGoal,
                    activityLevel = currentState.activityLevel,
                    joinedDate = existingProfile?.joinedDate ?: System.currentTimeMillis(),
                    bio = currentState.bio.ifBlank { null }
                )
                repository.saveProfile(profile)
                savedProfileState = _uiState.value
            }
        }
    }
}
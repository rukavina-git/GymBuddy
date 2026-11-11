package com.rukavina.gymbuddy.ui.you.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.Gender
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.UserProfile
import com.rukavina.gymbuddy.domain.repository.BodyWeightRepository
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.utils.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Who is signed in, for the email line under Name (R15-3). Open for tests. */
open class SignInInfo @Inject constructor() {
    data class Info(val email: String, val viaGoogle: Boolean)

    open fun read(): Info {
        val user = FirebaseAuth.getInstance().currentUser
        val google = user?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
        return Info(user?.email.orEmpty(), google)
    }
}

/** Profile page state (R15-3). Weights kg, height cm (metric storage). */
data class ProfileUiState(
    val loading: Boolean = true,
    val isGuest: Boolean = true,
    val name: String = "",
    val email: String = "",
    val viaGoogle: Boolean = false,
    val avatarId: String? = null,
    /** Newest body weight entry, else the profile's weight. */
    val bodyWeightKg: Float? = null,
    val experience: ExperienceLevel? = null,
    val heightCm: Float? = null,
    val birthDate: Long? = null,
    val gender: Gender? = null,
    val activity: ActivityLevel? = null,
    val goal: FitnessGoal? = null,
    val targetWeightKg: Float? = null,
    val units: PreferredUnits = PreferredUnits.METRIC,
)

/**
 * Profile (7.10b). Every edit is one read-modify-write of the stored row
 * ([UserProfileRepository.updateProfile]), so fields this page doesn't
 * show (consent, plan, training days) survive. Null clears a field.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: UserProfileRepository,
    bodyWeight: BodyWeightRepository,
    private val preferences: AppPreferencesRepository,
    signIn: SignInInfo,
    private val clock: Clock,
) : ViewModel() {

    private val account = signIn.read()

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.observeProfile().catch { emit(null) },
        bodyWeight.observeEntries().catch { emit(emptyList()) },
        preferences.preferredUnits,
    ) { p, entries, units ->
        ProfileUiState(
            loading = false,
            isGuest = repository.isGuest,
            name = p?.name.orEmpty(),
            email = p?.email?.takeIf { it.isNotBlank() } ?: account.email,
            viaGoogle = account.viaGoogle,
            avatarId = p?.avatarId,
            bodyWeightKg = entries.firstOrNull()?.weightKg ?: p?.weight,
            experience = p?.experienceLevel,
            heightCm = p?.height,
            birthDate = p?.birthDate,
            gender = p?.gender,
            activity = p?.activityLevel,
            goal = p?.fitnessGoal,
            targetWeightKg = p?.targetWeight,
            units = units,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState())

    /** Blank names are ignored (the letter avatar needs one). */
    fun setName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_NAME) return
        edit { it.copy(name = trimmed) }
    }

    /** Null = the letter. Unknown ids are ignored. */
    fun setAvatar(id: String?) {
        if (id != null && com.rukavina.gymbuddy.ui.you.Avatars.of(id) == null) return
        edit { it.copy(avatarId = id) }
    }

    fun setExperience(level: ExperienceLevel?) = edit { it.copy(experienceLevel = level) }
    fun setBirthDate(millis: Long?) = edit { it.copy(birthDate = millis) }
    fun setGender(gender: Gender?) = edit { it.copy(gender = gender) }
    fun setActivity(level: ActivityLevel?) = edit { it.copy(activityLevel = level) }
    fun setGoal(goal: FitnessGoal?) = edit { it.copy(fitnessGoal = goal) }

    /** Keypad text in the display unit; empty clears. False (nothing saved) when out of range. */
    fun setHeight(text: String): Boolean {
        val cm = parse(text) { UnitConverter.heightToMetric(it, uiState.value.units) } ?: return false
        if (cm.value != null && cm.value !in HEIGHT_CM) return false
        edit { it.copy(height = cm.value) }
        return true
    }

    /** Keypad text in the display unit; empty clears. False (nothing saved) when out of range. */
    fun setTargetWeight(text: String): Boolean {
        val kg = parse(text) { UnitConverter.weightToMetric(it, uiState.value.units) } ?: return false
        if (kg.value != null && kg.value !in WEIGHT_KG) return false
        edit { it.copy(targetWeight = kg.value) }
        return true
    }

    /** Empty → Box(null) (clear); unparsable → null (reject). */
    private fun parse(text: String, toMetric: (String) -> Float?): Box? {
        if (text.isBlank()) return Box(null)
        val number = KeypadValue.parse(text) ?: return null
        return toMetric(number.toString())?.let { Box(it) }
    }

    private class Box(val value: Float?)

    private fun edit(change: (UserProfile) -> UserProfile) {
        val uid = repository.ownerUid
        viewModelScope.launch {
            repository.updateProfile(
                uid,
                create = { UserProfile(uid = uid, name = "", email = account.email, joinedDate = clock.millis()) },
                change = change,
            )
        }
    }

    companion object {
        const val MAX_NAME = 40
        val HEIGHT_CM = 50f..272f
        val WEIGHT_KG = 20f..700f
    }
}

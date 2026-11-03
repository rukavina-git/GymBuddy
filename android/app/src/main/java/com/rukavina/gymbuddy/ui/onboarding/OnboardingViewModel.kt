package com.rukavina.gymbuddy.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.StarterPlanRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.StarterPlan
import com.rukavina.gymbuddy.domain.model.TrainingDays
import com.rukavina.gymbuddy.domain.model.UserProfile
import com.rukavina.gymbuddy.domain.usecase.template.StarterPlans
import com.rukavina.gymbuddy.ui.theme.Durations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

/** Setup after "Get started" (owner decision 4A): experience → goal → days → plan → ready. */
enum class SetupStep { EXPERIENCE, GOAL, DAYS, PLAN, READY }

/** One answer slot. [answered] tells "Not sure yet" / "Start with no plan" (null value) from no answer. */
data class Answer<T>(val value: T? = null, val answered: Boolean = false)

data class OnboardingUiState(
    val step: SetupStep = SetupStep.EXPERIENCE,
    val experience: Answer<ExperienceLevel> = Answer(),
    val goal: Answer<FitnessGoal> = Answer(),
    val days: Answer<TrainingDays> = Answer(),
    /** Plan id; answered with null = "Start with no plan". */
    val plan: Answer<String> = Answer(),
    val plans: List<StarterPlan> = emptyList(),
    val saving: Boolean = false,
) {
    /** From days a week, or experience for "Not sure yet" (StarterPlans.suggestPlan). */
    val suggested: StarterPlan? get() = StarterPlans.suggestPlan(plans, days.value, experience.value)

    /** What Let's go saves: the chosen plan, or the suggestion when the step was skipped. */
    val finalPlanId: String? get() = if (plan.answered) plan.value else suggested?.id
}

sealed interface OnboardingEvent {
    data object Done : OnboardingEvent
}

/**
 * Tap = choose and go on (Figma 114:20520): the tapped row shows cobalt
 * for a moment, then the next question. Back keeps the answer. Skip
 * jumps to Ready with defaults. Nothing is saved before Let's go.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val profiles: UserProfileRepository,
    private val plans: StarterPlanRepository,
    private val preferences: AppPreferencesRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    private var advance: Job? = null

    init {
        viewModelScope.launch {
            val live = runCatching { plans.getPlans() }.getOrDefault(emptyList()).filterNot { it.deprecated }
            _state.update { it.copy(plans = live) }
        }
    }

    fun chooseExperience(level: ExperienceLevel) = choose(SetupStep.EXPERIENCE) { it.copy(experience = Answer(level, true)) }
    fun chooseGoal(goal: FitnessGoal) = choose(SetupStep.GOAL) { it.copy(goal = Answer(goal, true)) }
    fun chooseDays(days: TrainingDays?) = choose(SetupStep.DAYS) { it.copy(days = Answer(days, true)) }
    fun choosePlan(planId: String?) = choose(SetupStep.PLAN) { it.copy(plan = Answer(planId, true)) }

    fun skip() {
        advance?.cancel()
        set { it.copy(step = SetupStep.READY) }
    }

    /** System or top-bar back. False on the first question: the caller leaves setup. */
    fun back(): Boolean {
        advance?.cancel()
        val step = _state.value.step
        if (step == SetupStep.EXPERIENCE) return false
        set { it.copy(step = SetupStep.entries[step.ordinal - 1]) }
        return true
    }

    /** Let's go: the answers land on the guest's profile, and the app opens as a guest. */
    fun finish() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val uid = profiles.ownerUid
            val existing = profiles.getProfile(uid)
            val base = existing ?: UserProfile(uid = uid, name = "", email = "", joinedDate = clock.millis())
            profiles.saveProfile(
                base.copy(
                    experienceLevel = s.experience.value ?: base.experienceLevel,
                    fitnessGoal = s.goal.value ?: base.fitnessGoal,
                    trainingDays = s.days.value ?: base.trainingDays,
                    activePlanId = s.finalPlanId,
                )
            )
            preferences.setGuest(true)
            _events.emit(OnboardingEvent.Done)
        }
    }

    private fun choose(at: SetupStep, answer: (OnboardingUiState) -> OnboardingUiState) {
        if (_state.value.step != at) return
        set(answer)
        advance?.cancel()
        advance = viewModelScope.launch {
            delay(PRESS_HOLD_MS)
            set { if (it.step == at) it.copy(step = SetupStep.entries[at.ordinal + 1]) else it }
        }
    }

    private fun set(change: (OnboardingUiState) -> OnboardingUiState) {
        _state.update(change)
        save(_state.value)
    }

    // Answers survive process death; plans are reloaded.
    private fun save(s: OnboardingUiState) {
        savedState[KEY_STEP] = s.step.name
        savedState[KEY_EXPERIENCE] = s.experience.encode()
        savedState[KEY_GOAL] = s.goal.encode()
        savedState[KEY_DAYS] = s.days.encode()
        savedState[KEY_PLAN] = s.plan.answered.let { if (it) s.plan.value ?: NONE else null }
    }

    private fun restore(): OnboardingUiState {
        val step = savedState.get<String>(KEY_STEP)?.let { runCatching { SetupStep.valueOf(it) }.getOrNull() }
            ?: return OnboardingUiState()
        val plan = savedState.get<String>(KEY_PLAN)
        return OnboardingUiState(
            step = step,
            experience = decode(savedState[KEY_EXPERIENCE]) { ExperienceLevel.valueOf(it) },
            goal = decode(savedState[KEY_GOAL]) { FitnessGoal.valueOf(it) },
            days = decode(savedState[KEY_DAYS]) { TrainingDays.valueOf(it) },
            plan = if (plan == null) Answer() else Answer(plan.takeUnless { it == NONE }, true),
        )
    }

    private fun <T : Enum<T>> Answer<T>.encode(): String? = if (answered) value?.name ?: NONE else null

    private fun <T> decode(raw: String?, parse: (String) -> T): Answer<T> = when (raw) {
        null -> Answer()
        NONE -> Answer(null, true)
        else -> runCatching { Answer(parse(raw), true) }.getOrDefault(Answer())
    }

    companion object {
        /** How long the tapped row stays cobalt before the next question (motion short3). */
        const val PRESS_HOLD_MS = Durations.SHORT3.toLong()
        private const val NONE = "-"
        private const val KEY_STEP = "setup_step"
        private const val KEY_EXPERIENCE = "setup_experience"
        private const val KEY_GOAL = "setup_goal"
        private const val KEY_DAYS = "setup_days"
        private const val KEY_PLAN = "setup_plan"
    }
}

package com.rukavina.gymbuddy.ui.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.sync.AuthSession
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import javax.inject.Inject

/**
 * Setup: experience → goal → days → plan → ready. Guests get the account
 * offer (create-account route, "step 5") between plan and ready
 * (amendment 1); it is a separate screen, not a [SetupStep].
 */
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
    /** An account is signed in: no account step, signed-in tip on Ready. */
    val signedIn: Boolean = false,
) {
    /** Progress bar length: four questions, plus the account step for guests. */
    val totalSteps: Int get() = if (signedIn) SETUP_QUESTIONS else SETUP_STEPS_WITH_ACCOUNT

    /** From days a week, or experience for "Not sure yet" (StarterPlans.suggestPlan). */
    val suggested: StarterPlan? get() = StarterPlans.suggestPlan(plans, days.value, experience.value)

    /** What Let's go saves: the chosen plan, or the suggestion when the step was skipped. */
    val finalPlanId: String? get() = if (plan.answered) plan.value else suggested?.id
}

sealed interface OnboardingEvent {
    data object Done : OnboardingEvent

    /** Questions done as a guest: open the create-account step. */
    data object OpenAccountStep : OnboardingEvent
}

const val SETUP_QUESTIONS = 4
const val SETUP_STEPS_WITH_ACCOUNT = 5

/**
 * Tap = choose and go on (Figma 114:20520): the tapped row shows cobalt
 * for a moment, then the next question. Back keeps the answer. Skip
 * ends the questions with defaults. Nothing is saved before Let's go.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val profiles: UserProfileRepository,
    private val plans: StarterPlanRepository,
    private val preferences: AppPreferencesRepository,
    private val clock: Clock,
    private val auth: AuthSession,
    private val holder: DatabaseHolder,
) : ViewModel() {

    private val _state = MutableStateFlow(restore().copy(signedIn = auth.currentUid != null))
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>(extraBufferCapacity = 2)
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

    /** Skip the remaining questions (defaults); guests still get the account offer. */
    fun skip() {
        advance?.cancel()
        questionsDone()
    }

    /** Back from the account step: created an account, or chose "Not now". */
    fun onAccountStep(created: Boolean) {
        set { it.copy(step = SetupStep.READY, signedIn = created || auth.currentUid != null) }
    }

    private fun questionsDone() {
        val signedIn = auth.currentUid != null
        _state.update { it.copy(signedIn = signedIn) }
        if (signedIn) set { it.copy(step = SetupStep.READY) } else _events.tryEmit(OnboardingEvent.OpenAccountStep)
    }

    /** System or top-bar back. False on the first question: the caller leaves setup. */
    fun back(): Boolean {
        advance?.cancel()
        val step = _state.value.step
        if (step == SetupStep.EXPERIENCE) return false
        set { it.copy(step = SetupStep.entries[step.ordinal - 1]) }
        return true
    }

    /**
     * Let's go: the answers land on the profile of whoever owns the data
     * (the new account, or the guest). After a sign-in, waits until
     * AuthGate opened the account's file, so nothing lands in the guest's.
     */
    fun finish() {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val account = auth.currentUid
                if (account != null) {
                    val open = withTimeoutOrNull(OWNER_WAIT_MS) { holder.ownerFlow.first { it == account } }
                    // The account's file never opened: don't put the answers in the guest's.
                    if (open == null) return@launch
                }
                val uid = profiles.ownerUid
                profiles.updateProfile(uid, { UserProfile(uid = uid, name = "", email = "", joinedDate = clock.millis()) }) { base ->
                    base.copy(
                        experienceLevel = s.experience.value ?: base.experienceLevel,
                        fitnessGoal = s.goal.value ?: base.fitnessGoal,
                        trainingDays = s.days.value ?: base.trainingDays,
                        activePlanId = s.finalPlanId,
                    )
                }
                if (account == null) preferences.setGuest(true)
                _events.emit(OnboardingEvent.Done)
            } finally {
                // A failed save leaves Let's go usable for another try.
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private fun choose(at: SetupStep, answer: (OnboardingUiState) -> OnboardingUiState) {
        if (_state.value.step != at) return
        set(answer)
        advance?.cancel()
        advance = viewModelScope.launch {
            delay(PRESS_HOLD_MS)
            when {
                _state.value.step != at -> Unit
                at == SetupStep.PLAN -> questionsDone()
                else -> set { it.copy(step = SetupStep.entries[at.ordinal + 1]) }
            }
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
        private const val OWNER_WAIT_MS = 5_000L
        private const val NONE = "-"
        private const val KEY_STEP = "setup_step"
        private const val KEY_EXPERIENCE = "setup_experience"
        private const val KEY_GOAL = "setup_goal"
        private const val KEY_DAYS = "setup_days"
        private const val KEY_PLAN = "setup_plan"
    }
}

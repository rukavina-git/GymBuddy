package com.rukavina.gymbuddy.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.sync.DataOwnerStore
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.ActiveWorkoutDraft
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.usecase.template.CreateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.domain.repository.ActiveWorkoutDraftRepository
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesIncludingHiddenUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.CreateWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.LastPerformance
import com.rukavina.gymbuddy.domain.usecase.workout.StaleWorkout
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.ui.template.editor.TemplateEditorViewModel
import com.rukavina.gymbuddy.ui.workout.service.RestBuzzScheduler
import com.rukavina.gymbuddy.ui.workout.service.WorkoutServiceStarter
import com.rukavina.gymbuddy.ui.workout.service.buzzTarget
import com.rukavina.gymbuddy.ui.workout.service.counterSeconds
import com.rukavina.gymbuddy.ui.workout.service.toNotice
import com.rukavina.gymbuddy.utils.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Data class representing a single set within an exercise during an active workout.
 * Values are the raw text in display units; [duration] is seconds.
 */
data class WorkoutSet(
    val id: String,
    val setNumber: Int,
    val reps: String = "",
    val weight: String = "",
    val duration: String = "",
    val distance: String = "",
    val notes: String = "",
    val isCompleted: Boolean = false,
    /** When Done was tapped, epoch ms (11m); null until done. */
    val doneAt: Long? = null,
    /** The workout clock (seconds) at Done (11m). */
    val doneAtElapsed: Long? = null,
)

/**
 * Data class representing an exercise with multiple sets during an active workout.
 */
data class ActiveExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseTrackingType: ExerciseTrackingType,
    val sets: List<WorkoutSet>,
    val plannedSets: Int,
    val plannedReps: Int?,
    val restSeconds: Int?,
    /** The template's note for this exercise; the line shows it first (R13-13). */
    val templateNote: String? = null,
) {
    /** The set the card shows: first not done, null when all are. */
    val currentSet: WorkoutSet? get() = sets.firstOrNull { !it.isCompleted }
    val allDone: Boolean get() = sets.none { !it.isCompleted }
}

/** Rest after [afterSetId], on the workout clock (see [ActiveWorkoutDraft.Rest]). */
data class RestState(val afterSetId: String, val startedAtElapsed: Long, val seconds: Int)

/** The keypad sheet on one value of the current set. [text] is the keypad's own text. */
data class WorkoutKeypad(val exerciseId: String, val setId: String, val field: SetField, val text: String) {
    /** The rule the typed text breaks, shown under the value (Figma 53:2873). */
    val rule: SetRule? get() = SetFields.rule(this.field, text)
}

/** Finish dialog numbers (Figma 53:3493 / 53:3594). Lifted is in display units. */
/** A workout left 24 h+ that was finished at its last set on open (F25). */
data class AutoFinished(val title: String, val endedAt: Long)

data class FinishPreview(val minutes: Long, val sets: Int, val lifted: Float, val setsLeft: Int)

/** One "What you did" row: the heaviest (then longest/most) set. */
data class SummaryExercise(
    val name: String,
    val setCount: Int,
    val trackingType: ExerciseTrackingType,
    val top: WorkoutSet,
)

/** The summary screen (Figma 53:3697). */
data class WorkoutSummary(
    val title: String,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val sets: Int,
    val lifted: Float,
    val units: PreferredUnits,
    val exercises: List<SummaryExercise>,
    val offerBackup: Boolean,
    /** F24b: what "Save as template" stores; null after a template workout. */
    val template: WorkoutTemplate? = null,
    /** Set once the template is saved; the summary then offers Edit. */
    val savedTemplateId: String? = null,
    /** The last Save as template failed; the button stays to retry. */
    val templateFailed: Boolean = false,
)

enum class WorkoutDialog { Finish, Discard }

/**
 * UI state for the active workout screen.
 */
data class ActiveWorkoutUiState(
    val workoutTitle: String = "",
    val workoutStartTime: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val isTimerRunning: Boolean = true,
    val exercises: List<ActiveExercise> = emptyList(),
    val preferredUnits: PreferredUnits = PreferredUnits.METRIC,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val workoutSaved: Boolean = false,
    val workoutDiscarded: Boolean = false,
    val templateId: String? = null,
    val templateTitle: String? = null,
    val currentExerciseIndex: Int = 0,
    val rest: RestState? = null,
    val keypad: WorkoutKeypad? = null,
    val dialog: WorkoutDialog? = null,
    val summary: WorkoutSummary? = null,
    /** First equipment per exercise id, for the "Barbell · 4 × 8" line. */
    val equipment: Map<String, Equipment> = emptyMap(),
    /** "Tap a number to type" in the user's first workouts (decision 5). */
    val showHint: Boolean = false,
    /** Workout note (⋯ → Workout note), saved as the session's notes. */
    val note: String = "",
    /** Buzz when rest ends (06c), a device preference. */
    val restBuzz: Boolean = false,
    /** Your notes by exercise id (one-note line, R13-13/14). */
    val userNotes: Map<String, String> = emptyMap(),
    /** The last draft write failed (R17-B1); cleared by the next good write. */
    val unsaved: UnsavedSet? = null,
    /**
     * Stale (R17-E2, F25): no set or resume for 2 h. Paused; Home and the
     * workout ask Resume / Finish at last set (Discard with no sets).
     * The [lastActivity] it went stale at, null otherwise.
     */
    val unfinishedSince: Long? = null,
    /** Last Resume of a stale workout (F25), epoch ms. */
    val resumedAt: Long? = null,
    /** Set when an expired workout was auto-finished; Main shows a snackbar. */
    val autoFinished: AutoFinished? = null,
) {
    /**
     * A workout runs: started (an empty workout has no exercises yet) and
     * not saved or discarded. The one "is a workout running" source.
     */
    val running: Boolean get() = (exercises.isNotEmpty() || workoutStartTime > 0L) && !workoutSaved && !workoutDiscarded
    val currentExercise: ActiveExercise? get() = exercises.getOrNull(currentExerciseIndex)
    val doneSets: Int get() = exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    val setsLeft: Int get() = exercises.sumOf { ex -> ex.sets.count { !it.isCompleted } }
    /** Latest done set, resume or start, epoch ms (F25). */
    val lastActivity: Long get() = StaleWorkout.lastActivity(
        workoutStartTime,
        exercises.flatMap { ex -> ex.sets.mapNotNull { it.doneAt } },
        resumedAt,
    )
    /** "Finish at last set": the latest done set's time and clock (F25). */
    val endAtLastSet: StaleWorkout.End? get() = StaleWorkout.endAtLastSet(
        exercises.flatMap { ex -> ex.sets.mapNotNull { s -> s.doneAt?.let { it to (s.doneAtElapsed ?: 0L) } } }
    )
    /** The next set to log is the last one left in the workout: Done finishes. */
    val lastSetLeft: Boolean get() = setsLeft == 1 && currentExercise?.currentSet != null
}

/** A draft write that failed; [setNumber] is the last set marked done, if any. */
data class UnsavedSet(val setNumber: Int?)

/** One-off things the screen does: haptics, snackbars. */
sealed interface WorkoutEvent {
    data object SetDone : WorkoutEvent
    data object Rejected : WorkoutEvent

    /** Done on a set that breaks [rule] (F22: say why, not just buzz). */
    data class DoneRejected(val rule: SetRule) : WorkoutEvent
    /** A draft write failed (R17-B1): haptic only; the banner is state. */
    data object DraftSaveFailed : WorkoutEvent
    data class SetDeleted(val setId: String) : WorkoutEvent
    data class SaveFailed(val message: String) : WorkoutEvent
}

/**
 * ViewModel for managing an active workout session (Plan 7 06a): the set
 * card with − / + and keypad, Done with rest, delete with Undo, finish and
 * summary.
 *
 * Every change is written to a local draft (Room), so the workout
 * survives process death, swipe-away and reboot. A fresh ViewModel
 * restores it; a save or discard deletes it.
 */
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val createWorkoutSessionUseCase: CreateWorkoutSessionUseCase,
    private val getAllExercisesIncludingHiddenUseCase: GetAllExercisesIncludingHiddenUseCase,
    private val appPreferencesRepository: AppPreferencesRepository,
    private val idGenerator: IdGenerator,
    private val draftRepository: ActiveWorkoutDraftRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val owners: DataOwnerStore,
    private val clock: Clock,
    private val workoutService: WorkoutServiceStarter,
    private val restBuzz: RestBuzzScheduler,
    private val exerciseRepository: ExerciseRepository,
    private val createTemplate: CreateWorkoutTemplateUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveWorkoutUiState())
    val uiState: StateFlow<ActiveWorkoutUiState> = _uiState.asStateFlow()

    private val _events = Channel<WorkoutEvent>(Channel.BUFFERED)
    val events: Flow<WorkoutEvent> = _events.receiveAsFlow()

    private var timerJob: Job? = null
    private var availableExercises: List<Exercise> = emptyList()

    /** The last deleted set, for Undo. */
    private data class Deleted(val exerciseId: String, val index: Int, val set: WorkoutSet, val rest: RestState?)
    private var deleted: Deleted? = null

    /** A pending draft write. Conflated: only the newest one matters. */
    private sealed interface DraftWrite {
        data class Save(val draft: ActiveWorkoutDraft) : DraftWrite
        data object Clear : DraftWrite
    }

    /** Set number of the last Done, for "Set N isn't saved yet". */
    private var lastDoneSetNumber: Int? = null

    private val draftWrites = MutableSharedFlow<DraftWrite>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        startDraftWriter()
        loadExercises()
        loadUserPreferences()
        restoreDraft()
        followService()
        followRestBuzz()
        followUserNotes()
    }

    /**
     * The workout notification follows the state (06c). Ticks don't count:
     * the notice holds no clock, the system chronometer counts from the
     * base worked out when something else changes. Leading nulls are
     * skipped, so a new ViewModel doesn't stop the service a restored
     * draft is about to keep.
     */
    private fun followService() {
        viewModelScope.launch {
            _uiState.map { it.toNotice() }
                .distinctUntilChanged()
                .dropWhile { it == null }
                .collect { notice ->
                    val base = notice?.takeIf { !it.paused }?.let {
                        clock.millis() - _uiState.value.counterSeconds() * 1000
                    }
                    workoutService.update(notice?.copy(counterBase = base))
                }
        }
    }

    /**
     * Rest buzz (06c): scheduled for the rest that runs, if the buzz is on.
     * A new rest, a changed length, undo, pause, finish or discard changes
     * (or clears) the target; a rest already over doesn't buzz.
     */
    private fun followRestBuzz() {
        viewModelScope.launch {
            _uiState.map { it.buzzTarget() }
                .distinctUntilChanged()
                .collect { target ->
                    val left = target?.let { it.seconds - (_uiState.value.elapsedSeconds - it.startedAtElapsed) }
                    if (left != null && left > 0) restBuzz.schedule(left) else restBuzz.cancel()
                }
        }
    }

    /** Rest timer sheet switch "Buzz when rest ends". */
    fun setRestBuzz(on: Boolean) {
        // The state follows the preference's flow (loadUserPreferences).
        viewModelScope.launch { appPreferencesRepository.setRestBuzz(on) }
    }

    /**
     * The notification permission is asked once, on the first workout
     * start; [ask] runs only the first time.
     */
    fun askNotificationsOnce(ask: () -> Unit) {
        viewModelScope.launch {
            if (appPreferencesRepository.notificationsAsked.first()) return@launch
            appPreferencesRepository.setNotificationsAsked()
            ask()
        }
    }

    /** One sequential writer, so writes land in the order they were made. */
    private fun startDraftWriter() {
        viewModelScope.launch {
            draftWrites.collect { write ->
                try {
                    when (write) {
                        is DraftWrite.Save -> draftRepository.save(write.draft)
                        DraftWrite.Clear -> draftRepository.clear()
                    }
                    if (_uiState.value.unsaved != null) _uiState.update { it.copy(unsaved = null) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Disk full or broken: keep the numbers on screen, say so (R17-B1).
                    if (write is DraftWrite.Save) {
                        _uiState.update { it.copy(unsaved = UnsavedSet(lastDoneSetNumber)) }
                        _events.trySend(WorkoutEvent.DraftSaveFailed)
                    }
                }
            }
        }
    }

    /**
     * Resumes a workout the process lost. Skipped if a workout was
     * already started or ended in this ViewModel before the read finished.
     * F25: no set for 2 h → paused as unfinished; for 24 h → finished at
     * the last set (dropped if it has none).
     */
    private fun restoreDraft() {
        viewModelScope.launch {
            val draft = draftRepository.load() ?: return@launch
            val current = _uiState.value
            if (current.running || current.workoutSaved || current.workoutDiscarded) {
                return@launch
            }
            // Drafts from before 11m have no set times: their done sets count as done at the last write.
            val exercises = draft.exercises.map { it.toActiveExercise(draft.savedAt, draft.elapsedSeconds) }
            val restored = current.copy(
                workoutTitle = draft.title,
                workoutStartTime = draft.startedAt,
                elapsedSeconds = draft.elapsedSeconds,
                isTimerRunning = false,
                exercises = exercises,
                templateId = draft.templateId,
                templateTitle = draft.templateTitle,
                currentExerciseIndex = draft.currentExerciseIndex.coerceIn(0, (exercises.size - 1).coerceAtLeast(0)),
                rest = draft.rest?.let { RestState(it.afterSetId, it.startedAtElapsed, it.seconds) },
                note = draft.note,
                resumedAt = draft.resumedAt,
            )
            val now = clock.millis()
            when (StaleWorkout.status(restored.lastActivity, now)) {
                StaleWorkout.Status.Expired -> {
                    _uiState.value = restored
                    autoFinish()
                }
                StaleWorkout.Status.Stale -> _uiState.value = restored.copy(rest = null, unfinishedSince = restored.lastActivity)
                StaleWorkout.Status.Active -> {
                    // A running timer kept counting while the app was gone.
                    val away = if (draft.isTimerRunning) ((now - draft.savedAt) / 1000).coerceAtLeast(0) else 0L
                    _uiState.value = restored.copy(elapsedSeconds = draft.elapsedSeconds + away, isTimerRunning = draft.isTimerRunning)
                    if (draft.isTimerRunning) startTimer()
                }
            }
        }
    }

    /**
     * F25: called every timer tick and when the app comes to the
     * foreground. 2 h without a set (or resume) pauses the workout as
     * unfinished; 24 h finishes it at the last set.
     */
    fun checkStale() {
        val state = _uiState.value
        if (!state.running || state.isLoading) return
        when (StaleWorkout.status(state.lastActivity, clock.millis())) {
            StaleWorkout.Status.Active -> Unit
            StaleWorkout.Status.Stale -> if (state.unfinishedSince == null) {
                timerJob?.cancel()
                _uiState.update {
                    it.copy(unfinishedSince = it.lastActivity, isTimerRunning = false, rest = null, keypad = null, dialog = null)
                }
                persistDraft()
            }
            StaleWorkout.Status.Expired -> autoFinish()
        }
    }

    /** Finish at the last set without asking, or drop a workout with no sets (F25). */
    private fun autoFinish() {
        val end = _uiState.value.endAtLastSet
        if (end == null) {
            discardWorkout()
        } else {
            saveWorkout(endedAt = end.endedAt, durationSeconds = end.durationSeconds, auto = true)
        }
    }

    /** Main showed the auto-finish snackbar. */
    fun autoFinishShown() = _uiState.update { it.copy(autoFinished = null) }

    private fun persistDraft() {
        val state = _uiState.value
        if (!state.running) return
        draftWrites.tryEmit(DraftWrite.Save(state.toDraft(clock.millis())))
    }

    /** "Try again · set N" (R17-B1): write the draft again. */
    fun retrySave() = persistDraft()

    /**
     * Unfinished workout → Resume (R17-E2): the timer picks up where it
     * stopped. F25: asks again 2 h after this (or the next set).
     */
    fun resumeUnfinished() {
        if (_uiState.value.unfinishedSince == null) return
        _uiState.update { it.copy(unfinishedSince = null, isTimerRunning = true, resumedAt = clock.millis()) }
        startTimer()
        persistDraft()
    }

    /** Unfinished workout → Finish (F25): saves it ending at the last done set. */
    fun finishUnfinished() {
        val state = _uiState.value
        if (state.unfinishedSince == null) return
        val end = state.endAtLastSet ?: return
        saveWorkout(endedAt = end.endedAt, durationSeconds = end.durationSeconds)
    }

    private fun clearDraft() {
        draftWrites.tryEmit(DraftWrite.Clear)
    }

    /**
     * Load the user's preferred display units - a device preference, not
     * account data.
     */
    private fun loadUserPreferences() {
        viewModelScope.launch {
            appPreferencesRepository.preferredUnits.collect { units ->
                _uiState.update { state -> state.copy(preferredUnits = units) }
            }
        }
        viewModelScope.launch {
            appPreferencesRepository.restBuzz.collect { on -> _uiState.update { it.copy(restBuzz = on) } }
        }
    }

    /**
     * Load available exercises for name mapping and the equipment line.
     * Includes hidden exercises so a template's hidden ones still resolve.
     */
    private fun loadExercises() {
        viewModelScope.launch {
            setExercises(getAllExercisesIncludingHiddenUseCase().first())
            val finished = sessionRepository.getAllWorkoutSessions().first().count { it.deletedAt == null }
            _uiState.update { it.copy(showHint = finished < HINT_WORKOUTS) }
        }
    }

    private fun setExercises(all: List<Exercise>) {
        availableExercises = all
        val equipment = all.mapNotNull { ex -> ex.equipmentNeeded.firstOrNull()?.let { ex.id to it } }.toMap()
        _uiState.update { it.copy(equipment = equipment) }
    }

    /**
     * Start a new workout from a template. Sets start from the last
     * session's numbers (F18), else the template's targets, so − / + and
     * Done work right away.
     */
    fun startWorkoutFromTemplate(template: WorkoutTemplate) {
        viewModelScope.launch {
            // Ensure exercises are loaded before starting
            if (availableExercises.isEmpty()) {
                setExercises(getAllExercisesIncludingHiddenUseCase().first())
            }

            val exerciseMap = availableExercises.associateBy { it.id }
            val workoutId = clock.millis()
            val units = _uiState.value.preferredUnits
            val past = pastSessions()

            val activeExercises = template.templateExercises
                .sortedBy { it.orderIndex }
                .mapIndexed { exerciseIndex, templateExercise ->
                    // A deleted exercise still starts with its template (R13-12): use the snapshot.
                    val exerciseName = exerciseMap[templateExercise.exerciseId]?.name
                        ?: templateExercise.exerciseName.ifBlank { "Unknown Exercise" }
                    val exerciseTrackingType = exerciseMap[templateExercise.exerciseId]?.trackingType
                        ?: templateExercise.exerciseTrackingType
                    val fields = SetFields.of(exerciseTrackingType)
                    val last = LastPerformance.prefill(
                        LastPerformance.lastSets(past, templateExercise.exerciseId),
                        templateExercise.plannedSets,
                    )
                    val sets = (1..templateExercise.plannedSets).mapIndexed { setIndex, setNumber ->
                        WorkoutSet(
                            id = "${workoutId}_ex${exerciseIndex}_set${setIndex}",
                            setNumber = setNumber,
                            reps = if (SetField.Reps in fields) templateExercise.plannedReps?.toString().orEmpty() else "",
                            weight = if (SetField.Weight in fields) {
                                templateExercise.plannedWeightKg?.let { displayWeight(it, units) }.orEmpty()
                            } else "",
                            duration = if (SetField.Duration in fields) {
                                templateExercise.plannedDurationSeconds?.toString().orEmpty()
                            } else "",
                            distance = if (SetField.Distance in fields) {
                                templateExercise.plannedDistanceMeters?.let(SetFields::format).orEmpty()
                            } else "",
                        ).prefilledFrom(last[setIndex], fields, units)
                    }

                    ActiveExercise(
                        id = "${workoutId}_exercise_${exerciseIndex}",
                        exerciseId = templateExercise.exerciseId,
                        exerciseName = exerciseName,
                        exerciseTrackingType = exerciseTrackingType,
                        sets = sets,
                        plannedSets = templateExercise.plannedSets,
                        plannedReps = templateExercise.plannedReps,
                        restSeconds = templateExercise.restSeconds,
                        templateNote = templateExercise.notes?.trim()?.ifEmpty { null },
                    )
                }

            begin(template.title, activeExercises, template.id)
        }
    }

    /** "+ Empty workout" on the Start sheet: no exercises, add them from ⋯. */
    fun startEmptyWorkout(title: String) = begin(title, emptyList(), null)

    // Reset state completely when starting a new workout, except what the
    // long-lived collectors loaded (units, equipment, hint): they won't
    // re-emit just because a workout started.
    private fun begin(title: String, exercises: List<ActiveExercise>, templateId: String?) {
        val old = _uiState.value
        deleted = null
        lastDoneSetNumber = null
        _uiState.value = ActiveWorkoutUiState(
            workoutTitle = title,
            workoutStartTime = clock.millis(),
            exercises = exercises,
            preferredUnits = old.preferredUnits,
            restBuzz = old.restBuzz,
            equipment = old.equipment,
            showHint = old.showHint,
            isTimerRunning = true,
            templateId = templateId,
            templateTitle = templateId?.let { title },
        )
        startTimer()
        persistDraft()
    }

    /**
     * Start the workout timer.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isTimerRunning) {
                delay(1000)
                _uiState.update { state ->
                    state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                }
                checkStale()
            }
        }
    }

    /**
     * Toggle timer pause/resume.
     */
    fun toggleTimer() {
        val currentState = _uiState.value.isTimerRunning
        _uiState.update { it.copy(isTimerRunning = !currentState) }

        if (!currentState) {
            startTimer()
        } else {
            timerJob?.cancel()
        }
        persistDraft()
    }

    // --- set values ---

    fun updateSetReps(exerciseId: String, setId: String, reps: String) =
        updateSet(exerciseId, setId) { it.copy(reps = reps.filter { c -> c.isDigit() }) }

    fun updateSetWeight(exerciseId: String, setId: String, weight: String) =
        updateSet(exerciseId, setId) { it.copy(weight = decimalText(weight)) }

    fun updateSetDuration(exerciseId: String, setId: String, duration: String) =
        updateSet(exerciseId, setId) { it.copy(duration = duration.filter { c -> c.isDigit() }) }

    fun updateSetDistance(exerciseId: String, setId: String, distance: String) =
        updateSet(exerciseId, setId) { it.copy(distance = decimalText(distance)) }

    fun updateSetNotes(exerciseId: String, setId: String, notes: String) =
        updateSet(exerciseId, setId) { it.copy(notes = notes) }

    private fun decimalText(text: String) = text.replace(',', '.').filter { it.isDigit() || it == '.' }

    private fun updateSet(exerciseId: String, setId: String, change: (WorkoutSet) -> WorkoutSet) {
        _uiState.update { state ->
            state.copy(
                exercises = state.exercises.map { exercise ->
                    if (exercise.id != exerciseId) exercise
                    else exercise.copy(sets = exercise.sets.map { if (it.id == setId) change(it) else it })
                }
            )
        }
        persistDraft()
    }

    /** − / + on the current set's [field]. Rejected at the floor or ceiling. */
    fun step(field: SetField, plus: Boolean) {
        val state = _uiState.value
        val exercise = state.currentExercise ?: return
        val set = exercise.currentSet ?: return
        val next = SetFields.stepped(set, field, plus, state.preferredUnits)
        if (next == null) {
            _events.trySend(WorkoutEvent.Rejected)
            return
        }
        updateSet(exercise.id, set.id) { next }
    }

    // --- keypad ---

    fun openKeypad(field: SetField) {
        val exercise = _uiState.value.currentExercise ?: return
        val set = exercise.currentSet ?: return
        _uiState.update { it.copy(keypad = keypadFor(exercise.id, set, field)) }
    }

    private fun keypadFor(exerciseId: String, set: WorkoutSet, field: SetField): WorkoutKeypad {
        val text = SetFields.text(set, field)
        return WorkoutKeypad(
            exerciseId = exerciseId,
            setId = set.id,
            field = field,
            text = if (field.kind == KeypadValue.Kind.Clock) KeypadValue.clockDigits(text.toIntOrNull()) else text,
        )
    }

    /** The field after the keypad's, on the same set; null on the last one (Save). */
    fun nextField(keypad: WorkoutKeypad): SetField? {
        val exercise = _uiState.value.exercises.firstOrNull { it.id == keypad.exerciseId } ?: return null
        val fields = SetFields.of(exercise.exerciseTrackingType)
        return fields.getOrNull(fields.indexOf(keypad.field) + 1)
    }

    fun keypad(input: KeypadInput, separator: Char = '.') {
        val keypad = _uiState.value.keypad ?: return
        when (input) {
            KeypadInput.Cancel -> _uiState.update { it.copy(keypad = null) }
            KeypadInput.Next -> {
                if (keypad.rule != null) {
                    _events.trySend(WorkoutEvent.Rejected)
                    return
                }
                val stored = when (keypad.field.kind) {
                    KeypadValue.Kind.Clock -> KeypadValue.parseClock(keypad.text)?.toString().orEmpty()
                    KeypadValue.Kind.Whole -> KeypadValue.parseWhole(keypad.text)?.toString().orEmpty()
                    KeypadValue.Kind.Decimal -> KeypadValue.parse(keypad.text)?.let(SetFields::format).orEmpty()
                }
                updateSet(keypad.exerciseId, keypad.setId) { SetFields.with(it, keypad.field, stored) }
                val next = nextField(keypad)
                val set = _uiState.value.exercises.firstOrNull { it.id == keypad.exerciseId }
                    ?.sets?.firstOrNull { it.id == keypad.setId }
                _uiState.update {
                    it.copy(keypad = if (next != null && set != null) keypadFor(keypad.exerciseId, set, next) else null)
                }
            }
            else -> _uiState.update {
                it.copy(keypad = keypad.copy(text = KeypadValue.apply(keypad.text, input, keypad.field.kind, separator)))
            }
        }
    }

    // --- Done, exercises ---

    fun selectExercise(index: Int) {
        if (index !in _uiState.value.exercises.indices) return
        _uiState.update { it.copy(currentExerciseIndex = index, keypad = null) }
        persistDraft()
    }

    /**
     * Done: logs the current set, starts its rest, carries its values to
     * the sets after it, and moves on to the next exercise with sets left
     * when this one is finished. The last set left opens Finish.
     */
    fun done() {
        val state = _uiState.value
        val exercise = state.currentExercise ?: return
        val set = exercise.currentSet
        if (set == null) {
            nextExercise()
            return
        }
        SetFields.rule(exercise.exerciseTrackingType, set)?.let { rule ->
            _events.trySend(WorkoutEvent.DoneRejected(rule))
            return
        }
        val last = state.lastSetLeft
        val doneIndex = exercise.sets.indexOf(set)
        lastDoneSetNumber = doneIndex + 1
        val sets = exercise.sets.mapIndexed { i, s ->
            when {
                i == doneIndex -> s.copy(isCompleted = true, doneAt = clock.millis(), doneAtElapsed = state.elapsedSeconds)
                i > doneIndex && !s.isCompleted -> s.copy(
                    weight = set.weight, reps = set.reps, duration = set.duration, distance = set.distance,
                )
                else -> s
            }
        }
        val updated = exercise.copy(sets = sets)
        val exercises = state.exercises.map { if (it.id == exercise.id) updated else it }
        val rest = RestState(set.id, state.elapsedSeconds, exercise.restSeconds ?: DEFAULT_REST_SECONDS)
        _uiState.update {
            it.copy(
                exercises = exercises,
                rest = rest,
                keypad = null,
                currentExerciseIndex = if (updated.allDone) nextWithSetsLeft(exercises, it.currentExerciseIndex) else it.currentExerciseIndex,
                dialog = if (last) WorkoutDialog.Finish else it.dialog,
            )
        }
        _events.trySend(WorkoutEvent.SetDone)
        persistDraft()
    }

    /** "Up next": the exercise after this one; past the last, Finish. */
    fun nextExercise() {
        val state = _uiState.value
        val next = state.currentExerciseIndex + 1
        if (next < state.exercises.size) selectExercise(next) else requestFinish()
    }

    private fun nextWithSetsLeft(exercises: List<ActiveExercise>, from: Int): Int {
        val order = (from + 1 until exercises.size) + (0 until from)
        return order.firstOrNull { !exercises[it].allDone } ?: from
    }

    // --- around the workout (06b) ---

    /**
     * ⋯ → Add exercise / sheet "+ Add exercise": appends the picked
     * exercises with 3 sets from the last session (F18), else the
     * editor's defaults (10 reps where reps count, no weight), and shows
     * the first one. Unknown ids are skipped.
     */
    fun addExercises(exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        viewModelScope.launch {
            if (availableExercises.isEmpty()) setExercises(getAllExercisesIncludingHiddenUseCase().first())
            val byId = availableExercises.associateBy { it.id }
            val rests = userStates()
            val past = pastSessions()
            val units = _uiState.value.preferredUnits
            val added = exerciseIds.mapNotNull { byId[it] }.map { exercise ->
                val fields = SetFields.of(exercise.trackingType)
                val reps = SetField.Reps in fields
                val last = LastPerformance.prefill(
                    LastPerformance.lastSets(past, exercise.id),
                    TemplateEditorViewModel.DEFAULT_SETS,
                )
                ActiveExercise(
                    id = idGenerator.newId(),
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    exerciseTrackingType = exercise.trackingType,
                    sets = (1..TemplateEditorViewModel.DEFAULT_SETS).map { n ->
                        WorkoutSet(
                            id = idGenerator.newId(),
                            setNumber = n,
                            reps = if (reps) TemplateEditorViewModel.DEFAULT_REPS.toString() else "",
                        ).prefilledFrom(last[n - 1], fields, units)
                    },
                    plannedSets = TemplateEditorViewModel.DEFAULT_SETS,
                    plannedReps = if (reps) TemplateEditorViewModel.DEFAULT_REPS else null,
                    // Your rest for this exercise (exercise page), else the default.
                    restSeconds = rests[exercise.id]?.restSeconds,
                )
            }
            if (added.isEmpty()) return@launch
            _uiState.update { it.copy(exercises = it.exercises + added, currentExerciseIndex = it.exercises.size, keypad = null) }
            persistDraft()
        }
    }

    /** "Add set" under the set list: a new set with the last set's values. */
    fun addSet() {
        val exercise = _uiState.value.currentExercise ?: return
        val last = exercise.sets.lastOrNull()
        val set = WorkoutSet(
            id = idGenerator.newId(),
            setNumber = exercise.sets.size + 1,
            reps = last?.reps.orEmpty(),
            weight = last?.weight.orEmpty(),
            duration = last?.duration.orEmpty(),
            distance = last?.distance.orEmpty(),
        )
        _uiState.update { s ->
            s.copy(exercises = s.exercises.map { if (it.id == exercise.id) it.copy(sets = it.sets + set) else it })
        }
        persistDraft()
    }

    /** Reorder: Move up (-1) / Move down (+1). The shown exercise stays shown. */
    fun moveExercise(exerciseId: String, delta: Int) {
        val state = _uiState.value
        val from = state.exercises.indexOfFirst { it.id == exerciseId }
        val to = from + delta
        if (from < 0 || to !in state.exercises.indices) return
        val shown = state.currentExercise?.id
        val list = state.exercises.toMutableList().apply { add(to, removeAt(from)) }
        _uiState.update {
            it.copy(exercises = list, currentExerciseIndex = list.indexOfFirst { e -> e.id == shown }.coerceAtLeast(0))
        }
        persistDraft()
    }

    /** Saved sessions for F18 prefill; empty on error (prefill is a nicety). */
    private suspend fun pastSessions(): List<WorkoutSession> = try {
        sessionRepository.getAllWorkoutSessions().first()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }

    /** F18: values from [last] where it has them and the exercise tracks them; else this set's own. */
    private fun WorkoutSet.prefilledFrom(
        last: com.rukavina.gymbuddy.domain.model.WorkoutSet?,
        fields: List<SetField>,
        units: PreferredUnits,
    ): WorkoutSet {
        if (last == null) return this
        return copy(
            reps = last.reps?.takeIf { SetField.Reps in fields }?.toString() ?: reps,
            weight = last.weightKg?.takeIf { SetField.Weight in fields }?.let { displayWeight(it, units) } ?: weight,
            duration = last.durationSeconds?.takeIf { SetField.Duration in fields }?.toString() ?: duration,
            distance = last.distanceMeters?.takeIf { SetField.Distance in fields }?.let(SetFields::format) ?: distance,
        )
    }

    private suspend fun userStates() = try {
        exerciseRepository.observeUserStates().first()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyMap()
    }

    private fun followUserNotes() {
        viewModelScope.launch {
            exerciseRepository.observeUserStates()
                .map { all -> all.values.mapNotNull { s -> s.note?.takeIf { it.isNotBlank() }?.let { s.exerciseId to it } }.toMap() }
                .distinctUntilChanged()
                .catch { emit(emptyMap()) }
                .collect { notes -> _uiState.update { it.copy(userNotes = notes) } }
        }
    }

    /** Your note sheet in the workout (R13-14): the note for this exercise everywhere. */
    fun saveUserNote(exerciseId: String, note: String) {
        viewModelScope.launch {
            try {
                exerciseRepository.updateExerciseNote(exerciseId, note.trim().ifEmpty { null })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(WorkoutEvent.SaveFailed(NOTE_FAILED))
            }
        }
    }

    /** ⋯ → Workout note. */
    fun updateNote(note: String) {
        _uiState.update { it.copy(note = note) }
        persistDraft()
    }

    /**
     * ⋯ → Rest timer: this exercise's rest for the rest of the workout
     * (the template is unchanged). A rest running after one of its sets
     * takes the new length too.
     */
    fun setRest(seconds: Int) {
        if (seconds <= 0) return
        val exercise = _uiState.value.currentExercise ?: return
        _uiState.update { s ->
            val rest = s.rest?.takeIf { r -> exercise.sets.any { it.id == r.afterSetId } }
            s.copy(
                exercises = s.exercises.map { if (it.id == exercise.id) it.copy(restSeconds = seconds) else it },
                rest = if (rest != null) rest.copy(seconds = seconds) else s.rest,
            )
        }
        persistDraft()
    }

    // --- delete + undo ---

    /** Swipe or long-press → Delete: no confirm, Undo on the snackbar. */
    fun deleteSet(exerciseId: String, setId: String) {
        val state = _uiState.value
        val exercise = state.exercises.firstOrNull { it.id == exerciseId } ?: return
        val index = exercise.sets.indexOfFirst { it.id == setId }
        if (index < 0) return
        val rest = state.rest?.takeIf { it.afterSetId == setId }
        deleted = Deleted(exerciseId, index, exercise.sets[index], rest)
        _uiState.update {
            it.copy(
                exercises = it.exercises.map { ex ->
                    if (ex.id == exerciseId) ex.copy(sets = renumber(ex.sets.filterNot { s -> s.id == setId })) else ex
                },
                rest = if (rest != null) null else it.rest,
                keypad = it.keypad?.takeIf { k -> k.setId != setId },
            )
        }
        _events.trySend(WorkoutEvent.SetDeleted(setId))
        persistDraft()
    }

    fun undoDelete(setId: String) {
        val d = deleted?.takeIf { it.set.id == setId } ?: return
        deleted = null
        _uiState.update {
            it.copy(
                exercises = it.exercises.map { ex ->
                    if (ex.id != d.exerciseId) ex
                    else ex.copy(sets = renumber(ex.sets.toMutableList().apply { add(d.index.coerceAtMost(size), d.set) }))
                },
                rest = d.rest ?: it.rest,
            )
        }
        persistDraft()
    }

    private fun renumber(sets: List<WorkoutSet>) = sets.mapIndexed { i, s -> s.copy(setNumber = i + 1) }

    // --- finish, discard ---

    fun requestFinish() = _uiState.update { it.copy(dialog = WorkoutDialog.Finish, keypad = null) }

    fun requestDiscard() = _uiState.update { it.copy(dialog = WorkoutDialog.Discard, keypad = null) }

    fun dismissDialog() = _uiState.update { it.copy(dialog = null) }

    fun finishPreview(): FinishPreview {
        val state = _uiState.value
        return FinishPreview(
            minutes = state.elapsedSeconds / 60,
            sets = state.doneSets,
            lifted = lifted(state.exercises),
            setsLeft = state.setsLeft,
        )
    }

    /**
     * Save the workout's done sets and stop the timer. Undone sets are
     * dropped (decision 6). On success the summary is ready; with [auto]
     * (F25 auto-finish) there is no summary, [ActiveWorkoutUiState.autoFinished] instead.
     */
    fun saveWorkout(endedAt: Long? = null, durationSeconds: Long? = null, auto: Boolean = false) {
        // A second tap while the first save runs would store the workout twice.
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, dialog = null) }
        viewModelScope.launch {
            timerJob?.cancel()

            val state = _uiState.value

            // Exercises with no done sets are dropped before assigning
            // orderIndex, so the kept exercises stay contiguously ordered.
            val kept = state.exercises.mapNotNull { activeExercise ->
                val done = activeExercise.sets.filter {
                    it.isCompleted && SetFields.savable(activeExercise.exerciseTrackingType, it)
                }
                if (done.isEmpty()) null else activeExercise to done
            }
            val performedExercises = kept.mapIndexed { index, (activeExercise, done) ->
                val fields = SetFields.of(activeExercise.exerciseTrackingType)
                // exerciseName/exerciseCategory/exercisePrimaryMuscles are
                // placeholders here - ValidateWorkoutSessionSetsUseCase
                // resolves the real Exercise and stamps the actual snapshot
                // before this is persisted.
                PerformedExercise(
                    id = idGenerator.newId(),
                    exerciseId = activeExercise.exerciseId,
                    orderIndex = index,
                    exerciseName = "",
                    exerciseCategory = ExerciseCategory.STRENGTH,
                    exerciseTrackingType = activeExercise.exerciseTrackingType,
                    exercisePrimaryMuscles = emptyList(),
                    sets = done.mapIndexed { setIndex, uiSet ->
                        com.rukavina.gymbuddy.domain.model.WorkoutSet(
                            id = idGenerator.newId(),
                            // Convert weight from display units to metric (kg) for storage
                            weightKg = if (SetField.Weight in fields) {
                                UnitConverter.weightToMetric(uiSet.weight, state.preferredUnits)
                            } else null,
                            reps = if (SetField.Reps in fields) uiSet.reps.toIntOrNull() else null,
                            durationSeconds = if (SetField.Duration in fields) uiSet.duration.toIntOrNull() else null,
                            distanceMeters = if (SetField.Distance in fields) uiSet.distance.toFloatOrNull() else null,
                            isCompleted = true,
                            orderIndex = setIndex
                        )
                    }
                )
            }

            // Nothing logged: a rejected finish, not a discard. Resume the
            // timer and keep the user here to log a set or discard.
            if (performedExercises.isEmpty()) {
                rejectSave(NOTHING_LOGGED)
                return@launch
            }

            val end = endedAt ?: clock.millis()
            val duration = durationSeconds ?: state.elapsedSeconds
            val workoutSession = WorkoutSession(
                id = idGenerator.newId(),
                startedAt = state.workoutStartTime,
                endedAt = end,
                durationSeconds = duration.toInt(),
                title = state.workoutTitle.ifBlank { "Workout" },
                templateId = state.templateId,
                templateTitle = state.templateTitle,
                notes = state.note.trim().ifEmpty { null },
                performedExercises = performedExercises
            )

            createWorkoutSessionUseCase(workoutSession)
                .onSuccess {
                    clearDraft()
                    deleted = null
                    val summary = WorkoutSummary(
                        title = workoutSession.title,
                        startedAt = state.workoutStartTime,
                        endedAt = end,
                        durationSeconds = duration,
                        sets = kept.sumOf { it.second.size },
                        lifted = lifted(state.exercises),
                        units = state.preferredUnits,
                        exercises = kept.map { (ex, done) ->
                            SummaryExercise(ex.exerciseName, done.size, ex.exerciseTrackingType, topSet(done))
                        },
                        offerBackup = owners.owner == null,
                        template = if (state.templateId == null) templateFrom(workoutSession.title, kept) else null,
                    )
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            workoutSaved = true,
                            isTimerRunning = false,
                            exercises = emptyList(),
                            elapsedSeconds = 0L,
                            workoutTitle = "",
                            note = "",
                            rest = null,
                            keypad = null,
                            summary = if (auto) null else summary,
                            autoFinished = if (auto) AutoFinished(workoutSession.title, end) else null,
                            unfinishedSince = null,
                            resumedAt = null,
                        )
                    }
                }
                .onFailure { error -> rejectSave(error.message ?: "Failed to save workout") }
        }
    }

    // Leave exercises intact; neither saved nor discarded is set, so the
    // user stays on the workout and can retry.
    private fun rejectSave(message: String) {
        if (_uiState.value.isTimerRunning) startTimer()
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
        _events.trySend(WorkoutEvent.SaveFailed(message))
    }

    /** Summary "Not now": hides the backup card for this summary only (decision 7). */
    fun dismissBackup() = _uiState.update { s -> s.copy(summary = s.summary?.copy(offerBackup = false)) }

    /** F24b: saves the summary's template once; then the summary offers Edit. */
    fun saveSummaryAsTemplate() {
        val summary = _uiState.value.summary ?: return
        val template = summary.template ?: return
        if (summary.savedTemplateId != null || savingTemplate) return
        savingTemplate = true
        viewModelScope.launch {
            createTemplate(template)
                .onSuccess {
                    _uiState.update { s -> s.copy(summary = s.summary?.copy(savedTemplateId = template.id, templateFailed = false)) }
                }
                .onFailure { _uiState.update { s -> s.copy(summary = s.summary?.copy(templateFailed = true)) } }
            savingTemplate = false
        }
    }

    private var savingTemplate = false

    /** One planned exercise per kept one: done set count, top set targets, rest; no weight. */
    private fun templateFrom(title: String, kept: List<Pair<ActiveExercise, List<WorkoutSet>>>) = WorkoutTemplate(
        id = idGenerator.newId(),
        title = title,
        templateExercises = kept.mapIndexed { index, (ex, done) ->
            val top = topSet(done)
            val fields = SetFields.of(ex.exerciseTrackingType)
            TemplateExercise(
                id = idGenerator.newId(),
                exerciseId = ex.exerciseId,
                exerciseName = ex.exerciseName,
                exerciseTrackingType = ex.exerciseTrackingType,
                plannedSets = done.size,
                plannedReps = if (SetField.Reps in fields) top.reps.toIntOrNull()?.takeIf { it > 0 } else null,
                orderIndex = index,
                restSeconds = ex.restSeconds?.takeIf { it > 0 },
                plannedDurationSeconds = if (SetField.Duration in fields) top.duration.toIntOrNull()?.takeIf { it > 0 } else null,
                plannedDistanceMeters = if (SetField.Distance in fields) top.distance.toFloatOrNull()?.takeIf { it > 0f } else null,
            )
        },
    )

    /** Summary Done. */
    fun closeSummary() = _uiState.update { it.copy(summary = null) }

    /**
     * Discard the current workout without saving.
     */
    fun discardWorkout() {
        clearDraft()
        timerJob?.cancel()
        timerJob = null
        deleted = null
        val old = _uiState.value
        _uiState.value = ActiveWorkoutUiState(
            workoutDiscarded = true,
            isTimerRunning = false,
            preferredUnits = old.preferredUnits,
            restBuzz = old.restBuzz,
            equipment = old.equipment,
            showHint = old.showHint,
        )
    }

    /**
     * Check if there's an active workout in progress.
     */
    fun hasActiveWorkout(): Boolean = _uiState.value.running

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        /** The hint shows until this many workouts are saved. */
        const val HINT_WORKOUTS = 3
        /** Rest when the template has none; 7.10's setting replaces it. */
        const val DEFAULT_REST_SECONDS = TemplateEditorViewModel.DEFAULT_REST_SECONDS
        const val NOTHING_LOGGED = "No exercises logged. Workout not saved."
        const val NOTE_FAILED = "Couldn't save your note. Try again."

        /** kg (or lb) shown for a stored kg value, "." separator. */
        fun displayWeight(kg: Float, units: PreferredUnits): String =
            UnitConverter.weightToDisplayUnit(kg, units)

        /** Weight × reps of done sets with both, in display units. */
        fun lifted(exercises: List<ActiveExercise>): Float = exercises.sumOf { ex ->
            if (SetField.Weight !in SetFields.of(ex.exerciseTrackingType) || SetField.Reps !in SetFields.of(ex.exerciseTrackingType)) 0.0
            else ex.sets.filter { it.isCompleted }.sumOf { s ->
                ((KeypadValue.parse(s.weight) ?: 0f) * (s.reps.toIntOrNull() ?: 0)).toDouble()
            }
        }.toFloat()

        /** Heaviest set, then most reps / longest / farthest. */
        fun topSet(sets: List<WorkoutSet>): WorkoutSet = sets.maxWith(
            compareBy<WorkoutSet> { KeypadValue.parse(it.weight) ?: 0f }
                .thenBy { it.reps.toIntOrNull() ?: 0 }
                .thenBy { it.duration.toIntOrNull() ?: 0 }
                .thenBy { KeypadValue.parse(it.distance) ?: 0f }
        )
    }
}

private fun ActiveWorkoutUiState.toDraft(now: Long) = ActiveWorkoutDraft(
    title = workoutTitle,
    templateId = templateId,
    templateTitle = templateTitle,
    startedAt = workoutStartTime,
    elapsedSeconds = elapsedSeconds,
    isTimerRunning = isTimerRunning,
    savedAt = now,
    currentExerciseIndex = currentExerciseIndex,
    rest = rest?.let { ActiveWorkoutDraft.Rest(it.afterSetId, it.startedAtElapsed, it.seconds) },
    note = note,
    resumedAt = resumedAt,
    exercises = exercises.map { exercise ->
        ActiveWorkoutDraft.Exercise(
            id = exercise.id,
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.exerciseName,
            trackingType = exercise.exerciseTrackingType,
            plannedSets = exercise.plannedSets,
            plannedReps = exercise.plannedReps,
            restSeconds = exercise.restSeconds,
            templateNote = exercise.templateNote,
            sets = exercise.sets.map { set ->
                ActiveWorkoutDraft.Set(
                    id = set.id,
                    setNumber = set.setNumber,
                    reps = set.reps,
                    weight = set.weight,
                    duration = set.duration,
                    distance = set.distance,
                    notes = set.notes,
                    isCompleted = set.isCompleted,
                    doneAt = set.doneAt,
                    doneAtElapsed = set.doneAtElapsed,
                )
            }
        )
    }
)

private fun ActiveWorkoutDraft.Exercise.toActiveExercise(savedAt: Long, savedElapsed: Long) = ActiveExercise(
    id = id,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    exerciseTrackingType = trackingType,
    sets = sets.map { set ->
        WorkoutSet(
            id = set.id,
            setNumber = set.setNumber,
            reps = set.reps,
            weight = set.weight,
            duration = set.duration,
            distance = set.distance,
            notes = set.notes,
            isCompleted = set.isCompleted,
            doneAt = set.doneAt ?: savedAt.takeIf { set.isCompleted },
            doneAtElapsed = set.doneAtElapsed ?: savedElapsed.takeIf { set.isCompleted },
        )
    },
    plannedSets = plannedSets,
    plannedReps = plannedReps,
    restSeconds = restSeconds,
    templateNote = templateNote,
)

package com.rukavina.gymbuddy.ui.progress.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.SetType
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import com.rukavina.gymbuddy.domain.usecase.workout.UpdateWorkoutSessionUseCase
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.ui.exercises.page.toUiSet
import com.rukavina.gymbuddy.ui.template.TemplateSources
import com.rukavina.gymbuddy.ui.workout.SetField
import com.rukavina.gymbuddy.ui.workout.SetFields
import com.rukavina.gymbuddy.ui.workout.SetRule
import com.rukavina.gymbuddy.ui.workout.WorkoutSet
import com.rukavina.gymbuddy.utils.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import com.rukavina.gymbuddy.domain.model.WorkoutSet as StoredSet

/**
 * One exercise card while editing. [performed] is the stored row (id,
 * snapshot, order); [sets] are the workout's text sets in display units.
 */
data class EditExercise(val performed: PerformedExercise, val sets: List<WorkoutSet>) {
    val id: String get() = performed.id
}

/** The keypad on one value of one set. [fresh]: the first key replaces the value. */
data class EditKeypad(
    val exerciseId: String,
    val setId: String,
    val field: SetField,
    val text: String,
    val fresh: Boolean = true,
) {
    // Weight stays optional here: past sessions were logged before F17.
    val rule: SetRule? get() = if (fresh) null else SetFields.rule(this.field, text, weightRequired = false)
}

data class SessionEditUiState(
    val loaded: Boolean = false,
    val title: String = "",
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val note: String = "",
    val exercises: List<EditExercise> = emptyList(),
    val units: PreferredUnits = PreferredUnits.METRIC,
    val zone: ZoneId = ZoneId.systemDefault(),
    val keypad: EditKeypad? = null,
    val nameError: Boolean = false,
    val saving: Boolean = false,
    val confirmDiscard: Boolean = false,
    val dirty: Boolean = false,
) {
    val durationSeconds: Int get() = ((endedAt - startedAt) / 1000L).toInt().coerceAtLeast(0)
    val canSave: Boolean get() = loaded && !saving
}

sealed interface SessionEditEvent {
    data object Close : SessionEditEvent
    data object SaveFailed : SessionEditEvent
    /** Swipe delete: snackbar "Set deleted" with Undo. */
    data class SetDeleted(val exerciseId: String, val setId: String) : SessionEditEvent
}

/**
 * Edit workout (Figma R14-4 107:16270, Plan 7 step 09b). Edits stay here
 * until Save, which validates and writes the whole session through
 * [UpdateWorkoutSessionUseCase]. Exercises can't be added, removed or
 * reordered here; one with no sets left is dropped on save.
 */
@HiltViewModel
class SessionEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    sources: TemplateSources,
    private val sessions: WorkoutSessionRepository,
    private val update: UpdateWorkoutSessionUseCase,
    private val idGenerator: IdGenerator,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedState.get<String>(SessionDetailViewModel.ARG_SESSION_ID))

    private val _uiState = MutableStateFlow(SessionEditUiState())
    val uiState: StateFlow<SessionEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<SessionEditEvent>(Channel.BUFFERED)
    val events: Flow<SessionEditEvent> = _events.receiveAsFlow()

    private var existing: WorkoutSession? = null
    private var saved: Snapshot? = null
    private var lastDeleted: Triple<String, Int, WorkoutSet>? = null

    init {
        viewModelScope.launch { load(sources.units.first()) }
    }

    private suspend fun load(units: PreferredUnits) {
        val session = try {
            sessions.getWorkoutSessionById(sessionId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (session == null || session.deletedAt != null) {
            _events.send(SessionEditEvent.Close)
            return
        }
        existing = session
        val exercises = session.performedExercises.sortedBy { it.orderIndex }.map { p ->
            EditExercise(p, p.sets.sortedBy { it.orderIndex }.map { it.toUiSet(units) }.renumbered())
        }
        _uiState.update {
            it.copy(
                loaded = true,
                title = session.title,
                startedAt = session.startedAt,
                endedAt = session.endedAt ?: (session.startedAt + session.durationSeconds * 1000L),
                note = session.notes.orEmpty(),
                exercises = exercises,
                units = units,
            )
        }
        saved = Snapshot.of(_uiState.value)
    }

    fun setTitle(title: String) = edit { it.copy(title = title, nameError = it.nameError && title.isBlank()) }

    fun setNote(note: String) = edit { it.copy(note = note) }

    // Date and times (R14-4): duration = end − start.

    /** New day; start and end keep their times of day. */
    fun setDate(date: LocalDate) = edit { s ->
        val start = Instant.ofEpochMilli(s.startedAt).atZone(s.zone)
        val shift = date.toEpochDay() - start.toLocalDate().toEpochDay()
        s.copy(
            startedAt = start.plusDays(shift).toInstant().toEpochMilli(),
            endedAt = Instant.ofEpochMilli(s.endedAt).atZone(s.zone).plusDays(shift).toInstant().toEpochMilli(),
        )
    }

    /** New start time on the same day; the end keeps its time and moves past midnight if needed. */
    fun setStart(time: LocalTime) = edit { s ->
        val start = Instant.ofEpochMilli(s.startedAt).atZone(s.zone).with(time).withSecond(0).withNano(0)
        s.copy(startedAt = start.toInstant().toEpochMilli(), endedAt = endAfter(start, endTime(s), s.zone))
    }

    fun setEnd(time: LocalTime) = edit { s ->
        val start = Instant.ofEpochMilli(s.startedAt).atZone(s.zone)
        s.copy(endedAt = endAfter(start, time.withSecond(0).withNano(0), s.zone))
    }

    private fun endTime(s: SessionEditUiState): LocalTime = Instant.ofEpochMilli(s.endedAt).atZone(s.zone).toLocalTime()

    /** [time] on the start's day, or the next day when that would be before the start. */
    private fun endAfter(start: ZonedDateTime, time: LocalTime, zone: ZoneId): Long {
        var end = start.toLocalDate().atTime(time).atZone(zone)
        if (end.isBefore(start)) end = end.plusDays(1)
        return end.toInstant().toEpochMilli()
    }

    // Sets

    fun addSet(exerciseId: String) {
        val exercise = _uiState.value.exercises.firstOrNull { it.id == exerciseId } ?: return
        val last = exercise.sets.lastOrNull()
        val set = (last ?: WorkoutSet(id = "", setNumber = 0)).copy(id = idGenerator.newId())
        editSets(exerciseId) { it + set }
        // An empty set needs its values before it can be saved.
        if (last == null) openKeypad(exerciseId, set.id, SetFields.of(exercise.performed.exerciseTrackingType).first())
    }

    fun deleteSet(exerciseId: String, setId: String) {
        val sets = _uiState.value.exercises.firstOrNull { it.id == exerciseId }?.sets ?: return
        val index = sets.indexOfFirst { it.id == setId }
        if (index < 0) return
        lastDeleted = Triple(exerciseId, index, sets[index])
        editSets(exerciseId) { it - sets[index] }
        _uiState.update { s -> s.copy(keypad = s.keypad?.takeIf { it.setId != setId }) }
        _events.trySend(SessionEditEvent.SetDeleted(exerciseId, setId))
    }

    fun undoDelete(setId: String) {
        val (exerciseId, index, set) = lastDeleted?.takeIf { it.third.id == setId } ?: return
        lastDeleted = null
        editSets(exerciseId) { sets -> sets.toMutableList().apply { add(index.coerceAtMost(size), set) } }
    }

    // Keypad: chips open it; Next walks this set's values, then the next set's.

    fun openKeypad(exerciseId: String, setId: String, field: SetField) {
        val set = setOf(exerciseId, setId) ?: return
        val text = SetFields.text(set, field)
        val shown = if (field.kind == KeypadValue.Kind.Clock) KeypadValue.clockDigits(text.toIntOrNull()) else text
        _uiState.update { it.copy(keypad = EditKeypad(exerciseId, setId, field, shown)) }
    }

    /** The value after [keypad]'s in this exercise, or null on its last one. */
    fun nextTarget(keypad: EditKeypad): Pair<String, SetField>? {
        val exercise = _uiState.value.exercises.firstOrNull { it.id == keypad.exerciseId } ?: return null
        val fields = SetFields.of(exercise.performed.exerciseTrackingType)
        val all = exercise.sets.flatMap { set -> fields.map { set.id to it } }
        val i = all.indexOf(keypad.setId to keypad.field)
        return if (i < 0) null else all.getOrNull(i + 1)
    }

    fun keypad(input: KeypadInput, separator: Char = '.') {
        val keypad = _uiState.value.keypad ?: return
        when (input) {
            KeypadInput.Cancel -> _uiState.update { it.copy(keypad = null) }
            KeypadInput.Next -> {
                if (keypad.rule != null) return
                if (!keypad.fresh) {
                    val stored = when (keypad.field.kind) {
                        KeypadValue.Kind.Clock -> KeypadValue.parseClock(keypad.text)?.toString().orEmpty()
                        KeypadValue.Kind.Whole -> KeypadValue.parseWhole(keypad.text)?.toString().orEmpty()
                        KeypadValue.Kind.Decimal -> KeypadValue.parse(keypad.text)?.let(SetFields::format).orEmpty()
                    }
                    editSets(keypad.exerciseId) { sets ->
                        sets.map { if (it.id == keypad.setId) SetFields.with(it, keypad.field, stored) else it }
                    }
                }
                val next = nextTarget(keypad)
                if (next == null) _uiState.update { it.copy(keypad = null) }
                else openKeypad(keypad.exerciseId, next.first, next.second)
            }
            else -> _uiState.update {
                val base = if (keypad.fresh) "" else keypad.text
                it.copy(keypad = keypad.copy(text = KeypadValue.apply(base, input, keypad.field.kind, separator), fresh = false))
            }
        }
    }

    private fun setOf(exerciseId: String, setId: String): WorkoutSet? =
        _uiState.value.exercises.firstOrNull { it.id == exerciseId }?.sets?.firstOrNull { it.id == setId }

    // Leave / save

    fun requestClose() {
        if (_uiState.value.dirty) _uiState.update { it.copy(confirmDiscard = true) }
        else _events.trySend(SessionEditEvent.Close)
    }

    fun keepEditing() = _uiState.update { it.copy(confirmDiscard = false) }

    fun discard() {
        _uiState.update { it.copy(confirmDiscard = false) }
        _events.trySend(SessionEditEvent.Close)
    }

    fun save() {
        val s = _uiState.value
        val base = existing ?: return
        if (!s.canSave) return
        if (s.title.isBlank()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        // A set missing a value (an empty added set): open the keypad on it instead of failing.
        s.exercises.firstNotNullOfOrNull { e ->
            val type = e.performed.exerciseTrackingType
            e.sets.firstOrNull { !SetFields.savable(type, it) }?.let { set ->
                Triple(e.id, set.id, SetFields.of(type).first { SetFields.rule(it, SetFields.text(set, it), weightRequired = false) != null })
            }
        }?.let { (exerciseId, setId, field) ->
            openKeypad(exerciseId, setId, field)
            return
        }
        _uiState.update { it.copy(saving = true, keypad = null) }
        viewModelScope.launch {
            val result = update(build(base, s))
            if (result.isSuccess) {
                saved = Snapshot.of(s)
                _uiState.update { it.copy(saving = false, dirty = false) }
                _events.send(SessionEditEvent.Close)
            } else {
                _uiState.update { it.copy(saving = false) }
                _events.send(SessionEditEvent.SaveFailed)
            }
        }
    }

    private fun build(base: WorkoutSession, s: SessionEditUiState): WorkoutSession {
        val performed = s.exercises.filter { it.sets.isNotEmpty() }.mapIndexed { index, e ->
            val stored = e.performed.sets.associateBy { it.id }
            val type = e.performed.exerciseTrackingType
            val fields = SetFields.of(type)
            e.performed.copy(
                orderIndex = index,
                sets = e.sets.mapIndexed { i, set ->
                    val old = stored[set.id]
                    // Unchanged: keep the stored set (no lb → kg rounding drift).
                    if (old != null && old.toUiSet(s.units).copy(setNumber = set.setNumber) == set) {
                        old.copy(orderIndex = i)
                    } else {
                        StoredSet(
                            id = set.id,
                            weightKg = if (SetField.Weight in fields) UnitConverter.weightToMetric(set.weight, s.units) else null,
                            reps = if (SetField.Reps in fields) set.reps.toIntOrNull() else null,
                            durationSeconds = if (SetField.Duration in fields) set.duration.toIntOrNull() else null,
                            distanceMeters = if (SetField.Distance in fields) set.distance.toFloatOrNull() else null,
                            setType = old?.setType ?: SetType.WORKING,
                            isCompleted = true,
                            restTakenSeconds = old?.restTakenSeconds,
                            orderIndex = i,
                        )
                    }
                },
            )
        }
        return base.copy(
            title = s.title.trim(),
            startedAt = s.startedAt,
            endedAt = s.endedAt,
            durationSeconds = s.durationSeconds,
            notes = s.note.trim().takeIf { it.isNotEmpty() },
            performedExercises = performed,
        )
    }

    private fun editSets(exerciseId: String, change: (List<WorkoutSet>) -> List<WorkoutSet>) = edit { s ->
        s.copy(exercises = s.exercises.map { if (it.id == exerciseId) it.copy(sets = change(it.sets).renumbered()) else it })
    }

    private fun edit(change: (SessionEditUiState) -> SessionEditUiState) = _uiState.update { s ->
        val next = change(s)
        next.copy(dirty = saved?.let { Snapshot.of(next) != it } ?: false)
    }

    /** What "changed" compares: trimmed text, times, set values. */
    private data class Snapshot(
        val title: String,
        val startedAt: Long,
        val endedAt: Long,
        val note: String,
        val sets: List<List<WorkoutSet>>,
    ) {
        companion object {
            fun of(s: SessionEditUiState) =
                Snapshot(s.title.trim(), s.startedAt, s.endedAt, s.note.trim(), s.exercises.map { it.sets })
        }
    }
}

private fun List<WorkoutSet>.renumbered() = mapIndexed { i, set -> set.copy(setNumber = i + 1) }

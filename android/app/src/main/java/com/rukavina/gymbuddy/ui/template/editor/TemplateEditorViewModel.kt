package com.rukavina.gymbuddy.ui.template.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import com.rukavina.gymbuddy.domain.usecase.template.CreateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.domain.usecase.template.UpdateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.design.KeypadValue
import com.rukavina.gymbuddy.ui.template.TemplateSources
import com.rukavina.gymbuddy.utils.UnitConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One value on an exercise card: a chip, or the quiet rest value. */
enum class EditField(val kind: KeypadValue.Kind) {
    Sets(KeypadValue.Kind.Whole),
    Reps(KeypadValue.Kind.Whole),
    Duration(KeypadValue.Kind.Clock),
    Distance(KeypadValue.Kind.Decimal),
    Weight(KeypadValue.Kind.Decimal),
    Rest(KeypadValue.Kind.Clock),
}

/** Chips by tracking type, in card order (Editor A: sets, reps, kg). Rest follows them. */
fun chipFields(type: ExerciseTrackingType): List<EditField> = when (type) {
    ExerciseTrackingType.WEIGHT_REPS -> listOf(EditField.Sets, EditField.Reps, EditField.Weight)
    ExerciseTrackingType.REPS_ONLY -> listOf(EditField.Sets, EditField.Reps)
    ExerciseTrackingType.WEIGHT_DURATION -> listOf(EditField.Sets, EditField.Duration, EditField.Weight)
    ExerciseTrackingType.DURATION -> listOf(EditField.Sets, EditField.Duration)
    ExerciseTrackingType.DISTANCE_DURATION -> listOf(EditField.Sets, EditField.Distance, EditField.Duration)
    ExerciseTrackingType.WEIGHT_DISTANCE -> listOf(EditField.Sets, EditField.Distance, EditField.Weight)
}

/**
 * One exercise card while editing. [key] is a list key only; [existingId]
 * carries the TemplateExercise id forward (null = added in this edit, the
 * ViewModel mints one on save). Weight and distance are metric.
 */
data class EditorExercise(
    val key: Long,
    val existingId: String?,
    val exerciseId: String,
    val name: String,
    val trackingType: ExerciseTrackingType,
    val sets: Int,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Float? = null,
    val weightKg: Float? = null,
    val restSeconds: Int? = null,
    val notes: String? = null,
) {
    val fields: List<EditField> get() = chipFields(trackingType) + EditField.Rest
}

data class KeypadTarget(val key: Long, val field: EditField)

/** Open keypad. [fresh]: the value is shown as is, the first key replaces it. */
data class KeypadState(val target: KeypadTarget, val text: String, val fresh: Boolean = true)

data class TemplateEditorUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val name: String = "",
    val exercises: List<EditorExercise> = emptyList(),
    val units: PreferredUnits = PreferredUnits.METRIC,
    /** Save was pressed with no name: "Give it a name". */
    val nameError: Boolean = false,
    val saving: Boolean = false,
    val keypad: KeypadState? = null,
    /** Back/× with changes: "Discard changes?" (R12-12). */
    val confirmDiscard: Boolean = false,
    val dirty: Boolean = false,
) {
    val canSave: Boolean get() = loaded && !saving && exercises.isNotEmpty()
}

sealed interface TemplateEditorEvent {
    data object Close : TemplateEditorEvent
    data object SaveFailed : TemplateEditorEvent
    /** Swipe remove: snackbar "<name> removed" with Undo. */
    data class Removed(val key: Long, val name: String) : TemplateEditorEvent
    /** Picker Add: snackbar "N exercises added at the end" (R12-15). */
    data class Added(val count: Int) : TemplateEditorEvent
}

/**
 * Template editor (Figma Editor A 93:12841, New template 93:13588,
 * 12 exercises 93:13736, Keypad 93:13879, Discard 93:14065, After Add
 * 93:14347). Edits stay here until Save: a half-built plan never goes
 * live, and a running workout is never touched (it holds its own copy).
 */
@HiltViewModel
class TemplateEditorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    sources: TemplateSources,
    private val templates: WorkoutTemplateRepository,
    private val exercises: ExerciseRepository,
    private val create: CreateWorkoutTemplateUseCase,
    private val update: UpdateWorkoutTemplateUseCase,
    private val idGenerator: IdGenerator,
) : ViewModel() {

    private val templateId: String? = savedState.get<String>(ARG_TEMPLATE_ID)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(TemplateEditorUiState(isNew = templateId == null))
    val uiState: StateFlow<TemplateEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<TemplateEditorEvent>(Channel.BUFFERED)
    val events: Flow<TemplateEditorEvent> = _events.receiveAsFlow()

    private var existing: WorkoutTemplate? = null
    private var saved: Snapshot = Snapshot("", emptyList())
    private var nextKey = 0L
    private var lastRemoved: Pair<Int, EditorExercise>? = null

    init {
        viewModelScope.launch { sources.units.collect { u -> _uiState.update { it.copy(units = u) } } }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (templateId == null) {
            _uiState.update { it.copy(loaded = true) }
            return
        }
        val template = try {
            templates.getTemplateById(templateId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        // Gone, or a starter (they are copied before editing): nothing to edit.
        if (template == null || template.deletedAt != null || template.source == EntitySource.DEFAULT) {
            _events.send(TemplateEditorEvent.Close)
            return
        }
        existing = template
        val items = template.templateExercises.sortedBy { it.orderIndex }.map { it.toEditor(nextKey++) }
        saved = Snapshot(template.title, items)
        _uiState.update { it.copy(loaded = true, isNew = false, name = template.title, exercises = items) }
    }

    fun setName(name: String) = edit { it.copy(name = name, nameError = it.nameError && name.isBlank()) }

    fun setNote(key: Long, note: String) = editExercise(key) { it.copy(notes = note) }

    /** Picker "Add N": appended in pick order, defaults 3 × 10, kg empty, rest 2:00. */
    fun add(exerciseIds: List<String>) {
        if (exerciseIds.isEmpty()) return
        viewModelScope.launch {
            val picked = exerciseIds.mapNotNull { id -> exercises.getExerciseById(id) }
            if (picked.isEmpty()) return@launch
            val added = picked.map { e ->
                val reps = e.trackingType == ExerciseTrackingType.WEIGHT_REPS || e.trackingType == ExerciseTrackingType.REPS_ONLY
                EditorExercise(
                    key = nextKey++,
                    existingId = null,
                    exerciseId = e.id,
                    name = e.name,
                    trackingType = e.trackingType,
                    sets = DEFAULT_SETS,
                    reps = if (reps) DEFAULT_REPS else null,
                    restSeconds = DEFAULT_REST_SECONDS,
                )
            }
            edit { it.copy(exercises = it.exercises + added) }
            _events.send(TemplateEditorEvent.Added(added.size))
        }
    }

    fun remove(key: Long) {
        val list = _uiState.value.exercises
        val index = list.indexOfFirst { it.key == key }
        if (index < 0) return
        lastRemoved = index to list[index]
        edit { it.copy(exercises = list - list[index], keypad = it.keypad?.takeIf { k -> k.target.key != key }) }
        _events.trySend(TemplateEditorEvent.Removed(key, list[index].name))
    }

    fun undoRemove(key: Long) {
        val (index, item) = lastRemoved?.takeIf { it.second.key == key } ?: return
        lastRemoved = null
        edit { s ->
            val list = s.exercises.toMutableList()
            // New list key: the swiped-away card's composition (and swipe state) must not come back.
            list.add(index.coerceAtMost(list.size), item.copy(key = nextKey++))
            s.copy(exercises = list)
        }
    }

    /** Drag: move [key] to [toIndex]. */
    fun move(key: Long, toIndex: Int) = edit { s ->
        val list = s.exercises.toMutableList()
        val from = list.indexOfFirst { it.key == key }
        if (from < 0) return@edit s
        val item = list.removeAt(from)
        list.add(toIndex.coerceIn(0, list.size), item)
        s.copy(exercises = list)
    }

    /** TalkBack "Move up" (-1) / "Move down" (+1). */
    fun moveBy(key: Long, delta: Int) {
        val from = _uiState.value.exercises.indexOfFirst { it.key == key }
        if (from >= 0) move(key, from + delta)
    }

    // Keypad (R12-11): chips and rest open it; Next walks the cards in order.

    fun openKeypad(target: KeypadTarget) {
        val item = _uiState.value.exercises.firstOrNull { it.key == target.key } ?: return
        _uiState.update { it.copy(keypad = KeypadState(target, item.text(target.field, it.units))) }
    }

    fun keypad(input: KeypadInput, separator: Char = '.') {
        val state = _uiState.value.keypad ?: return
        when (input) {
            KeypadInput.Cancel -> _uiState.update { it.copy(keypad = null) }
            KeypadInput.Next -> {
                commit(state)
                val next = nextTarget(state.target)
                if (next == null) _uiState.update { it.copy(keypad = null) } else openKeypad(next)
            }
            else -> {
                val base = if (state.fresh) "" else state.text
                val text = KeypadValue.apply(base, input, state.target.field.kind, separator)
                _uiState.update { it.copy(keypad = state.copy(text = text, fresh = false)) }
            }
        }
    }

    /** The field after [target]: rest of this card, then the next card's first chip. Null at the end. */
    fun nextTarget(target: KeypadTarget): KeypadTarget? {
        val all = _uiState.value.exercises.flatMap { e -> e.fields.map { KeypadTarget(e.key, it) } }
        val i = all.indexOf(target)
        return if (i < 0) null else all.getOrNull(i + 1)
    }

    private fun commit(state: KeypadState) {
        if (state.fresh) return
        val units = _uiState.value.units
        val text = state.text
        editExercise(state.target.key) { e ->
            when (state.target.field) {
                // Sets must stay ≥ 1: an empty or 0 entry keeps the old value.
                EditField.Sets -> e.copy(sets = KeypadValue.parseWhole(text)?.takeIf { it > 0 } ?: e.sets)
                EditField.Reps -> e.copy(reps = KeypadValue.parseWhole(text)?.takeIf { it > 0 })
                EditField.Duration -> e.copy(durationSeconds = KeypadValue.parseClock(text)?.takeIf { it > 0 })
                EditField.Distance -> e.copy(distanceMeters = KeypadValue.parse(text)?.takeIf { it > 0f })
                EditField.Weight -> e.copy(
                    weightKg = KeypadValue.parse(text)?.takeIf { it > 0f }
                        ?.let { UnitConverter.weightToMetric(it.toString(), units) },
                )
                EditField.Rest -> e.copy(restSeconds = KeypadValue.parseClock(text)?.takeIf { it > 0 })
            }
        }
    }

    // Leave / save

    /** × or system back. */
    fun requestClose() {
        if (_uiState.value.dirty) _uiState.update { it.copy(confirmDiscard = true) }
        else _events.trySend(TemplateEditorEvent.Close)
    }

    fun keepEditing() = _uiState.update { it.copy(confirmDiscard = false) }

    fun discard() {
        _uiState.update { it.copy(confirmDiscard = false) }
        _events.trySend(TemplateEditorEvent.Close)
    }

    fun save() {
        val s = _uiState.value
        if (!s.canSave) return
        if (s.name.isBlank()) {
            _uiState.update { it.copy(nameError = true) }
            return
        }
        _uiState.update { it.copy(saving = true, keypad = null) }
        viewModelScope.launch {
            val template = build(s)
            val result = if (existing == null) create(template) else update(template)
            if (result.isSuccess) {
                saved = Snapshot(s.name, s.exercises)
                _uiState.update { it.copy(saving = false, dirty = false) }
                _events.send(TemplateEditorEvent.Close)
            } else {
                _uiState.update { it.copy(saving = false) }
                _events.send(TemplateEditorEvent.SaveFailed)
            }
        }
    }

    private fun build(s: TemplateEditorUiState): WorkoutTemplate {
        val items = s.exercises.mapIndexed { index, e ->
            TemplateExercise(
                id = e.existingId ?: idGenerator.newId(),
                exerciseId = e.exerciseId,
                exerciseName = e.name,
                exerciseTrackingType = e.trackingType,
                plannedSets = e.sets,
                plannedReps = e.reps,
                orderIndex = index,
                restSeconds = e.restSeconds,
                plannedDurationSeconds = e.durationSeconds,
                plannedDistanceMeters = e.distanceMeters,
                plannedWeightKg = e.weightKg,
                notes = e.notes?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
        val base = existing
        return base?.copy(title = s.name.trim(), templateExercises = items)
            ?: WorkoutTemplate(id = idGenerator.newId(), title = s.name.trim(), templateExercises = items, source = EntitySource.CUSTOM)
    }

    private fun editExercise(key: Long, change: (EditorExercise) -> EditorExercise) = edit { s ->
        s.copy(exercises = s.exercises.map { if (it.key == key) change(it) else it })
    }

    private fun edit(change: (TemplateEditorUiState) -> TemplateEditorUiState) = _uiState.update { s ->
        val next = change(s)
        next.copy(dirty = Snapshot(next.name, next.exercises) != saved)
    }

    /** What "changed" compares: name and cards, without list keys or note whitespace. */
    private data class Snapshot(val name: String, val items: List<EditorExercise>) {
        override fun equals(other: Any?): Boolean =
            other is Snapshot && name.trim() == other.name.trim() && norm(items) == norm(other.items)

        override fun hashCode(): Int = name.trim().hashCode()

        private fun norm(list: List<EditorExercise>) =
            list.map { it.copy(key = 0, notes = it.notes?.trim()?.takeIf { n -> n.isNotEmpty() }) }
    }

    companion object {
        const val ARG_TEMPLATE_ID = "templateId"
        const val DEFAULT_SETS = 3
        const val DEFAULT_REPS = 10
        /** Rest for new exercises until 7.10 adds the Settings default (decision 7). */
        const val DEFAULT_REST_SECONDS = 120
    }
}

private fun TemplateExercise.toEditor(key: Long) = EditorExercise(
    key = key,
    existingId = id,
    exerciseId = exerciseId,
    name = exerciseName,
    trackingType = exerciseTrackingType,
    sets = plannedSets,
    reps = plannedReps,
    durationSeconds = plannedDurationSeconds,
    distanceMeters = plannedDistanceMeters,
    weightKg = plannedWeightKg,
    restSeconds = restSeconds,
    notes = notes,
)

/** Keypad start text for [field], in display units. */
internal fun EditorExercise.text(field: EditField, units: PreferredUnits): String = when (field) {
    EditField.Sets -> sets.toString()
    EditField.Reps -> reps?.toString() ?: ""
    EditField.Duration -> KeypadValue.clockDigits(durationSeconds)
    EditField.Distance -> distanceMeters?.let { trimmed(it) } ?: ""
    EditField.Weight -> weightKg?.let { UnitConverter.weightToDisplayUnit(it, units) } ?: ""
    EditField.Rest -> KeypadValue.clockDigits(restSeconds)
}

private fun trimmed(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()

package com.rukavina.gymbuddy.ui.exercises.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.ui.template.TemplateSources
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
import javax.inject.Inject

data class ExerciseEditUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val form: ExerciseForm = ExerciseForm(),
    /** The name was typed and cleared: "Give it a name". */
    val nameError: Boolean = false,
    val saving: Boolean = false,
    val units: PreferredUnits = PreferredUnits.METRIC,
) {
    val canSave: Boolean get() = loaded && !saving && form.canSave
}

sealed interface ExerciseEditEvent {
    /** Saved: a new one opens its page, an edit goes back to it. */
    data class Saved(val exerciseId: String, val isNew: Boolean) : ExerciseEditEvent
    data object Close : ExerciseEditEvent
    data object Failed : ExerciseEditEvent
}

/**
 * Create A and Edit (Figma R13-4): one short screen with name, what you
 * log, main muscle and equipment; other muscles, steps, rest and the
 * hidden data fields fold under More details. Only your own exercises
 * can be edited (no "Make a copy" of built-ins, owner).
 */
@HiltViewModel
class ExerciseEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val exercises: ExerciseRepository,
    private val creator: ExerciseCreator,
    sources: TemplateSources,
) : ViewModel() {

    private val exerciseId: String? = savedState.get<String>(ARG_EXERCISE_ID)?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(ExerciseEditUiState(isNew = exerciseId == null))
    val uiState: StateFlow<ExerciseEditUiState> = _uiState.asStateFlow()

    private val _events = Channel<ExerciseEditEvent>(Channel.BUFFERED)
    val events: Flow<ExerciseEditEvent> = _events.receiveAsFlow()

    private var base: Exercise? = null
    private var baseRest: Int? = null

    init {
        viewModelScope.launch { sources.units.collect { u -> _uiState.update { it.copy(units = u) } } }
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (exerciseId == null) {
            _uiState.update { it.copy(loaded = true) }
            return
        }
        val exercise = try {
            exercises.getExerciseById(exerciseId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        if (exercise == null || exercise.source != EntitySource.CUSTOM) {
            _events.send(ExerciseEditEvent.Close)
            return
        }
        val rest = exercises.observeUserState(exerciseId).first().restSeconds
        base = exercise
        baseRest = rest
        _uiState.update { it.copy(loaded = true, isNew = false, form = ExerciseForm.of(exercise, rest)) }
    }

    fun setName(name: String) = _uiState.update { it.copy(form = it.form.copy(name = name), nameError = name.isBlank()) }

    fun setTracking(type: ExerciseTrackingType) = form { it.copy(tracking = type) }

    fun setMainMuscle(muscle: MuscleGroup?) = form { it.copy(mainMuscle = muscle, otherMuscles = it.otherMuscles - setOfNotNull(muscle)) }

    fun toggleEquipment(e: Equipment) = form { it.copy(equipment = if (e in it.equipment) it.equipment - e else it.equipment + e) }

    fun toggleOtherMuscle(m: MuscleGroup) = form { it.copy(otherMuscles = if (m in it.otherMuscles) it.otherMuscles - m else it.otherMuscles + m) }

    fun setSteps(text: String) = form { it.copy(steps = text) }

    fun setRest(seconds: Int?) = form { it.copy(restSeconds = seconds) }

    fun setDifficulty(level: DifficultyLevel) = form { it.copy(difficulty = level) }

    fun setKind(kind: ExerciseType) = form { it.copy(kind = kind) }

    fun save() {
        val s = _uiState.value
        if (!s.canSave) {
            if (s.form.name.isBlank()) _uiState.update { it.copy(nameError = true) }
            return
        }
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val existing = base
            val result = if (existing == null) creator.create(s.form) else creator.update(existing, s.form, baseRest)
            _uiState.update { it.copy(saving = false) }
            result
                .onSuccess { _events.send(ExerciseEditEvent.Saved(it.id, existing == null)) }
                .onFailure { _events.send(ExerciseEditEvent.Failed) }
        }
    }

    private fun form(change: (ExerciseForm) -> ExerciseForm) = _uiState.update { it.copy(form = change(it.form)) }

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"
    }
}

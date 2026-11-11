package com.rukavina.gymbuddy.ui.template.picker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.ui.exercises.create.ExerciseCreator
import com.rukavina.gymbuddy.ui.exercises.create.ExerciseForm
import com.rukavina.gymbuddy.ui.exercises.filterExercises
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PickerRow(
    val exercise: Exercise,
    val inTemplate: Boolean,
    /** 1-based place in the add order; null = not picked. */
    val pickNumber: Int?,
)

data class ExercisePickerUiState(
    val loaded: Boolean = false,
    val query: String = "",
    /** Null = All. */
    val muscle: MuscleGroup? = null,
    val rows: List<PickerRow> = emptyList(),
    val pickCount: Int = 0,
)

/**
 * Exercise picker (Figma Picker A 93:13183, search 93:14148). Pick
 * several; numbers show the add order. Picks survive searches and
 * filters. "In template" rows can still be picked (bench twice). Back
 * drops the picks (the screen just leaves). Shared with 7.6.
 */
@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    exercises: ExerciseRepository,
    private val creator: ExerciseCreator,
) : ViewModel() {

    private val inTemplate: Set<String> =
        savedState.get<String>(ARG_IN_TEMPLATE).orEmpty().split(',').filter { it.isNotBlank() }.toSet()

    private val query = MutableStateFlow("")
    private val muscle = MutableStateFlow<MuscleGroup?>(null)
    private val picks = MutableStateFlow<List<String>>(emptyList())

    /** Picked ids in pick order. */
    val picked: List<String> get() = picks.value

    val uiState: StateFlow<ExercisePickerUiState> = combine(
        exercises.getAllExercises(), query, muscle, picks,
    ) { all, q, m, picked ->
        val rows = filterExercises(all, q, m).map { e ->
            val index = picked.indexOf(e.id)
            PickerRow(e, e.id in inTemplate, if (index >= 0) index + 1 else null)
        }
        ExercisePickerUiState(loaded = true, query = q, muscle = m, rows = rows, pickCount = picked.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisePickerUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setMuscle(value: MuscleGroup?) {
        muscle.value = value
    }

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    private val _created = Channel<Boolean>(Channel.BUFFERED)
    /** Create C finished: true = created and picked, false = failed. */
    val created: Flow<Boolean> = _created.receiveAsFlow()

    /** Create C from an empty search (R13-6): write it and pick it, you stay here. */
    fun quickCreate(form: ExerciseForm) {
        if (!form.canSave || _creating.value) return
        _creating.value = true
        viewModelScope.launch {
            val result = creator.create(form)
            _creating.value = false
            result.onSuccess { e -> picks.update { if (e.id in it) it else it + e.id } }
            _created.send(result.isSuccess)
        }
    }

    fun toggle(exerciseId: String) = picks.update { if (exerciseId in it) it - exerciseId else it + exerciseId }

    companion object {
        const val ARG_IN_TEMPLATE = "inTemplate"
        /** Back-stack result: ArrayList<String> of picked ids, in order. */
        const val RESULT_PICKED = "picked_exercises"
    }
}

package com.rukavina.gymbuddy.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.search.TypoMatcher
import com.rukavina.gymbuddy.ui.exercises.create.ExerciseCreator
import com.rukavina.gymbuddy.ui.exercises.create.ExerciseForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExercisesUiState(
    val loaded: Boolean = false,
    /** Null = All. */
    val muscle: MuscleGroup? = null,
    val rows: List<Exercise> = emptyList(),
)

sealed interface ExercisesEvent {
    data class Created(val name: String) : ExercisesEvent
    data object CreateFailed : ExercisesEvent
}

/**
 * Exercises tab (Figma 85:10376, R13-6): typo-forgiving search, muscle
 * chips, rows "Muscle · Equipment". Hidden, deprecated and deleted
 * exercises are left out. A search with no match offers Create C.
 */
@HiltViewModel
class ExercisesViewModel @Inject constructor(
    exercises: ExerciseRepository,
    private val creator: ExerciseCreator,
) : ViewModel() {

    /** Plain sync state: the field never waits on the list (05b gotcha). */
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    private val muscle = MutableStateFlow<MuscleGroup?>(null)

    private val _creating = MutableStateFlow(false)
    val creating: StateFlow<Boolean> = _creating.asStateFlow()

    private val _events = Channel<ExercisesEvent>(Channel.BUFFERED)
    val events: Flow<ExercisesEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<ExercisesUiState> = combine(exercises.getAllExercises(), _query, muscle) { all, q, m ->
        ExercisesUiState(loaded = true, muscle = m, rows = filterExercises(all, q, m))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExercisesUiState())

    fun setQuery(value: String) {
        _query.value = value
    }

    fun setMuscle(value: MuscleGroup?) {
        muscle.value = value
    }

    /** Create C: write it and stay on the list (the search now finds it). */
    fun quickCreate(form: ExerciseForm) {
        if (!form.canSave || _creating.value) return
        _creating.value = true
        viewModelScope.launch {
            val result = creator.create(form)
            _creating.value = false
            _events.send(result.fold({ ExercisesEvent.Created(it.name) }, { ExercisesEvent.CreateFailed }))
        }
    }
}

/** Search + muscle filter, best match first then by name (picker rules). */
fun filterExercises(all: List<Exercise>, query: String, muscle: MuscleGroup?): List<Exercise> =
    all.asSequence()
        .filter { it.deletedAt == null && !it.deprecated }
        .filter { muscle == null || muscle in it.primaryMuscles }
        .mapNotNull { e -> TypoMatcher.match(query, e.name)?.let { e to it } }
        .sortedWith(compareBy<Pair<Exercise, TypoMatcher.Match>> { it.second }.thenBy { it.first.name.lowercase() })
        .map { it.first }
        .toList()

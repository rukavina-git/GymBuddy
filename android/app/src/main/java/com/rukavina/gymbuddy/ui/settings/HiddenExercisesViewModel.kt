package com.rukavina.gymbuddy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Hidden exercises screen (restyled in 7.10): list and unhide, nothing else. */
@HiltViewModel
class HiddenExercisesViewModel @Inject constructor(
    private val exercises: ExerciseRepository,
) : ViewModel() {
    val hiddenExercises: Flow<List<Exercise>> = exercises.getHiddenExercises()

    fun unhideExercise(id: String) {
        viewModelScope.launch { runCatching { exercises.unhideExercise(id) } }
    }

    fun unhideAllExercises() {
        viewModelScope.launch { runCatching { exercises.unhideAllExercises() } }
    }
}

package com.rukavina.gymbuddy.ui.exercises.page

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.exercise.ExerciseHistory
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseUserState
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import com.rukavina.gymbuddy.domain.usecase.template.UpdateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.ui.template.TemplateSources
import com.rukavina.gymbuddy.ui.template.editor.TemplateEditorViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A template you can add the exercise to (your own, not a starter). */
data class TemplateChoice(val id: String, val title: String)

data class ExercisePageUiState(
    val loaded: Boolean = false,
    val exercise: Exercise? = null,
    val user: ExerciseUserState = ExerciseUserState(""),
    val history: ExerciseHistory = ExerciseHistory.Empty,
    val units: PreferredUnits = PreferredUnits.METRIC,
    /** Your templates, for ⋯ → Add to template. */
    val templates: List<TemplateChoice> = emptyList(),
    /** Titles of visible templates that use it (delete dialog, R13-11). */
    val usedIn: List<String> = emptyList(),
) {
    /** Your own exercise: no photo, Edit and Delete in ⋯ (R13-7, R13-9). */
    val own: Boolean get() = exercise?.source == EntitySource.CUSTOM
    val restSeconds: Int get() = user.restSeconds ?: TemplateEditorViewModel.DEFAULT_REST_SECONDS
}

sealed interface ExercisePageEvent {
    /** Not found, or deleted somewhere else: leave the page. */
    data object Gone : ExercisePageEvent
    data class Hidden(val name: String) : ExercisePageEvent
    data class Deleted(val name: String) : ExercisePageEvent
    data class AddedTo(val title: String) : ExercisePageEvent
    data object Failed : ExercisePageEvent
}

/**
 * Exercise page (Figma R13-1 one scroll, R13-7 your own, R13-8 never
 * done): what it is, your numbers (Last / Best, History), your settings
 * (rest, note), how to do it. Progress chart comes with 7.8.
 */
@HiltViewModel
class ExercisePageViewModel @Inject constructor(
    savedState: SavedStateHandle,
    sources: TemplateSources,
    private val exercises: ExerciseRepository,
    private val templates: WorkoutTemplateRepository,
    private val updateTemplate: UpdateWorkoutTemplateUseCase,
    private val ids: IdGenerator,
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedState.get<String>(ARG_EXERCISE_ID))

    private val _uiState = MutableStateFlow(ExercisePageUiState(user = ExerciseUserState(exerciseId)))
    val uiState: StateFlow<ExercisePageUiState> = _uiState.asStateFlow()

    private val _events = Channel<ExercisePageEvent>(Channel.BUFFERED)
    val events: Flow<ExercisePageEvent> = _events.receiveAsFlow()

    /** Set once we delete it ourselves, so the null that follows is not "gone". */
    private var leaving = false

    init {
        viewModelScope.launch {
            combine(
                exercises.observeExercise(exerciseId),
                exercises.observeUserState(exerciseId),
                sources.sessions,
                sources.templates,
                sources.units,
            ) { exercise, user, sessions, all, units ->
                val usedIn = all.filter { t -> t.templateExercises.any { it.exerciseId == exerciseId } }
                ExercisePageUiState(
                    loaded = true,
                    exercise = exercise,
                    user = user,
                    history = ExerciseHistory.of(exerciseId, sessions),
                    units = units,
                    templates = all.filter { it.source == EntitySource.CUSTOM && !it.deprecated }
                        .sortedBy { it.title.lowercase() }
                        .map { TemplateChoice(it.id, it.title) },
                    usedIn = usedIn.map { it.title },
                )
            }.collect { next ->
                if (next.exercise == null) {
                    if (!leaving) {
                        leaving = true
                        _events.send(ExercisePageEvent.Gone)
                    }
                    return@collect
                }
                _uiState.value = next
            }
        }
    }

    /** Rest sheet (R13-10); null = "Use my default". */
    fun setRest(seconds: Int?) = write { exercises.setExerciseRest(exerciseId, seconds?.takeIf { it > 0 }) }

    /** Note sheet: trimmed, blank clears it. */
    fun setNote(note: String) = write { exercises.updateExerciseNote(exerciseId, note.trim().ifEmpty { null }) }

    fun hide() {
        val name = _uiState.value.exercise?.name ?: return
        write {
            exercises.hideExercise(exerciseId)
            _events.send(ExercisePageEvent.Hidden(name))
        }
    }

    /** Your own only (R13-11): templates and past workouts keep it. */
    fun delete() {
        val s = _uiState.value
        val name = s.exercise?.name ?: return
        if (!s.own || leaving) return
        leaving = true
        write {
            exercises.deleteExercise(exerciseId)
            _events.send(ExercisePageEvent.Deleted(name))
        }
    }

    /** ⋯ → Add to template: appended with the editor defaults and your rest. */
    fun addToTemplate(templateId: String) {
        val s = _uiState.value
        val exercise = s.exercise ?: return
        write {
            val template = templates.getTemplateById(templateId)
                ?.takeIf { it.deletedAt == null && it.source == EntitySource.CUSTOM }
                ?: throw IllegalStateException("template gone")
            val item = newTemplateExercise(ids.newId(), exercise, s.user.restSeconds, template.templateExercises.size)
            updateTemplate(template.copy(templateExercises = template.templateExercises + item)).getOrThrow()
            _events.send(ExercisePageEvent.AddedTo(template.title))
        }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (leaving && _uiState.value.exercise != null) leaving = false
                _events.send(ExercisePageEvent.Failed)
            }
        }
    }

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"
    }
}

/** Editor defaults for an added exercise: 3 sets, 10 reps where reps count, no weight, your rest or 2:00. */
fun newTemplateExercise(id: String, exercise: Exercise, restSeconds: Int?, orderIndex: Int): TemplateExercise {
    val reps = exercise.trackingType == ExerciseTrackingType.WEIGHT_REPS || exercise.trackingType == ExerciseTrackingType.REPS_ONLY
    return TemplateExercise(
        id = id,
        exerciseId = exercise.id,
        exerciseName = exercise.name,
        exerciseTrackingType = exercise.trackingType,
        plannedSets = TemplateEditorViewModel.DEFAULT_SETS,
        plannedReps = if (reps) TemplateEditorViewModel.DEFAULT_REPS else null,
        orderIndex = orderIndex,
        restSeconds = restSeconds ?: TemplateEditorViewModel.DEFAULT_REST_SECONDS,
    )
}

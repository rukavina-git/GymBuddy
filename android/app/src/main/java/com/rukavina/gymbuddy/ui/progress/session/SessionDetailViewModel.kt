package com.rukavina.gymbuddy.ui.progress.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.progress.DetailExercise
import com.rukavina.gymbuddy.domain.progress.SessionDetail
import com.rukavina.gymbuddy.domain.progress.SessionTiles
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import com.rukavina.gymbuddy.ui.template.TemplateSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import javax.inject.Inject

data class SessionDetailUiState(
    val loaded: Boolean = false,
    /** Null once loaded = gone (deleted elsewhere, wrong id): the screen leaves. */
    val session: WorkoutSession? = null,
    val tiles: SessionTiles? = null,
    val exercises: List<DetailExercise> = emptyList(),
    /** The live template it came from; "Start again" only with one (note: ⋯). */
    val template: WorkoutTemplate? = null,
    val units: PreferredUnits = PreferredUnits.METRIC,
    val zone: ZoneId = ZoneId.systemDefault(),
)

/** Session detail (Figma R14-1/2/3/6, Plan 7 step 09a). */
@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    sources: TemplateSources,
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedState.get<String>(ARG_SESSION_ID))

    /** Opened from this exercise's history (R14-3): its card starts open and outlined. */
    val highlightExerciseId: String? = savedState.get<String>(ARG_EXERCISE_ID)

    val uiState: StateFlow<SessionDetailUiState> = combine(
        sources.sessions, sources.templates, sources.units,
    ) { sessions, templates, units ->
        val session = sessions.firstOrNull { it.id == sessionId }
        SessionDetailUiState(
            loaded = true,
            session = session,
            tiles = session?.let(SessionDetail::tiles),
            exercises = session?.let { SessionDetail.exercises(it, sessions) }.orEmpty(),
            template = session?.templateId?.let { id -> templates.firstOrNull { it.id == id } },
            units = units,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    companion object {
        const val ARG_SESSION_ID = "sessionId"
        const val ARG_EXERCISE_ID = "exerciseId"
    }
}

/**
 * Delete (Undo) for past sessions (R14-5). Lives at MainScreen level so
 * Undo still works after the detail screen has gone: the tombstone is
 * written at once and Undo clears it (decision 2).
 */
@HiltViewModel
class SessionDeleteViewModel @Inject constructor(
    private val repository: WorkoutSessionRepository,
) : ViewModel() {

    /** A fast Undo must not overtake its delete. */
    private val writes = Mutex()

    fun delete(id: String) {
        viewModelScope.launch { writes.withLock { repository.deleteWorkoutSession(id) } }
    }

    fun restore(id: String) {
        viewModelScope.launch { writes.withLock { repository.restoreWorkoutSession(id) } }
    }
}

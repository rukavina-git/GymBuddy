package com.rukavina.gymbuddy.ui.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.progress.Lift
import com.rukavina.gymbuddy.domain.progress.ProgressStats
import com.rukavina.gymbuddy.domain.search.TypoMatcher
import com.rukavina.gymbuddy.ui.template.TemplateSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** One month block (R7-7): header totals, then its cards. */
data class MonthSection(val month: YearMonth, val rows: List<SessionRow>, val volumeKg: Float)

data class AllSessionsUiState(
    val loaded: Boolean = false,
    /** Any session at all, before search and filter. */
    val hasAny: Boolean = false,
    val months: List<MonthSection> = emptyList(),
    /** Exercise filter (R7-8); rows then show its sets inline. */
    val filter: Lift? = null,
    /** Filter sheet: exercises you have logged. */
    val exercises: List<Lift> = emptyList(),
    val units: PreferredUnits = PreferredUnits.METRIC,
    val zone: ZoneId = ZoneId.systemDefault(),
)

/**
 * All sessions (Figma R7-7/8/9): newest first in month blocks, search on
 * workout names, one exercise filter. The query is plain state (05b
 * gotcha), the list follows it.
 */
@HiltViewModel
class AllSessionsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    sources: TemplateSources,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val exerciseId = MutableStateFlow(savedState.get<String>(ARG_EXERCISE_ID)?.takeIf { it.isNotEmpty() })

    private val zone: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<AllSessionsUiState> = combine(
        sources.sessions, sources.units, _query, exerciseId,
    ) { sessions, units, query, filterId ->
        val logged = ProgressStats.loggedExercises(sessions)
        val filter = filterId?.let { id -> logged.firstOrNull { it.exerciseId == id } }
        val shown = ProgressStats.newestFirst(sessions).filter { s ->
            TypoMatcher.match(query, s.title) != null &&
                (filter == null || s.performedExercises.any { it.exerciseId == filter.exerciseId && it.sets.isNotEmpty() })
        }
        val prs = ProgressStats.prSessions(sessions)
        AllSessionsUiState(
            loaded = true,
            hasAny = logged.isNotEmpty() || sessions.any { it.deletedAt == null },
            months = ProgressStats.byMonth(shown, zone).map { g ->
                MonthSection(g.month, g.sessions.map { SessionRow(it, it.id in prs, ProgressStats.topSet(it)) }, g.volumeKg)
            },
            filter = filter,
            exercises = logged,
            units = units,
            zone = zone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AllSessionsUiState())

    fun setQuery(text: String) {
        _query.value = text
    }

    fun setExercise(id: String?) {
        exerciseId.value = id
    }

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"
    }
}

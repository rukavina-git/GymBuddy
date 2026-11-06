package com.rukavina.gymbuddy.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.domain.progress.Lift
import com.rukavina.gymbuddy.domain.progress.LiftProgress
import com.rukavina.gymbuddy.domain.progress.ProgressStats
import com.rukavina.gymbuddy.domain.progress.WeekTotals
import com.rukavina.gymbuddy.ui.template.TemplateSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** A session card (R7-1): PR tag, and the set its subtitle names. */
data class SessionRow(
    val session: WorkoutSession,
    val isPr: Boolean,
    val top: Pair<PerformedExercise, WorkoutSet>?,
)

data class ProgressUiState(
    val loaded: Boolean = false,
    /** No finished session at all (R7-3). */
    val empty: Boolean = true,
    val week: WeekTotals = WeekTotals(0, 0, 0f),
    /** The chart card; null until a lift is eligible (R7-2). */
    val chart: LiftProgress? = null,
    /** Change lift sheet (R7-4). */
    val lifts: List<Lift> = emptyList(),
    val recent: List<SessionRow> = emptyList(),
    val units: PreferredUnits = PreferredUnits.METRIC,
    val zone: ZoneId = ZoneId.systemDefault(),
)

/** Session cards for [sessions] (already filtered/sorted), PRs judged on [all]. */
fun sessionRows(sessions: List<WorkoutSession>, all: List<WorkoutSession>): List<SessionRow> {
    val prs = ProgressStats.prSessions(all)
    return sessions.map { SessionRow(it, it.id in prs, ProgressStats.topSet(it)) }
}

/** Progress tab (Figma R7-1/2/3/4): week line, one lift chart, last 5 sessions. */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    sources: TemplateSources,
    private val preferences: AppPreferencesRepository,
    private val clock: Clock,
) : ViewModel() {

    /** Local days (the injected clock is UTC). */
    private val zone: ZoneId = ZoneId.systemDefault()

    val uiState: StateFlow<ProgressUiState> = combine(
        sources.sessions, sources.units, preferences.progressLiftId,
    ) { sessions, units, storedLift ->
        val lifts = ProgressStats.eligibleLifts(sessions)
        val chart = ProgressStats.chosenLift(lifts, storedLift)?.let { ProgressStats.progress(it, sessions) }
        val newest = ProgressStats.newestFirst(sessions)
        ProgressUiState(
            loaded = true,
            empty = newest.isEmpty(),
            week = ProgressStats.week(sessions, LocalDate.now(clock.withZone(zone)), zone),
            chart = chart,
            lifts = lifts,
            recent = sessionRows(newest.take(ProgressStats.RECENT), sessions),
            units = units,
            zone = zone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    /** Change lift (R7-4): the pick is kept until it stops being eligible. */
    fun chooseLift(exerciseId: String) {
        viewModelScope.launch { preferences.setProgressLiftId(exerciseId) }
    }
}

package com.rukavina.gymbuddy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.usecase.template.TemplateStats
import com.rukavina.gymbuddy.ui.template.TemplateLine
import com.rukavina.gymbuddy.ui.template.TemplateLines
import com.rukavina.gymbuddy.ui.template.TemplateSources
import dagger.hilt.android.lifecycle.HiltViewModel
import com.rukavina.gymbuddy.data.sync.Resync
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val loaded: Boolean = false,
    val week: TemplateStats.Week? = null,
    /** Home rows: 3 most recent, or the active plan's for a new user. */
    val rows: List<TemplateLine> = emptyList(),
    /** Start sheet: templates you made or have done, by recency. */
    val sheetYours: List<TemplateLine> = emptyList(),
    /** Start sheet: the other starter templates, in plan order. */
    val sheetStarters: List<TemplateLine> = emptyList(),
    val today: LocalDate = LocalDate.MIN,
    val zone: ZoneId = ZoneId.systemDefault(),
    /** The week card's backup line (R17-C1b). */
    val backup: HomeBackup = HomeBackup.Hidden,
    /** A 410 re-download: Running/Failed swap the week card and rows (R17-C2a, R2, R3). */
    val resync: Resync = Resync.Idle,
)

/** Home (Figma R11-1/2/3): week strip, Start sheet, recent templates. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    sources: TemplateSources,
    private val clock: Clock,
    private val sync: HomeSyncSignals,
) : ViewModel() {

    private enum class Retry { None, Running, Offline }
    private val retry = MutableStateFlow(Retry.None)

    private val backup = combine(sync.oldestWaiting, retry) { oldest, r ->
        val days = stuckDays(oldest, clock.millis())
        when {
            days == null -> HomeBackup.Hidden
            r == Retry.Running -> HomeBackup.BackingUp
            r == Retry.Offline -> HomeBackup.Offline
            else -> HomeBackup.Stuck(days)
        }
    }

    private val templates = combine(
        sources.templates, sources.sessions, sources.plans, sources.activePlanId,
    ) { templates, sessions, plans, activePlanId ->
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val lines = TemplateLines(sessions, plans)
        val activePlan = plans.firstOrNull { it.id == activePlanId }
        val (yours, starters) = templates.partition { it.source == EntitySource.CUSTOM || it.id in lines.lastDone }
        HomeUiState(
            loaded = true,
            week = TemplateStats.week(sessions, today, zone),
            rows = TemplateStats.homeTemplates(templates, sessions, activePlan).map(lines::line),
            sheetYours = TemplateStats.byRecency(yours, lines.lastDone).map(lines::line),
            sheetStarters = lines.starterOrder(starters).map(lines::line),
            today = today,
            zone = zone,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(templates, backup, sync.resync) { state, b, r ->
        state.copy(backup = b, resync = r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** C1b Retry: back up now (R1), or say it needs internet for 4 s (E3). */
    fun retryBackup() {
        if (retry.value != Retry.None) return
        viewModelScope.launch {
            if (sync.isOnline()) {
                retry.value = Retry.Running
                try { sync.backUpNow() } finally { retry.value = Retry.None }
            } else {
                retry.value = Retry.Offline
                delay(OFFLINE_NOTICE_MS)
                retry.value = Retry.None
            }
        }
    }

    /** R2 Retry. */
    fun retryResync() = sync.retryResync()
}

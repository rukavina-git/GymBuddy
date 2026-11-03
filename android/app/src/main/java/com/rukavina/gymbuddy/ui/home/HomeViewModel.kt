package com.rukavina.gymbuddy.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.usecase.template.TemplateStats
import com.rukavina.gymbuddy.ui.template.TemplateLine
import com.rukavina.gymbuddy.ui.template.TemplateLines
import com.rukavina.gymbuddy.ui.template.TemplateSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
)

/** Home (Figma R11-1/2/3): week strip, Start sheet, recent templates. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    sources: TemplateSources,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

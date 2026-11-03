package com.rukavina.gymbuddy.ui.workouts

import androidx.lifecycle.SavedStateHandle
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
import java.time.ZoneId
import javax.inject.Inject

data class WorkoutsUiState(
    val loaded: Boolean = false,
    val query: String = "",
    /** Templates you made, by recency (never done last). */
    val yours: List<TemplateLine> = emptyList(),
    /** Starter templates, in plan order. */
    val starters: List<TemplateLine> = emptyList(),
    val zone: ZoneId = ZoneId.systemDefault(),
)

/** Workouts tab (Figma 85:10177): your templates, then starter plans. */
@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    sources: TemplateSources,
    private val savedState: SavedStateHandle,
    private val clock: java.time.Clock,
) : ViewModel() {

    private val query = savedState.getStateFlow(KEY_QUERY, "")

    val uiState: StateFlow<WorkoutsUiState> = combine(
        sources.templates, sources.sessions, sources.plans, query,
    ) { templates, sessions, plans, q ->
        val lines = TemplateLines(sessions, plans)
        val shown = if (q.isBlank()) templates else templates.filter { it.title.contains(q.trim(), ignoreCase = true) }
        val (yours, starters) = shown.partition { it.source == EntitySource.CUSTOM }
        WorkoutsUiState(
            loaded = true,
            query = q,
            yours = TemplateStats.byRecency(yours, lines.lastDone).map(lines::line),
            starters = lines.starterOrder(starters).map(lines::line),
            zone = clock.zone,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutsUiState())

    fun setQuery(value: String) {
        savedState[KEY_QUERY] = value
    }

    private companion object {
        const val KEY_QUERY = "query"
    }
}

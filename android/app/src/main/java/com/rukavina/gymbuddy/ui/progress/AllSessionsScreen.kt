package com.rukavina.gymbuddy.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.progress.ProgressStats
import com.rukavina.gymbuddy.ui.design.AppChip
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.SearchField
import com.rukavina.gymbuddy.ui.design.SecondaryButton
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/**
 * All sessions (Figma R7-7 list, R7-8 exercise filter, R7-9 no results):
 * back, search on workout names, Exercise chip, month blocks of cards.
 */
@Composable
fun AllSessionsScreen(
    onBack: () -> Unit,
    onOpenSession: (String) -> Unit,
    viewModel: AllSessionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    AllSessionsContent(
        state = state,
        query = query,
        onQuery = viewModel::setQuery,
        onBack = onBack,
        onPickExercise = { picking = true },
        onClearExercise = { viewModel.setExercise(null) },
        onOpenSession = onOpenSession,
    )
    if (picking) {
        LiftSheet(
            lifts = state.exercises,
            selectedId = state.filter?.exerciseId,
            onPick = { picking = false; viewModel.setExercise(it) },
            onDismiss = { picking = false },
            title = stringResource(R.string.sessions_pick_exercise),
            subtitle = null,
        )
    }
}

@Composable
internal fun AllSessionsContent(
    state: AllSessionsUiState,
    query: String,
    onQuery: (String) -> Unit,
    onBack: () -> Unit,
    onPickExercise: () -> Unit,
    onClearExercise: () -> Unit,
    onOpenSession: (String) -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack, title = stringResource(R.string.progress_all_sessions))
            Column(Modifier.padding(horizontal = d.insetScreen), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
                SearchField(query, onQuery, stringResource(R.string.sessions_search))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val filter = state.filter
                    if (filter == null) {
                        AppChip(stringResource(R.string.sessions_exercise), selected = false, onClick = onPickExercise, dropdown = true)
                    } else {
                        AppChip(filter.name, selected = true, onClick = onPickExercise)
                        AppIconButton(AppIcons.Ui.Close, stringResource(R.string.sessions_clear_exercise), onClearExercise)
                    }
                }
            }
            if (!state.loaded) return@Column
            if (state.months.isEmpty()) {
                NoResults(state, query, { onQuery("") }, onClearExercise)
                return@Column
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                state.months.forEach { section ->
                    item(key = "m-${section.month}") { MonthHeader(section, filtered = state.filter != null, state) }
                    items(section.rows, key = { it.session.id }) { row ->
                        val filter = state.filter
                        val inline = filter?.let {
                            inlineSets(it.name, ProgressStats.setsOf(row.session, it.exerciseId), state.units)
                        }
                        SessionRowCard(row, state.units, state.zone, onOpenSession, subtitle = inline)
                    }
                }
            }
        }
    }
}

/** "October 2026" + "13 sessions · 41,200 kg" (filtered: count only, R7-8). */
@Composable
private fun MonthHeader(section: MonthSection, filtered: Boolean, state: AllSessionsUiState) {
    val d = GymBuddyTheme.dimens
    val count = pluralStringResource(R.plurals.progress_sessions, section.rows.size, section.rows.size)
    Column(Modifier.padding(top = d.stack).semantics(mergeDescendants = true) { heading() }, verticalArrangement = Arrangement.spacedBy(d.hairline)) {
        Text(monthTitle(section.month), style = MaterialTheme.typography.titleMedium)
        Text(
            if (filtered) count else joinDot(count, volumeText(section.volumeKg, state.units)),
            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** R7-9: what was searched, what search looks at, one way out. */
@Composable
private fun NoResults(state: AllSessionsUiState, query: String, onClearSearch: () -> Unit, onClearExercise: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    if (!state.hasAny) return
    Box(Modifier.fillMaxSize().padding(horizontal = d.insetScreen), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(d.stack)) {
            val searching = query.isNotBlank()
            val title = if (searching) stringResource(R.string.sessions_no_match_title, query.trim())
            else stringResource(R.string.sessions_none_title, state.filter?.name.orEmpty())
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            if (searching) {
                Text(stringResource(R.string.sessions_no_match_body), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                SecondaryButton(stringResource(R.string.sessions_clear_search), onClearSearch, Modifier.fillMaxWidth())
            } else {
                SecondaryButton(stringResource(R.string.sessions_clear_exercise), onClearExercise, Modifier.fillMaxWidth())
            }
        }
    }
}

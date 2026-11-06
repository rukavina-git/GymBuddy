package com.rukavina.gymbuddy.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.progress.Lift
import com.rukavina.gymbuddy.domain.progress.LiftProgress
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppChip
import com.rukavina.gymbuddy.ui.design.EmptyState
import com.rukavina.gymbuddy.ui.design.LargeTitleTopBar
import com.rukavina.gymbuddy.ui.design.SecondaryButton
import com.rukavina.gymbuddy.ui.design.SessionCard
import com.rukavina.gymbuddy.ui.design.Sparkline
import com.rukavina.gymbuddy.ui.design.rememberLargeTitleState
import com.rukavina.gymbuddy.ui.template.shortDate
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import java.time.ZoneId

/** Figma note 10: at 200% the chart line drops and the sentence carries the trend. */
internal const val CHART_HIDDEN_FONT_SCALE = 2f

/**
 * Progress tab (Figma R7-1 full, R7-2 no card, R7-3 empty, R7-10 200%):
 * this week's totals, one lift card, last 5 sessions, All sessions.
 */
@Composable
fun ProgressScreen(
    onOpenLift: (String) -> Unit,
    onOpenSession: (String) -> Unit,
    onAllSessions: () -> Unit,
    onStartWorkout: () -> Unit,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var picking by rememberSaveable { mutableStateOf(false) }
    ProgressContent(state, onOpenLift, { picking = true }, onOpenSession, onAllSessions, onStartWorkout)
    if (picking) {
        LiftSheet(
            lifts = state.lifts,
            selectedId = state.chart?.lift?.exerciseId,
            onPick = { picking = false; viewModel.chooseLift(it) },
            onDismiss = { picking = false },
        )
    }
}

@Composable
internal fun ProgressContent(
    state: ProgressUiState,
    onOpenLift: (String) -> Unit,
    onChangeLift: () -> Unit,
    onOpenSession: (String) -> Unit,
    onAllSessions: () -> Unit,
    onStartWorkout: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val titleState = rememberLargeTitleState()
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            LargeTitleTopBar(title = stringResource(R.string.tab_progress), state = titleState)
            if (!state.loaded) return@Column
            if (state.empty) {
                Box(Modifier.fillMaxSize().padding(horizontal = d.insetScreen), contentAlignment = Alignment.Center) {
                    EmptyState(
                        glyph = AppIcons.Ui.TrendingUp,
                        title = stringResource(R.string.progress_empty_title),
                        body = stringResource(R.string.progress_empty_body),
                        actionLabel = stringResource(R.string.progress_start),
                        onAction = onStartWorkout,
                    )
                }
                return@Column
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(titleState.nestedScrollConnection),
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                item(key = "week") { WeekBlock(state) }
                state.chart?.let { chart ->
                    item(key = "chart") { LiftCard(chart, state.units, { onOpenLift(chart.lift.exerciseId) }, onChangeLift) }
                }
                item(key = "recent") {
                    Text(
                        stringResource(R.string.progress_recent),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = d.stack).semantics { heading() },
                    )
                }
                items(state.recent, key = { it.session.id }) { row ->
                    SessionRowCard(row, state.units, state.zone, onOpenSession)
                }
                item(key = "all") {
                    SecondaryButton(
                        stringResource(R.string.progress_all_sessions),
                        onAllSessions,
                        Modifier.fillMaxWidth().padding(top = d.stackTight),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekBlock(state: ProgressUiState) {
    val d = GymBuddyTheme.dimens
    Column(Modifier.padding(top = d.hairline).semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(d.hairline)) {
        Text(stringResource(R.string.progress_this_week), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(weekLine(state.week, state.units), style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
    }
}

/** Shared by Progress and All sessions: title, PR, "64 min · Squat 110 kg × 5", date. */
@Composable
internal fun SessionRowCard(row: SessionRow, units: PreferredUnits, zone: ZoneId, onOpen: (String) -> Unit, subtitle: String? = null) {
    SessionCard(
        title = row.session.title,
        subtitle = subtitle ?: sessionSubtitle(row, units),
        date = shortDate(row.session.startedAt, zone),
        onClick = { onOpen(row.session.id) },
        isPr = row.isPr,
    )
}

/**
 * Chart card (R7-1): the whole card opens the lift's page at Progress;
 * "Change lift" opens the sheet. At 200% (R7-10) the line drops and the
 * chip becomes a full-width button.
 */
@Composable
private fun LiftCard(chart: LiftProgress, units: PreferredUnits, onOpen: () -> Unit, onChange: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val large = LocalDensity.current.fontScale >= CHART_HIDDEN_FONT_SCALE
    Surface(onClick = onOpen, shape = MaterialTheme.shapes.large, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth().padding(top = d.stackTight)) {
        Column(Modifier.padding(d.insetCard), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Text(chart.lift.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    Icon(painterResource(AppIcons.Ui.ChevronRight), null, Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
                }
                if (!large) AppChip(stringResource(R.string.progress_change_lift), selected = false, onClick = onChange, dropdown = true)
            }
            Text(liftSentence(chart, units), style = MaterialTheme.typography.bodyLarge)
            if (large) {
                SecondaryButton(stringResource(R.string.progress_change_lift), onChange, Modifier.fillMaxWidth())
            } else {
                Sparkline(chart.points.map { displayWeight(it.weightKg, units) }, Modifier.padding(vertical = d.hairline))
                Text(heaviestRange(chart, units), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

/** Change lift (R7-4): lifts with 3+ logs, "N sessions", radio; selected is cobalt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LiftSheet(
    lifts: List<Lift>,
    selectedId: String?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.progress_lift_sheet),
    subtitle: String? = stringResource(R.string.progress_lift_sheet_sub),
) {
    val d = GymBuddyTheme.dimens
    val c = GymBuddyTheme.colors
    val cs = MaterialTheme.colorScheme
    AppBottomSheet(onDismiss = onDismiss, title = title, subtitle = subtitle) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
        lifts.forEach { lift ->
            val selected = lift.exerciseId == selectedId
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = d.control)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(lift.exerciseId) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(d.stack),
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = c.cobalt, unselectedColor = cs.outline),
                )
                Text(lift.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    pluralStringResource(R.plurals.progress_sessions, lift.sessionCount, lift.sessionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
        }
    }
}

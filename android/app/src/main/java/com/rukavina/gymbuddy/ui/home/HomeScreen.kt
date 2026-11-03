package com.rukavina.gymbuddy.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.usecase.template.TemplateStats
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.TemplateRow
import com.rukavina.gymbuddy.ui.design.WeekDayState
import com.rukavina.gymbuddy.ui.design.WeekDayUi
import com.rukavina.gymbuddy.ui.design.WeekStrip
import com.rukavina.gymbuddy.ui.template.TemplateLine
import com.rukavina.gymbuddy.ui.template.aboutText
import com.rukavina.gymbuddy.ui.template.exerciseCountText
import com.rukavina.gymbuddy.ui.template.hoursMinutes
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.template.relativeDay
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import java.time.format.TextStyle
import java.util.Locale

/** Raw size (DESIGN.md §4): Home top bar wordmark, Figma R11-1. */
private val WordmarkHome = 24.dp
private val TopBarHeight = 64.dp
/** Start sheet row (Figma R11-3), same min as a list row. */
private val SheetRowHeight = 56.dp

/**
 * Home (Figma R11-1/2/3, 85:10122): week strip, lime Start workout (opens
 * the Start sheet), your 3 most recent templates (play starts one), All
 * templates. While a workout runs, the button and rows hide (the live
 * bar is the way back). No greeting, streaks or prediction (council).
 */
@Composable
fun HomeScreen(
    workoutRunning: Boolean,
    onStartTemplate: (WorkoutTemplate) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onAllTemplates: () -> Unit,
    onOpenWeek: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(state, workoutRunning, onStartTemplate, onOpenTemplate, onAllTemplates, onOpenWeek)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    state: HomeUiState,
    workoutRunning: Boolean,
    onStartTemplate: (WorkoutTemplate) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onAllTemplates: () -> Unit,
    onOpenWeek: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = d.insetScreen),
            verticalArrangement = Arrangement.spacedBy(d.group),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = TopBarHeight).padding(start = d.hairline), contentAlignment = Alignment.CenterStart) {
                Image(
                    painterResource(AppIcons.Brand.Wordmark),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.height(WordmarkHome).semantics { heading() },
                )
            }
            state.week?.let { week ->
                WeekStrip(
                    days = weekDays(week),
                    summary = joinDot(
                        pluralStringResource(R.plurals.week_workouts, week.sessionCount, week.sessionCount),
                        if (week.totalSeconds > 0) hoursMinutes(week.totalSeconds) else null,
                    ),
                    onClick = onOpenWeek,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!workoutRunning) {
                PrimaryButton(
                    text = stringResource(R.string.home_start_workout),
                    onClick = { sheetOpen = true },
                    modifier = Modifier.fillMaxWidth().padding(top = d.hairline),
                )
                if (state.rows.isNotEmpty()) {
                    Text(
                        stringResource(R.string.home_your_templates),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = d.hairline, top = d.stackTight).semantics { heading() },
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
                        state.rows.forEach { line ->
                            TemplateRow(
                                title = line.title,
                                subtitle = rowSubtitle(line, state),
                                onClick = { onOpenTemplate(line.id) },
                                onStart = { onStartTemplate(line.template) },
                            )
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().padding(bottom = d.group), contentAlignment = Alignment.Center) {
                AppLinkButton(stringResource(R.string.home_all_templates), onAllTemplates)
            }
        }
    }
    if (sheetOpen && !workoutRunning) {
        StartSheet(
            state = state,
            onDismiss = { sheetOpen = false },
            onStart = { sheetOpen = false; onStartTemplate(it) },
        )
    }
}

@Composable
private fun rowSubtitle(line: TemplateLine, state: HomeUiState): String =
    joinDot(exerciseCountText(line.exerciseCount), line.lastDone?.let { relativeDay(it, state.today, state.zone) })

/**
 * Start sheet (Figma R11-3): your templates by recency (last done right),
 * then starter templates with a one-liner (plan, day, about N min) and
 * "N ex". "+ Empty workout" waits for 7.6 (the workout can't add
 * exercises yet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSheet(state: HomeUiState, onDismiss: () -> Unit, onStart: (WorkoutTemplate) -> Unit) {
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.start_sheet_title)) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            if (state.sheetYours.isNotEmpty()) {
                SheetHeader(stringResource(R.string.home_your_templates))
                state.sheetYours.forEach { line ->
                    SheetRow(
                        title = line.title,
                        subtitle = null,
                        trailing = line.lastDone?.let { relativeDay(it, state.today, state.zone) },
                        onClick = { onStart(line.template) },
                    )
                }
            }
            if (state.sheetStarters.isNotEmpty()) {
                SheetHeader(stringResource(R.string.starter_plans))
                state.sheetStarters.forEach { line ->
                    SheetRow(
                        title = line.title,
                        subtitle = starterOneLiner(line),
                        trailing = stringResource(R.string.exercise_count_short, line.exerciseCount),
                        onClick = { onStart(line.template) },
                    )
                }
            }
        }
    }
}

/** "Full body plan, day 1 of 3 · about 45 min" (Figma flag: built from data). */
@Composable
internal fun starterOneLiner(line: TemplateLine): String? {
    val plan = line.plan
    val day = line.planDay
    val planText = if (plan != null && day != null) {
        stringResource(R.string.plan_day, plan.title, day, plan.templateIds.size)
    } else null
    return joinDot(planText, aboutText(line.averageMinutes)).ifEmpty { null }
}

@Composable
private fun SheetHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = GymBuddyTheme.dimens.group, bottom = GymBuddyTheme.dimens.hairline).semantics { heading() },
    )
}

@Composable
private fun SheetRow(title: String, subtitle: String?, trailing: String?, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = SheetRowHeight)
            .clickable(onClick = onClick)
            .padding(vertical = d.stackTight),
        horizontalArrangement = Arrangement.spacedBy(d.stack),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
}

/** Week → seven strip days, Monday first, in the phone's language. */
@Composable
private fun weekDays(week: TemplateStats.Week): List<WeekDayUi> {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    return (0L until 7L).map { offset ->
        val date = week.monday.plusDays(offset)
        val done = date in week.doneDays
        val today = date == week.today
        WeekDayUi(
            day = date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
            dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
            state = when {
                today && done -> WeekDayState.TodayDone
                today -> WeekDayState.Today
                done -> WeekDayState.Done
                else -> WeekDayState.Rest
            },
        )
    }
}

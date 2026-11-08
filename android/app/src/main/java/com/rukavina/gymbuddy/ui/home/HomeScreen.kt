package com.rukavina.gymbuddy.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import com.rukavina.gymbuddy.data.sync.Resync
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import com.rukavina.gymbuddy.ui.design.AppTextButton
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
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutUiState
import java.time.Instant
import java.time.ZoneId
import com.rukavina.gymbuddy.ui.design.AppDialog
import com.rukavina.gymbuddy.ui.design.ButtonSize
import com.rukavina.gymbuddy.ui.design.OutlineButton
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    workout: RunningWorkout? = null,
    onOpenWorkout: () -> Unit = {},
    onEmptyWorkout: () -> Unit = {},
    unfinished: UnfinishedActions = UnfinishedActions(),
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state, workoutRunning, onStartTemplate, onOpenTemplate, onAllTemplates, onOpenWeek, workout, onOpenWorkout, onEmptyWorkout, unfinished,
        onRetryBackup = viewModel::retryBackup,
        onRetryResync = viewModel::retryResync,
    )
}

/** Unfinished workout card (R17-E2). Discard confirms first. */
data class UnfinishedActions(
    val onDiscard: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onFinish: () -> Unit = {},
)

/** The running workout for Home's "Workout in progress" card (Figma 53:3414). */
data class RunningWorkout(
    val title: String,
    val startedAt: Long,
    val exercisesDone: Int,
    val exerciseCount: Int,
    val setsDone: Int = 0,
    /** Last write of a workout left 3 h+ ago (R17-E2); null while live. */
    val unfinishedSince: Long? = null,
)

/** Null when no workout runs. An exercise counts as done once all its sets are. */
fun ActiveWorkoutUiState.toRunningWorkout(): RunningWorkout? =
    if (!running) null
    else RunningWorkout(
        title = workoutTitle,
        startedAt = workoutStartTime,
        exercisesDone = exercises.count { it.allDone && it.sets.isNotEmpty() },
        exerciseCount = exercises.size,
        setsDone = doneSets,
        unfinishedSince = unfinishedSince,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    state: HomeUiState,
    workoutRunning: Boolean,
    onStartTemplate: (WorkoutTemplate) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onAllTemplates: () -> Unit,
    onOpenWeek: () -> Unit,
    workout: RunningWorkout? = null,
    onOpenWorkout: () -> Unit = {},
    onEmptyWorkout: () -> Unit = {},
    unfinished: UnfinishedActions = UnfinishedActions(),
    onRetryBackup: () -> Unit = {},
    onRetryResync: () -> Unit = {},
) {
    val d = GymBuddyTheme.dimens
    val resyncing = state.resync != Resync.Idle
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
            if (workoutRunning && workout != null && workout.unfinishedSince == null) {
                InProgressCard(workout, state.zone, onOpenWorkout)
            }
            if (resyncing) {
                ResyncCard(failed = state.resync == Resync.Failed, onRetry = onRetryResync)
            }
            state.week?.takeIf { !resyncing }?.let { week ->
                WeekStrip(
                    days = weekDays(week),
                    summary = joinDot(
                        pluralStringResource(R.plurals.week_workouts, week.sessionCount, week.sessionCount),
                        if (week.totalSeconds > 0) hoursMinutes(week.totalSeconds) else null,
                    ),
                    onClick = onOpenWeek,
                    modifier = Modifier.fillMaxWidth(),
                    footer = if (state.backup == HomeBackup.Hidden) null else {
                        { BackupFooter(state.backup, onRetryBackup) }
                    },
                )
            }
            val since = workout?.unfinishedSince
            if (workoutRunning && workout != null && since != null) {
                UnfinishedCard(workout, since, state.today, state.zone, unfinished)
            }
            if (!workoutRunning) {
                PrimaryButton(
                    text = stringResource(R.string.home_start_workout),
                    onClick = { sheetOpen = true },
                    modifier = Modifier.fillMaxWidth().padding(top = d.hairline),
                )
                if (resyncing) {
                    Text(
                        stringResource(R.string.home_your_templates),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = d.hairline, top = d.stackTight).semantics { heading() },
                    )
                    Text(
                        stringResource(if (state.resync == Resync.Failed) R.string.home_resync_rows_failed else R.string.home_resync_rows_running),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = d.hairline),
                    )
                } else if (state.rows.isNotEmpty()) {
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
            if (!resyncing) {
                Box(Modifier.fillMaxWidth().padding(bottom = d.group), contentAlignment = Alignment.Center) {
                    AppLinkButton(stringResource(R.string.home_all_templates), onAllTemplates)
                }
            }
        }
    }
    if (sheetOpen && !workoutRunning) {
        StartSheet(
            state = state,
            onDismiss = { sheetOpen = false },
            onStart = { sheetOpen = false; onStartTemplate(it) },
            onEmpty = { sheetOpen = false; onEmptyWorkout() },
        )
    }
}

/**
 * The week card's backup line (R17-C1b, R1, E3): one 48dp row, muted.
 * TalkBack reads "Not backed up for 3 days, Retry, button" (notes).
 */
@Composable
private fun BackupFooter(backup: HomeBackup, onRetry: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Row(
        Modifier.fillMaxWidth().heightIn(min = d.target).padding(horizontal = d.group),
        horizontalArrangement = Arrangement.spacedBy(d.stackTight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (backup == HomeBackup.BackingUp) {
            Spinner(Modifier.size(d.iconSmall))
        } else {
            Icon(painterResource(AppIcons.Ui.CloudOff), null, Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
        }
        val text = when (backup) {
            is HomeBackup.Stuck -> pluralStringResource(R.plurals.home_not_backed_up, backup.days, backup.days)
            HomeBackup.BackingUp -> stringResource(R.string.home_backing_up)
            HomeBackup.Offline -> stringResource(R.string.home_backup_offline)
            HomeBackup.Hidden -> ""
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (backup is HomeBackup.Stuck) AppLinkButton(stringResource(R.string.home_retry), onRetry)
    }
}

/** R17-C2a / R2: in place of the week card while a re-download runs or after it failed. */
@Composable
private fun ResyncCard(failed: Boolean, onRetry: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Surface(shape = MaterialTheme.shapes.large, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(d.insetCard).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(d.hairline),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight), verticalAlignment = Alignment.CenterVertically) {
                if (failed) {
                    Icon(painterResource(AppIcons.Ui.CloudOff), null, Modifier.size(d.icon), tint = cs.onSurfaceVariant)
                } else {
                    Spinner(Modifier.size(d.icon))
                }
                Text(
                    stringResource(if (failed) R.string.home_resync_failed else R.string.home_resync_running),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                if (failed) AppLinkButton(stringResource(R.string.home_retry), onRetry)
            }
            Text(
                stringResource(if (failed) R.string.home_resync_failed_body else R.string.home_resync_running_body),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

/** The spinner glyph, turning (stays still with animations off, like the system). */
@Composable
private fun Spinner(modifier: Modifier) {
    val turn by rememberInfiniteTransition(label = "spinner").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(SPIN_MS, easing = LinearEasing)),
        label = "turn",
    )
    Icon(
        painterResource(AppIcons.Ui.Spinner),
        null,
        modifier.graphicsLayer { rotationZ = turn },
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val SPIN_MS = 1_000

/** "Workout in progress · Push A · Started 18:24 · 2 of 15 exercises done"; tap opens it. */
@Composable
private fun InProgressCard(workout: RunningWorkout, zone: ZoneId, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val started = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        .format(Instant.ofEpochMilli(workout.startedAt).atZone(zone))
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = cs.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(d.insetCard), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
            Text(stringResource(R.string.home_in_progress), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            Text(workout.title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.home_in_progress_line, started, workout.exercisesDone, workout.exerciseCount),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

/**
 * R17-E2 / F25: no set for 2 h. "Push A · 12 sets", "Last set
 * yesterday at 19:02"; Resume · Finish (lime, at the last set). With no
 * sets: Discard (confirms) · Resume.
 */
@Composable
private fun UnfinishedCard(workout: RunningWorkout, since: Long, today: LocalDate, zone: ZoneId, actions: UnfinishedActions) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        .format(Instant.ofEpochMilli(since).atZone(zone))
    var confirm by rememberSaveable { mutableStateOf(false) }
    Surface(shape = MaterialTheme.shapes.large, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(d.insetCard), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
            Text(
                stringResource(R.string.home_unfinished),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                pluralStringResource(R.plurals.home_unfinished_line, workout.setsDone, workout.title, workout.setsDone),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(
                    if (workout.setsDone > 0) R.string.home_unfinished_last else R.string.home_unfinished_started,
                    relativeDay(since, today, zone), time,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = d.stack),
                horizontalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                // F25: Finish ends at the last set; with no sets there is nothing to finish, only Discard.
                if (workout.setsDone == 0) {
                    OutlineButton(stringResource(R.string.workout_discard), { confirm = true }, Modifier.weight(1f), size = ButtonSize.Small)
                }
                OutlineButton(stringResource(R.string.home_unfinished_resume), actions.onResume, Modifier.weight(1f), size = ButtonSize.Small)
                if (workout.setsDone > 0) {
                    PrimaryButton(stringResource(R.string.home_unfinished_finish), actions.onFinish, Modifier.weight(1f), size = ButtonSize.Small)
                }
            }
        }
    }
    if (confirm) {
        AppDialog(
            title = stringResource(R.string.workout_discard_title),
            body = stringResource(R.string.workout_discard_body),
            confirmLabel = stringResource(R.string.workout_discard),
            onConfirm = { confirm = false; actions.onDiscard() },
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = { confirm = false },
            destructive = true,
        )
    }
}

@Composable
private fun rowSubtitle(line: TemplateLine, state: HomeUiState): String =
    joinDot(exerciseCountText(line.exerciseCount), line.lastDone?.let { relativeDay(it, state.today, state.zone) })

/**
 * Start sheet (Figma R11-3): your templates by recency (last done right),
 * then starter templates with a one-liner (plan, day, about N min) and
 * "N ex". A quiet "Start an empty workout" is pinned under the list
 * (F24a): templates first, and the list scrolls, so last-in-list would bury it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSheet(state: HomeUiState, onDismiss: () -> Unit, onStart: (WorkoutTemplate) -> Unit, onEmpty: () -> Unit) {
    AppBottomSheet(onDismiss = onDismiss, title = stringResource(R.string.start_sheet_title)) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            if (state.resync != Resync.Idle) {
                // R3: yours are still on their way; starters and Empty work.
                SheetHeader(stringResource(R.string.home_your_templates))
                Text(
                    stringResource(R.string.start_sheet_resync),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = GymBuddyTheme.dimens.stackTight),
                )
            } else if (state.sheetYours.isNotEmpty()) {
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
        AppTextButton(stringResource(R.string.start_sheet_empty), onEmpty, Modifier.fillMaxWidth())
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

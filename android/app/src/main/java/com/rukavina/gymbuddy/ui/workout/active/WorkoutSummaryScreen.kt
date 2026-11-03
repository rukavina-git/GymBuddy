package com.rukavina.gymbuddy.ui.workout.active

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.AppTextButton
import com.rukavina.gymbuddy.ui.design.ListRow
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.StatTile
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel
import com.rukavina.gymbuddy.ui.workout.SetField
import com.rukavina.gymbuddy.ui.workout.SetFields
import com.rukavina.gymbuddy.ui.workout.SummaryExercise
import com.rukavina.gymbuddy.ui.workout.WorkoutSummary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Rows shown before "+ N more" (Figma 53:3697). */
private const val SUMMARY_ROWS = 5

/**
 * After Finish (Figma 53:3697): title, day and times, time / sets /
 * lifted, what you did, the guest backup card, lime Done. Done and Back
 * both close it ([onDone]).
 */
@Composable
fun WorkoutSummaryScreen(
    viewModel: ActiveWorkoutViewModel,
    onDone: () -> Unit,
    onCreateAccount: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val summary = state.summary
    val close = { viewModel.closeSummary(); onDone() }
    BackHandler { close() }
    if (summary == null) return
    WorkoutSummaryContent(
        summary = summary,
        onDone = close,
        onCreateAccount = onCreateAccount,
        onNotNow = viewModel::dismissBackup,
    )
}

@Composable
internal fun WorkoutSummaryContent(
    summary: WorkoutSummary,
    onDone: () -> Unit,
    onCreateAccount: () -> Unit,
    onNotNow: () -> Unit,
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    var showAll by rememberSaveable { mutableStateOf(false) }
    val rows = if (showAll) summary.exercises else summary.exercises.take(SUMMARY_ROWS)
    Scaffold(
        containerColor = cs.background,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.summary_done),
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = d.insetScreen, vertical = d.group),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = d.insetScreen, vertical = d.section),
            verticalArrangement = Arrangement.spacedBy(d.stackTight),
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = d.section),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(d.stackTight),
                ) {
                    Text(
                        stringResource(R.string.summary_title, summary.title),
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(whenText(summary, zone), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                    StatTile(stringResource(R.string.workout_minutes, summary.durationSeconds / 60), stringResource(R.string.summary_time), Modifier.weight(1f))
                    StatTile(summary.sets.toString(), stringResource(R.string.summary_sets), Modifier.weight(1f))
                    StatTile(
                        "${wholeNumber(summary.lifted)} ${unitText(SetField.Weight, summary.units)}",
                        stringResource(R.string.summary_lifted),
                        Modifier.weight(1f),
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.summary_what_you_did),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = d.section, bottom = d.hairline),
                )
            }
            items(rows) { ex -> ListRow(ex.name, subtitle = exerciseLine(ex, summary)) }
            val more = summary.exercises.size - rows.size
            if (more > 0) {
                item {
                    AppTextButton(stringResource(R.string.summary_more, more), { showAll = true })
                }
            }
            if (summary.offerBackup) {
                item { BackupCard(onCreateAccount, onNotNow, Modifier.padding(top = d.group)) }
            }
        }
    }
}

/** "Saturday · 18:24 – 19:16". */
@Composable
private fun whenText(summary: WorkoutSummary, zone: ZoneId): String {
    val start = Instant.ofEpochMilli(summary.startedAt).atZone(zone)
    val end = Instant.ofEpochMilli(summary.endedAt).atZone(zone)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val day = start.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return joinDot(day, "${start.format(time)} – ${end.format(time)}")
}

/** "4 sets · top 82.5 kg × 8"; timed or distance sets say "best". */
@Composable
private fun exerciseLine(ex: SummaryExercise, summary: WorkoutSummary): String {
    val value = setValueText(ex.trackingType, ex.top, summary.units)
    val best = if (SetField.Weight in SetFields.of(ex.trackingType)) {
        stringResource(R.string.summary_top, value)
    } else {
        stringResource(R.string.summary_best, value)
    }
    return joinDot(pluralStringResource(R.plurals.workout_sets, ex.setCount, ex.setCount), best)
}

@Composable
private fun BackupCard(onCreateAccount: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = cs.surfaceContainerHigh, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(d.insetCard), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
            Row(horizontalArrangement = Arrangement.spacedBy(d.stack), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(AppIcons.Ui.Cloud), null, Modifier.size(d.icon))
                Text(stringResource(R.string.summary_backup_title), style = MaterialTheme.typography.titleSmall)
            }
            Text(stringResource(R.string.summary_backup_body), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
                AppLinkButton(stringResource(R.string.summary_create_account), onCreateAccount)
                AppTextButton(stringResource(R.string.summary_not_now), onNotNow)
            }
        }
    }
}

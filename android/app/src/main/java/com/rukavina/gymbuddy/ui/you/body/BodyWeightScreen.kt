package com.rukavina.gymbuddy.ui.you.body

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.BodyWeightEntry
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.progress.ProgressStats
import com.rukavina.gymbuddy.ui.design.AppBottomSheet
import com.rukavina.gymbuddy.ui.design.AppChip
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.EmptyState
import com.rukavina.gymbuddy.ui.design.Keypad
import com.rukavina.gymbuddy.ui.design.LiftChart
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.ValuePanel
import com.rukavina.gymbuddy.ui.design.keypadSeparator
import com.rukavina.gymbuddy.ui.theme.Haptic
import com.rukavina.gymbuddy.ui.theme.rememberHaptics
import com.rukavina.gymbuddy.ui.progress.dayMonth
import com.rukavina.gymbuddy.ui.progress.displayWeight
import com.rukavina.gymbuddy.ui.progress.tickText
import com.rukavina.gymbuddy.ui.progress.weightText
import com.rukavina.gymbuddy.ui.template.shortDate
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.utils.UnitConverter
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private const val CHART_HIDDEN_FONT_SCALE = 2f

/** Body weight (R15-5) with the Log weight sheet (R15-6). */
@Composable
fun BodyWeightScreen(onBack: () -> Unit, viewModel: BodyWeightViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BodyWeightEvent.Deleted -> {
                    val result = snackbar.showSnackbar(
                        message = context.getString(R.string.body_weight_deleted),
                        actionLabel = context.getString(R.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.entry)
                }
                BodyWeightEvent.SaveFailed -> snackbar.showSnackbar(context.getString(R.string.body_weight_save_failed))
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        BodyWeightContent(
            state = state,
            now = Clock.systemUTC().millis(),
            onBack = onBack,
            onRange = viewModel::setRange,
            onDelete = viewModel::delete,
            onLog = viewModel::openLog,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) { AppSnackbar(it) }
    }
    log?.let { LogWeightSheet(it, state.units, viewModel) }
}

@Composable
internal fun BodyWeightContent(
    state: BodyWeightUiState,
    now: Long,
    onBack: () -> Unit,
    onRange: (WeightRange) -> Unit,
    onDelete: (BodyWeightEntry) -> Unit,
    onLog: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val zone = remember { ZoneId.systemDefault() }
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack)
            if (state.loading) return@Column
            val shown = BodyWeightStats.inRange(state.entries, state.range, now)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                item(key = "title") {
                    Text(
                        stringResource(R.string.body_weight_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                val latest = state.latest
                if (latest == null) {
                    item(key = "empty") {
                        Box(Modifier.fillMaxWidth().padding(top = d.section), contentAlignment = Alignment.Center) {
                            EmptyState(
                                glyph = AppIcons.Glyph.Scale,
                                title = stringResource(R.string.body_weight_empty),
                                body = stringResource(R.string.body_weight_empty_body),
                            )
                        }
                    }
                    return@LazyColumn
                }
                item(key = "latest") {
                    Column {
                        Text(weightText(latest.weightKg, state.units), style = MaterialTheme.typography.displayMedium)
                        changeText(BodyWeightStats.change(state.entries), state.units)?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        }
                    }
                }
                item(key = "ranges") {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = d.stackTight),
                        horizontalArrangement = Arrangement.spacedBy(d.stackTight),
                    ) {
                        WeightRange.entries.forEach { r ->
                            AppChip(label = rangeLabel(r), selected = r == state.range, onClick = { onRange(r) })
                        }
                    }
                }
                item(key = "chart") { WeightChartCard(shown, state.units, zone) }
                items(shown, key = { it.id }) { entry ->
                    EntryRow(entry, state.units, zone, onDelete)
                }
            }
            PrimaryButton(
                stringResource(R.string.body_weight_log),
                onLog,
                Modifier.fillMaxWidth().padding(horizontal = d.insetScreen, vertical = d.stack),
            )
        }
    }
}

@Composable
private fun rangeLabel(r: WeightRange): String = stringResource(
    when (r) {
        WeightRange.Days30 -> R.string.body_weight_range_30
        WeightRange.Days90 -> R.string.body_weight_range_90
        WeightRange.Year -> R.string.body_weight_range_year
        WeightRange.All -> R.string.body_weight_range_all
    },
)

/** "1.6 kg less than 30 days ago" / "… more …" / "Same as 30 days ago"; null without an old enough entry. */
@Composable
internal fun changeText(changeKg: Float?, units: PreferredUnits): String? {
    if (changeKg == null) return null
    val shown = UnitConverter.weightToDisplayUnit(abs(changeKg), units)
    return when {
        shown == "0" || BodyWeightViewModel.round1(displayWeight(abs(changeKg), units)) == 0f ->
            stringResource(R.string.body_weight_same)
        changeKg > 0 -> stringResource(R.string.body_weight_more, oneDecimal(abs(changeKg), units))
        else -> stringResource(R.string.body_weight_less, oneDecimal(abs(changeKg), units))
    }
}

@Composable
private fun oneDecimal(kg: Float, units: PreferredUnits): String =
    stringResource(R.string.weight_value, number1(displayWeight(kg, units)), UnitConverter.getWeightUnitLabel(units))

/** One decimal at most, locale separator: 82.4, 82. */
internal fun number1(v: Float): String {
    val r = BodyWeightViewModel.round1(v)
    return if (r == r.toInt().toFloat()) r.toInt().toString() else String.format(Locale.getDefault(), "%.1f", r)
}

@Composable
private fun WeightChartCard(shown: List<BodyWeightEntry>, units: PreferredUnits, zone: ZoneId) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    if (LocalDensity.current.fontScale >= CHART_HIDDEN_FONT_SCALE) return
    Surface(shape = MaterialTheme.shapes.large, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth().padding(vertical = d.stackTight)) {
        Box(Modifier.padding(d.insetCard)) {
            if (shown.size < 2) {
                Text(stringResource(R.string.body_weight_chart_few), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                return@Surface
            }
            val points = shown.asReversed()
            val values = points.map { displayWeight(it.weightKg, units) }
            val dates = points.map { dayMonth(it.measuredAt, zone) }
            val xLabels = if (dates.size < 3) listOf(dates.first(), dates.last()) else listOf(dates.first(), dates[dates.size / 2], dates.last())
            val texts = points.map { weightLine(it.weightKg, units) }
            LiftChart(
                values = values,
                yTicks = ProgressStats.yTicks(values.min(), values.max()),
                yLabel = ::tickText,
                xLabels = xLabels,
                readout = { i -> dates[i] to texts[i] },
                summary = stringResource(R.string.body_weight_chart_summary, dates.first(), texts.first(), dates.last(), texts.last()),
            )
        }
    }
}

@Composable
private fun weightLine(kg: Float, units: PreferredUnits): String =
    stringResource(R.string.weight_value, number1(displayWeight(kg, units)), UnitConverter.getWeightUnitLabel(units))

/** "Sun 1 Nov … 82.4 kg"; swipe left deletes (Undo on the snackbar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryRow(entry: BodyWeightEntry, units: PreferredUnits, zone: ZoneId, onDelete: (BodyWeightEntry) -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val haptics = rememberHaptics()
    val swipe = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipe.currentValue) {
        if (swipe.currentValue != SwipeToDismissBoxValue.Settled) {
            haptics(Haptic.SwipeThreshold)
            onDelete(entry)
        }
    }
    val remove = stringResource(R.string.a11y_remove)
    SwipeToDismissBox(
        state = swipe,
        enableDismissFromStartToEnd = false,
        modifier = Modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(remove) { onDelete(entry); true })
        },
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(c.dangerSwipe, MaterialTheme.shapes.medium).padding(horizontal = d.section),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(painterResource(AppIcons.Ui.Delete), null, Modifier.size(d.icon), tint = cs.onSurface)
            }
        },
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.heightIn(min = d.control).padding(horizontal = d.insetCard),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(shortDate(entry.measuredAt, zone), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(weightLine(entry.weightKg, units), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** Log weight (R15-6): − / + 0.1, tap the number for the keypad, date row, Save. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogWeightSheet(log: LogState, units: PreferredUnits, viewModel: BodyWeightViewModel) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val unit = UnitConverter.getWeightUnitLabel(units)
    var picking by rememberSaveable { mutableStateOf(false) }
    AppBottomSheet(onDismiss = viewModel::closeLog, title = stringResource(R.string.body_weight_log)) {
        val typed = log.keypad
        if (typed != null) {
            Keypad(
                label = stringResource(R.string.body_weight_title),
                value = typed.replace('.', keypadSeparator()),
                unit = unit,
                onInput = viewModel::keypadInput,
                nextLabel = stringResource(R.string.body_weight_keypad_done),
                message = if (log.keypadError) stringResource(R.string.body_weight_out_of_range) else null,
            )
            return@AppBottomSheet
        }
        ValuePanel(
            number = number1(log.value),
            unit = unit,
            editing = false,
            onClick = viewModel::openKeypad,
            onMinus = { viewModel.step(up = false) },
            onPlus = { viewModel.step(up = true) },
            modifier = Modifier.fillMaxWidth(),
            minusEnabled = viewModel.canStep(up = false),
            plusEnabled = viewModel.canStep(up = true),
            minusDescription = stringResource(R.string.body_weight_decrease),
            plusDescription = stringResource(R.string.body_weight_increase),
        )
        Text(stringResource(R.string.body_weight_hint, unit), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        Surface(
            onClick = { picking = true },
            shape = MaterialTheme.shapes.medium,
            color = cs.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(top = d.stackTight),
        ) {
            Row(
                Modifier.heightIn(min = d.control).padding(horizontal = d.insetCard),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.body_weight_date), style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(dateText(log.date), style = MaterialTheme.typography.titleSmall)
                Icon(painterResource(AppIcons.Ui.ChevronRight), null, Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
            }
        }
        PrimaryButton(stringResource(R.string.you_save), viewModel::save, Modifier.fillMaxWidth().padding(top = d.stackTight))
    }
    if (picking) {
        LogDateDialog(log.date, onPicked = { viewModel.setDate(it); picking = false }, onDismiss = { picking = false })
    }
}

/** "Today, Mon 2 Nov" or "Sun 1 Nov". */
@Composable
private fun dateText(date: LocalDate): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEdMMM")
    val text = DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(date)
    return if (date == LocalDate.now()) stringResource(R.string.body_weight_today, text) else text
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogDateDialog(date: LocalDate, onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // DatePicker works in UTC midnight millis; no future days.
    val today = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val picker = rememberDatePickerState(
        initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            AppLinkButton(stringResource(R.string.session_edit_ok), {
                val millis = picker.selectedDateMillis
                if (millis == null) onDismiss() else onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
            })
        },
        dismissButton = { AppLinkButton(stringResource(R.string.cancel), onDismiss) },
    ) {
        DatePicker(picker)
    }
}

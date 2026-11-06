package com.rukavina.gymbuddy.ui.progress.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.progress.DetailExercise
import com.rukavina.gymbuddy.domain.progress.SessionTiles
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.AppMenu
import com.rukavina.gymbuddy.ui.design.AppMenuItem
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.PrTag
import com.rukavina.gymbuddy.ui.design.StatTile
import com.rukavina.gymbuddy.ui.exercises.page.setText
import com.rukavina.gymbuddy.ui.progress.volumeText
import com.rukavina.gymbuddy.ui.template.clock
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.Durations
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.theme.motionTween
import com.rukavina.gymbuddy.ui.workout.SetField
import com.rukavina.gymbuddy.ui.workout.SetFields
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

/** How long a card opened from an exercise's history stays outlined (R14-3). */
private const val HIGHLIGHT_MS = 1_500L

private val IndexColumn = 20.dp

@Composable
fun SessionDetailScreen(
    workoutRunning: Boolean,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onStartAgain: (WorkoutTemplate) -> Unit,
    onDelete: (id: String, title: String) -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Gone (deleted elsewhere, wrong id): nothing to show.
    LaunchedEffect(state.loaded, state.session == null) {
        if (state.loaded && state.session == null) onBack()
    }
    SessionDetailContent(
        state = state,
        highlightExerciseId = viewModel.highlightExerciseId,
        workoutRunning = workoutRunning,
        onBack = onBack,
        onOpenExercise = onOpenExercise,
        onStartAgain = onStartAgain,
        onDelete = onDelete,
    )
}

@Composable
internal fun SessionDetailContent(
    state: SessionDetailUiState,
    highlightExerciseId: String?,
    workoutRunning: Boolean,
    onBack: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onStartAgain: (WorkoutTemplate) -> Unit,
    onDelete: (id: String, title: String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val session = state.session
    var menuOpen by remember { mutableStateOf(false) }
    // Performed-exercise ids whose sets are open (ArrayList: saveable).
    var open by rememberSaveable { mutableStateOf(ArrayList<String>()) }
    var highlightHandled by rememberSaveable { mutableStateOf(false) }
    var outlined by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val headerItems = if (session?.notes.isNullOrBlank()) 2 else 3

    LaunchedEffect(state.loaded, session?.id) {
        if (highlightHandled || session == null || highlightExerciseId == null) return@LaunchedEffect
        highlightHandled = true
        val index = state.exercises.indexOfFirst { it.performed.exerciseId == highlightExerciseId }
        if (index < 0) return@LaunchedEffect
        val id = state.exercises[index].performed.id
        open = ArrayList(open + id)
        listState.animateScrollToItem(headerItems + index)
        outlined = id
        delay(HIGHLIGHT_MS)
        outlined = null
    }

    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack) {
                if (session != null) {
                    Box {
                        AppIconButton(AppIcons.Ui.More, stringResource(R.string.cd_more), { menuOpen = true })
                        AppMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                            val template = state.template
                            // Start is hidden while a workout runs, like Home (Figma 85:10122).
                            if (template != null && !workoutRunning) {
                                AppMenuItem(
                                    stringResource(R.string.session_start_again, template.title),
                                    { menuOpen = false; onStartAgain(template) },
                                    AppIcons.Ui.Add,
                                )
                            }
                            AppMenuItem(
                                stringResource(R.string.menu_delete),
                                { menuOpen = false; onDelete(session.id, session.title) },
                                AppIcons.Ui.Delete,
                                danger = true,
                            )
                        }
                    }
                }
            }
            if (session == null) return@Column
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.group),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                item(key = "head") {
                    Column(Modifier.padding(start = d.hairline), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                        Text(session.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                        Text(whenText(session, state.zone), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
                item(key = "tiles") { state.tiles?.let { Tiles(it, state.units) } }
                session.notes?.takeIf { it.isNotBlank() }?.let { note ->
                    item(key = "note") {
                        Text(
                            note,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = d.hairline, vertical = d.stackTight),
                        )
                    }
                }
                itemsIndexed(state.exercises, key = { _, e -> e.performed.id }) { _, exercise ->
                    val id = exercise.performed.id
                    ExerciseCard(
                        exercise = exercise,
                        units = state.units,
                        expanded = id in open,
                        outlined = outlined == id,
                        onToggle = { open = if (id in open) ArrayList(open - id) else ArrayList(open + id) },
                        onOpenExercise = { onOpenExercise(exercise.performed.exerciseId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Tiles(tiles: SessionTiles, units: PreferredUnits) {
    val d = GymBuddyTheme.dimens
    Row(horizontalArrangement = Arrangement.spacedBy(d.stackTight)) {
        val tile = Modifier.weight(1f)
        StatTile(stringResource(R.string.workout_minutes, tiles.seconds / 60), stringResource(R.string.summary_time), tile)
        when (tiles) {
            is SessionTiles.Lifted -> {
                StatTile(tiles.sets.toString(), stringResource(R.string.summary_sets), tile)
                StatTile(volumeText(tiles.liftedKg, units), stringResource(R.string.summary_lifted), tile)
            }
            is SessionTiles.Distance -> {
                StatTile(distanceText(tiles.meters, units), stringResource(R.string.session_distance), tile)
                tiles.secondsPerKm?.let { perKm ->
                    val perUnit = if (units == PreferredUnits.IMPERIAL) perKm * KM_PER_MILE else perKm
                    val label = if (units == PreferredUnits.IMPERIAL) R.string.session_per_mi else R.string.session_per_km
                    StatTile(clock(perUnit.roundToInt()), stringResource(label), tile)
                }
            }
            is SessionTiles.Plain -> StatTile(tiles.sets.toString(), stringResource(R.string.summary_sets), tile)
        }
    }
}

private const val KM_PER_MILE = 1.609344f

/** "5.2 km" / "3.2 mi": one decimal, trimmed. */
@Composable
private fun distanceText(meters: Float, units: PreferredUnits): String {
    val imperial = units == PreferredUnits.IMPERIAL
    val value = if (imperial) meters / 1000f / KM_PER_MILE else meters / 1000f
    val rounded = (value * 10).roundToInt() / 10f
    val text = if (rounded == rounded.roundToInt().toFloat()) rounded.roundToInt().toString()
    else String.format(Locale.getDefault(), "%.1f", rounded)
    return stringResource(if (imperial) R.string.session_mi else R.string.session_km, text)
}

/** "Mon 26 Oct · 18:24–19:16" (R14-1). */
@Composable
private fun whenText(session: WorkoutSession, zone: ZoneId): String {
    val locale = Locale.getDefault()
    val start = Instant.ofEpochMilli(session.startedAt).atZone(zone)
    val day = DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale).format(start)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    val range = session.endedAt?.let { end ->
        stringResource(R.string.session_time_range, start.format(time), Instant.ofEpochMilli(end).atZone(zone).format(time))
    } ?: start.format(time)
    return joinDot(day, range)
}

/** "4 sets · top 82.5 kg × 8"; without weight "best …" (decision 4). */
@Composable
private fun exerciseLine(exercise: DetailExercise, units: PreferredUnits): String {
    val type = exercise.performed.exerciseTrackingType
    val count = pluralStringResource(R.plurals.workout_sets, exercise.sets.size, exercise.sets.size)
    val top = exercise.top ?: return count
    val value = setText(type, top, units)
    val best = if (SetField.Weight in SetFields.of(type) && (top.weightKg ?: 0f) > 0f) {
        stringResource(R.string.summary_top, value)
    } else {
        stringResource(R.string.summary_best, value)
    }
    return joinDot(count, best)
}

/** One exercise (R14-1): name, line, PR pill; tap opens its sets and a link to its page. */
@Composable
private fun ExerciseCard(
    exercise: DetailExercise,
    units: PreferredUnits,
    expanded: Boolean,
    outlined: Boolean,
    onToggle: () -> Unit,
    onOpenExercise: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val type = exercise.performed.exerciseTrackingType
    val state = stringResource(if (expanded) R.string.state_expanded else R.string.state_collapsed)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceContainer,
        border = if (outlined) BorderStroke(d.strokeEmphasis, GymBuddyTheme.colors.cobalt) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = d.target)
                    .clickable(role = Role.Button, onClick = onToggle)
                    .semantics(mergeDescendants = true) { stateDescription = state }
                    .padding(horizontal = d.insetCard, vertical = d.stack),
                horizontalArrangement = Arrangement.spacedBy(d.stackTight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                    Text(exercise.performed.exerciseName, style = MaterialTheme.typography.titleSmall)
                    Text(exerciseLine(exercise, units), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
                if (exercise.isPr) PrTag()
                Icon(
                    painterResource(if (expanded) AppIcons.Ui.ChevronUp else AppIcons.Ui.ChevronDown),
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(d.iconSmall),
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(motionTween(Durations.MEDIUM2)) + fadeIn(motionTween(Durations.MEDIUM2)),
                exit = shrinkVertically(motionTween(Durations.SHORT4)) + fadeOut(motionTween(Durations.SHORT4)),
            ) {
                Column(
                    Modifier.padding(start = d.insetCard, end = d.stackTight, bottom = d.stackTight),
                    verticalArrangement = Arrangement.spacedBy(d.hairline),
                ) {
                    exercise.sets.forEachIndexed { i, set ->
                        val number = stringResource(R.string.session_set_number, i + 1)
                        Row(
                            Modifier.semantics(mergeDescendants = true) {},
                            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
                        ) {
                            Text(
                                "${i + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.widthIn(min = IndexColumn).semantics { contentDescription = number },
                            )
                            Text(setText(type, set, units), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    AppLinkButton(
                        stringResource(R.string.session_exercise_page, exercise.performed.exerciseName),
                        onOpenExercise,
                        Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

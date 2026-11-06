package com.rukavina.gymbuddy.ui.exercises.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.exercise.ExerciseHistoryEntry
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.AppLinkButton
import com.rukavina.gymbuddy.ui.design.AppMenu
import com.rukavina.gymbuddy.ui.design.AppMenuItem
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.exercise.equipmentLabel
import com.rukavina.gymbuddy.ui.exercise.muscleLabel
import com.rukavina.gymbuddy.ui.exercises.exerciseSubtitle
import com.rukavina.gymbuddy.ui.template.clock
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.template.shortDate
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.workout.SetFields
import com.rukavina.gymbuddy.ui.workout.active.setValueText
import com.rukavina.gymbuddy.utils.UnitConverter
import java.time.ZoneId
import com.rukavina.gymbuddy.ui.workout.WorkoutSet as UiSet

/** Raw size (DESIGN.md §4): the bundled photo's frame on R13-1 (384 × 216). */
private const val PHOTO_RATIO = 16f / 9f

/** History rows on the page; the full list is All sessions (7.8). */
private const val HISTORY_ROWS = 3

private enum class PageSheet { Rest, Note, AddToTemplate }

/**
 * Exercise page (Figma R13-1, R13-7, R13-8, menus R13-9): back and ⋯,
 * photo (built-in only), name, Last / Best, quiet rest row, your note,
 * history, how to, tips, muscles and equipment. Nothing empty shows.
 */
@Composable
fun ExercisePageScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onMessage: (String) -> Unit,
    viewModel: ExercisePageViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var sheet by rememberSaveable { mutableStateOf<PageSheet?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ExercisePageEvent.Gone -> onBack()
                is ExercisePageEvent.Hidden -> {
                    onBack()
                    onMessage(context.getString(R.string.exercise_hidden_done, event.name))
                }
                is ExercisePageEvent.Deleted -> {
                    onBack()
                    onMessage(context.getString(R.string.exercise_deleted_done, event.name))
                }
                is ExercisePageEvent.AddedTo -> onMessage(context.getString(R.string.exercise_added_to, event.title))
                ExercisePageEvent.Failed -> onMessage(context.getString(R.string.exercise_action_failed))
            }
        }
    }
    ExercisePageContent(
        state = state,
        onBack = onBack,
        onRest = { sheet = PageSheet.Rest },
        onNote = { sheet = PageSheet.Note },
        onEdit = { onEdit(it) },
        onAddToTemplate = { sheet = PageSheet.AddToTemplate },
        onHide = viewModel::hide,
        onDelete = { confirmDelete = true },
    )
    val exercise = state.exercise ?: return
    when (sheet) {
        PageSheet.Rest -> ExerciseRestSheet(
            exerciseName = exercise.name,
            seconds = state.user.restSeconds,
            defaultSeconds = com.rukavina.gymbuddy.ui.template.editor.TemplateEditorViewModel.DEFAULT_REST_SECONDS,
            onPick = { sheet = null; viewModel.setRest(it) },
            onDismiss = { sheet = null },
        )
        PageSheet.Note -> ExerciseNoteSheet(
            exerciseName = exercise.name,
            note = state.user.note,
            editing = true,
            onSave = { sheet = null; viewModel.setNote(it) },
            onDismiss = { sheet = null },
        )
        PageSheet.AddToTemplate -> AddToTemplateSheet(
            templates = state.templates,
            onPick = { sheet = null; viewModel.addToTemplate(it) },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
    if (confirmDelete) {
        DeleteExerciseDialog(
            name = exercise.name,
            usedIn = state.usedIn,
            onDelete = { confirmDelete = false; viewModel.delete() },
            onHide = { confirmDelete = false; viewModel.hide() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
internal fun ExercisePageContent(
    state: ExercisePageUiState,
    onBack: () -> Unit,
    onRest: () -> Unit,
    onNote: () -> Unit,
    onEdit: (String) -> Unit,
    onAddToTemplate: () -> Unit,
    onHide: () -> Unit,
    onDelete: () -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }
    val exercise = state.exercise
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack) {
                if (exercise != null) {
                    Box {
                        AppIconButton(AppIcons.Ui.More, stringResource(R.string.cd_more), { menuOpen = true })
                        AppMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                            if (state.own) {
                                AppMenuItem(stringResource(R.string.exercise_menu_edit), { menuOpen = false; onEdit(exercise.id) }, AppIcons.Ui.Edit)
                            }
                            AppMenuItem(stringResource(R.string.exercise_menu_add_to_template), { menuOpen = false; onAddToTemplate() }, AppIcons.Ui.Add)
                            AppMenuItem(stringResource(R.string.exercise_menu_hide), { menuOpen = false; onHide() }, AppIcons.Ui.Hide)
                            if (state.own) {
                                AppMenuItem(stringResource(R.string.exercise_menu_delete), { menuOpen = false; onDelete() }, AppIcons.Ui.Delete, danger = true)
                            }
                        }
                    }
                }
            }
            if (exercise == null) return@Column
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                verticalArrangement = Arrangement.spacedBy(d.stack),
            ) {
                exercise.thumbnailUrl?.takeIf { exercise.source == EntitySource.DEFAULT }?.let { url ->
                    item(key = "photo") { Photo(url, exercise.name) }
                }
                item(key = "title") { Title(exercise, state.own) }
                val last = state.history.last
                val best = state.history.best
                if (last != null && best != null) {
                    item(key = "numbers") { Numbers(exercise.trackingType, last, best, state.units) }
                }
                item(key = "rest") { RestRow(state.restSeconds, onRest) }
                item(key = "note") { NoteBlock(state.user.note, onNote) }
                if (state.history.entries.isNotEmpty()) {
                    item(key = "history") { SectionTitle(stringResource(R.string.exercise_history)) }
                    items(state.history.entries.take(HISTORY_ROWS), key = { it.sessionId }) { entry ->
                        HistoryRow(entry, exercise.trackingType, state.units)
                    }
                }
                if (exercise.instructions.isNotEmpty() || state.own) {
                    item(key = "howto") {
                        Section(stringResource(R.string.exercise_how_to)) {
                            if (exercise.instructions.isEmpty()) {
                                AppLinkButton("+ " + stringResource(R.string.exercise_add_steps), { onEdit(exercise.id) })
                            }
                            exercise.instructions.forEachIndexed { i, step ->
                                BodyLine(stringResource(R.string.exercise_step, i + 1, step))
                            }
                        }
                    }
                }
                if (exercise.tips.isNotEmpty()) {
                    item(key = "tips") {
                        Section(stringResource(R.string.exercise_tips)) {
                            exercise.tips.forEach { BodyLine(stringResource(R.string.exercise_tip, it)) }
                        }
                    }
                }
                if (!state.own) {
                    item(key = "muscles") { MusclesBlock(exercise) }
                }
            }
        }
    }
}

/** Bundled photo; nothing shows while it can't load (offline: name on top, like R13-7). */
@Composable
private fun Photo(url: String, name: String) {
    var failed by rememberSaveable(url) { mutableStateOf(false) }
    if (failed) return
    AsyncImage(
        model = url,
        contentDescription = stringResource(R.string.exercise_photo_cd, name),
        contentScale = ContentScale.Crop,
        onError = { failed = true },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PHOTO_RATIO)
            .clip(MaterialTheme.shapes.large),
    )
}

@Composable
private fun Title(exercise: Exercise, own: Boolean) {
    val d = GymBuddyTheme.dimens
    Column(Modifier.padding(top = d.hairline), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
        Text(exercise.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        val line = joinDot(exerciseSubtitle(exercise), if (own) stringResource(R.string.exercise_yours) else null)
        if (line.isNotEmpty()) {
            Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Last / Best in one card (R13-1). */
@Composable
private fun Numbers(type: ExerciseTrackingType, last: WorkoutSet, best: WorkoutSet, units: PreferredUnits) {
    val d = GymBuddyTheme.dimens
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = d.insetCard, vertical = d.stack)) {
            NumberCell(stringResource(R.string.exercise_last), setText(type, last, units), Modifier.weight(1f))
            NumberCell(stringResource(R.string.exercise_best), setText(type, best, units), Modifier.weight(1f))
        }
    }
}

@Composable
private fun NumberCell(label: String, value: String, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(GymBuddyTheme.dimens.hairline)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"))
    }
}

/** Quiet row (owner rule): timer, "Rest 2:00 between sets", chevron. */
@Composable
private fun RestRow(seconds: Int, onClick: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val cd = stringResource(R.string.exercise_rest_cd, clock(seconds))
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, color = cs.background, modifier = Modifier.fillMaxWidth().semantics { contentDescription = cd }) {
        Row(
            Modifier.heightIn(min = d.target).padding(horizontal = d.hairline),
            horizontalArrangement = Arrangement.spacedBy(d.stackTight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(AppIcons.Ui.Timer), null, Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
            Text(
                stringResource(R.string.exercise_rest_row, clock(seconds)),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(painterResource(AppIcons.Ui.ChevronRight), null, Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
        }
    }
}

/** Your note card with a pencil, or "+ Add note" (R13-1 / R13-7). */
@Composable
private fun NoteBlock(note: String?, onClick: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    if (note.isNullOrBlank()) {
        AppLinkButton("+ " + stringResource(R.string.exercise_add_note), onClick)
        return
    }
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = d.insetCard, vertical = d.stack), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                Text(stringResource(R.string.exercise_your_note), style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
                Text(note, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(painterResource(AppIcons.Ui.Edit), stringResource(R.string.exercise_edit_note), Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
        }
    }
}

/** Date left, best set right. Not tappable until session detail (7.9). */
@Composable
private fun HistoryRow(entry: ExerciseHistoryEntry, type: ExerciseTrackingType, units: PreferredUnits) {
    val d = GymBuddyTheme.dimens
    val zone = remember { ZoneId.systemDefault() }
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.heightIn(min = d.control).padding(horizontal = d.insetCard).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(shortDate(entry.startedAt, zone), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(setText(type, entry.best, units), style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = GymBuddyTheme.dimens.stack).semantics { heading() },
    )
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GymBuddyTheme.dimens.hairline)) {
        SectionTitle(title)
        Column(Modifier.padding(top = GymBuddyTheme.dimens.hairline), verticalArrangement = Arrangement.spacedBy(GymBuddyTheme.dimens.hairline)) {
            content()
        }
    }
}

@Composable
private fun BodyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Main / Also works / Equipment (R13-1). */
@Composable
private fun MusclesBlock(exercise: Exercise) {
    val main = exercise.primaryMuscles.map { stringResource(muscleLabel(it)) }
    val also = exercise.secondaryMuscles.map { stringResource(muscleLabel(it)) }
    val equipment = exercise.equipmentNeeded.map { stringResource(equipmentLabel(it)) }
    if (main.isEmpty() && also.isEmpty() && equipment.isEmpty()) return
    Section(stringResource(R.string.exercise_muscles_equipment)) {
        if (main.isNotEmpty()) BodyLine(stringResource(R.string.exercise_main, main.joinToString(", ")))
        if (also.isNotEmpty()) BodyLine(stringResource(R.string.exercise_also, also.joinToString(", ")))
        if (equipment.isNotEmpty()) BodyLine(stringResource(R.string.exercise_equipment, equipment.joinToString(", ")))
    }
}

/** A stored set as one line ("87.5 kg × 5"), same text as the workout. */
@Composable
internal fun setText(type: ExerciseTrackingType, set: WorkoutSet, units: PreferredUnits): String =
    setValueText(type, set.toUiSet(units), units)

/** Stored (metric) set → the workout's display-unit text set. */
internal fun WorkoutSet.toUiSet(units: PreferredUnits) = UiSet(
    id = id,
    setNumber = orderIndex + 1,
    reps = reps?.toString().orEmpty(),
    weight = weightKg?.let { UnitConverter.weightToDisplayUnit(it, units) }.orEmpty(),
    duration = durationSeconds?.toString().orEmpty(),
    distance = distanceMeters?.let(SetFields::format).orEmpty(),
)

package com.rukavina.gymbuddy.ui.template.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.AppMenu
import com.rukavina.gymbuddy.ui.design.AppMenuItem
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.template.WorkoutTemplateFormDialog
import com.rukavina.gymbuddy.ui.template.WorkoutTemplateViewModel
import com.rukavina.gymbuddy.ui.template.clock
import com.rukavina.gymbuddy.ui.template.exerciseCountText
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.template.relativeDay
import com.rukavina.gymbuddy.ui.template.targetText
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import androidx.compose.ui.res.pluralStringResource

/** Raw size (DESIGN.md §4): number column on Page A. */
private val IndexColumn = 28.dp

/**
 * Template page (Figma Page A 92:12491, Starter 93:13464, 200% 93:14244):
 * back, pencil, ⋯; title (wraps), summary line, numbered exercises with
 * a quiet rest on the right, lime Start pinned at the bottom (hidden
 * while a workout runs). Yours: ⋯ Duplicate, Delete (Undo). Starter:
 * ⋯ Make a copy, Hide this plan; the pencil makes a copy and edits it.
 * Editing still uses the old dialog until 05b.
 */
@Composable
fun TemplatePageScreen(
    openEditor: Boolean,
    workoutRunning: Boolean,
    onBack: () -> Unit,
    onStart: (WorkoutTemplate) -> Unit,
    onOpenCopy: (templateId: String, edit: Boolean) -> Unit,
    onDeleted: (templateId: String, title: String) -> Unit,
    onMessage: (String) -> Unit,
    viewModel: TemplatePageViewModel = hiltViewModel(),
    editViewModel: WorkoutTemplateViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editState by editViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by rememberSaveable { mutableStateOf(openEditor) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TemplatePageEvent.Open -> onOpenCopy(event.templateId, event.edit)
                is TemplatePageEvent.Deleted -> onDeleted(event.templateId, event.title)
                TemplatePageEvent.Hidden -> {
                    onMessage(context.getString(R.string.template_hidden))
                    onBack()
                }
                TemplatePageEvent.Failed -> onMessage(context.getString(R.string.template_action_failed))
            }
        }
    }
    // Gone (deleted or hidden elsewhere): nothing to show here.
    LaunchedEffect(state.loaded, state.line == null) {
        if (state.loaded && state.line == null) onBack()
    }

    val copyName: (String, Int) -> String = { title, n ->
        if (n == 1) context.getString(R.string.copy_name, title) else context.getString(R.string.copy_name_n, title, n)
    }
    TemplatePageContent(
        state = state,
        workoutRunning = workoutRunning,
        onBack = onBack,
        onStart = onStart,
        onEdit = { if (state.line?.isStarter == true) viewModel.copy(edit = true, name = copyName) else editing = true },
        onCopy = { viewModel.copy(edit = false, name = copyName) },
        onDelete = viewModel::delete,
        onHide = viewModel::hide,
    )

    val template = state.line?.template
    if (editing && template != null && state.line?.isStarter != true) {
        WorkoutTemplateFormDialog(
            template = template,
            availableExercises = editState.availableExercises,
            onDismiss = { editing = false },
            onSave = { draft ->
                editViewModel.updateTemplate(template, draft)
                editing = false
            },
        )
    }
}

@Composable
internal fun TemplatePageContent(
    state: TemplatePageUiState,
    workoutRunning: Boolean,
    onBack: () -> Unit,
    onStart: (WorkoutTemplate) -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onHide: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val line = state.line
    var menuOpen by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            BackTopBar(onBack = onBack) {
                if (line != null) {
                    AppIconButton(AppIcons.Ui.Edit, stringResource(R.string.cd_edit_template), onEdit, enabled = !state.busy)
                    Box {
                        AppIconButton(AppIcons.Ui.More, stringResource(R.string.cd_more), { menuOpen = true }, enabled = !state.busy)
                        AppMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                            if (line.isStarter) {
                                AppMenuItem(stringResource(R.string.menu_make_copy), { menuOpen = false; onCopy() }, AppIcons.Ui.Copy)
                                AppMenuItem(stringResource(R.string.menu_hide_plan), { menuOpen = false; onHide() }, AppIcons.Ui.Hide)
                            } else {
                                AppMenuItem(stringResource(R.string.menu_duplicate), { menuOpen = false; onCopy() }, AppIcons.Ui.Copy)
                                AppMenuItem(stringResource(R.string.menu_delete), { menuOpen = false; onDelete() }, AppIcons.Ui.Delete, danger = true)
                            }
                        }
                    }
                }
            }
            if (line == null) return@Column
            val exercises = line.template.templateExercises.sortedBy { it.orderIndex }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = d.insetScreen, end = d.insetScreen, bottom = d.group),
            ) {
                item(key = "head") {
                    Column(
                        Modifier.padding(start = d.hairline, bottom = d.group),
                        verticalArrangement = Arrangement.spacedBy(d.hairline),
                    ) {
                        Text(line.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                        Text(pageSubtitle(state), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                    }
                }
                itemsIndexed(exercises, key = { _, e -> e.id }) { index, exercise ->
                    if (index > 0) HorizontalDivider(thickness = d.strokeDivider, color = cs.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = d.stack),
                        horizontalArrangement = Arrangement.spacedBy(d.stackTight),
                    ) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(min = IndexColumn),
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                            Text(exercise.exerciseName, style = MaterialTheme.typography.titleSmall)
                            Text(targetText(exercise, state.units), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            exercise.notes?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                            }
                        }
                        exercise.restSeconds?.let { rest ->
                            Text(
                                stringResource(R.string.rest_value, clock(rest)),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    }
                }
            }
            if (!workoutRunning) {
                PrimaryButton(
                    text = stringResource(R.string.home_start_workout),
                    onClick = { onStart(line.template) },
                    enabled = exercises.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = d.insetScreen, vertical = d.stack),
                )
            }
        }
    }
}

/** Yours: "7 exercises · 23 sets · last done Tue". Starter: "Starter plan · 6 exercises". */
@Composable
private fun pageSubtitle(state: TemplatePageUiState): String {
    val line = state.line ?: return ""
    val exercises = exerciseCountText(line.exerciseCount)
    if (line.isStarter) return joinDot(stringResource(R.string.starter_plan), exercises)
    val sets = line.template.templateExercises.sumOf { it.plannedSets }
    return joinDot(
        exercises,
        pluralStringResource(R.plurals.set_count, sets, sets),
        line.lastDone?.let { stringResource(R.string.last_done, relativeDay(it, state.today, state.zone)) },
    )
}

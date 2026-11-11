package com.rukavina.gymbuddy.ui.exercises.create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.ui.design.AppTextField
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.InfoRow
import com.rukavina.gymbuddy.ui.design.ButtonSize
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.SegmentedButtons
import com.rukavina.gymbuddy.ui.exercise.difficultyLabel
import com.rukavina.gymbuddy.ui.exercise.kindLabel
import com.rukavina.gymbuddy.ui.exercises.page.ExerciseRestSheet
import com.rukavina.gymbuddy.ui.template.clock
import com.rukavina.gymbuddy.ui.template.editor.TemplateEditorViewModel
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Create A / Edit (Figma R13-4): × and lime Save, the four groups, More details folded. */
@Composable
fun ExerciseEditScreen(
    onClose: () -> Unit,
    onSaved: (exerciseId: String, isNew: Boolean) -> Unit,
    onMessage: (String) -> Unit,
    viewModel: ExerciseEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var restSheet by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExerciseEditEvent.Saved -> onSaved(event.exerciseId, event.isNew)
                ExerciseEditEvent.Close -> onClose()
                ExerciseEditEvent.Failed -> onMessage(context.getString(R.string.create_save_failed))
            }
        }
    }
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val form = state.form
    var details by rememberSaveable { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize().imePadding()) {
            BackTopBar(
                onBack = onClose,
                close = true,
                title = stringResource(if (state.isNew) R.string.create_title_new else R.string.create_title_edit),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.create_save),
                    onClick = viewModel::save,
                    size = ButtonSize.Small,
                    enabled = state.canSave,
                    loading = state.saving,
                    modifier = Modifier.padding(end = d.stackTight),
                )
            }
            if (!state.loaded) return@Column
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = d.insetScreen, end = d.insetScreen, bottom = d.section),
                verticalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                AppTextField(
                    value = form.name,
                    onValueChange = viewModel::setName,
                    label = stringResource(R.string.create_name),
                    isError = state.nameError,
                    helper = if (state.nameError) stringResource(R.string.create_name_error) else null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                FormLabel(stringResource(R.string.create_what_you_log))
                TrackingChips(form.tracking, viewModel::setTracking)
                EachSetLine(form.tracking, state.units)
                FormLabel(stringResource(R.string.create_main_muscle))
                MuscleChips(form.mainMuscle, viewModel::setMainMuscle)
                FormLabel(stringResource(R.string.create_equipment))
                EquipmentChips(form.equipment, viewModel::toggleEquipment)
                MoreDetailsHeader(details) { details = !details }
                AnimatedVisibility(details) {
                    Column(verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
                        FormLabel(stringResource(R.string.create_other_muscles))
                        MultiMuscleChips(form.otherMuscles, form.mainMuscle, viewModel::toggleOtherMuscle)
                        AppTextField(
                            value = form.steps,
                            onValueChange = viewModel::setSteps,
                            label = stringResource(R.string.create_steps),
                            helper = stringResource(R.string.create_steps_helper),
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.fillMaxWidth().padding(top = d.stack),
                        )
                        InfoRow(
                            label = stringResource(R.string.create_rest),
                            value = clock(form.restSeconds ?: TemplateEditorViewModel.DEFAULT_REST_SECONDS),
                            onClick = { restSheet = true },
                        )
                        FormLabel(stringResource(R.string.create_difficulty))
                        SegmentedButtons(
                            options = DifficultyLevel.entries.map { stringResource(difficultyLabel(it)) },
                            selectedIndex = form.difficulty.ordinal,
                            onSelect = { viewModel.setDifficulty(DifficultyLevel.entries[it]) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        FormLabel(stringResource(R.string.create_kind))
                        SegmentedButtons(
                            options = ExerciseType.entries.map { stringResource(kindLabel(it)) },
                            selectedIndex = form.kind.ordinal,
                            onSelect = { viewModel.setKind(ExerciseType.entries[it]) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
    if (restSheet) {
        ExerciseRestSheet(
            exerciseName = form.name.ifBlank { stringResource(R.string.create_title_new) },
            seconds = form.restSeconds,
            defaultSeconds = TemplateEditorViewModel.DEFAULT_REST_SECONDS,
            onPick = { restSheet = false; viewModel.setRest(it) },
            onDismiss = { restSheet = false },
        )
    }
}

/** "More details · Other muscles, how-to steps, rest" card with a chevron (R13-4). */
@Composable
private fun MoreDetailsHeader(open: Boolean, onClick: () -> Unit) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceContainer,
        modifier = Modifier.fillMaxWidth().padding(top = d.group),
    ) {
        Row(Modifier.padding(horizontal = d.insetCard, vertical = d.stack), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                Text(stringResource(R.string.create_more_details), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.create_more_details_body), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Icon(
                painterResource(if (open) AppIcons.Ui.ChevronUp else AppIcons.Ui.ChevronDown),
                null,
                Modifier.size(d.iconSmall),
                tint = cs.onSurfaceVariant,
            )
        }
    }
}

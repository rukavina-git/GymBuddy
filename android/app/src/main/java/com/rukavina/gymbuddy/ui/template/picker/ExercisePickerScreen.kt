package com.rukavina.gymbuddy.ui.template.picker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.ui.design.AppChip
import com.rukavina.gymbuddy.ui.design.AppSnackbar
import com.rukavina.gymbuddy.ui.exercises.CreateNamed
import com.rukavina.gymbuddy.ui.exercises.ListMuscleChips
import com.rukavina.gymbuddy.ui.exercises.create.QuickCreateScreen
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.rukavina.gymbuddy.ui.design.BackTopBar
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.SearchField
import com.rukavina.gymbuddy.ui.exercise.equipmentLabel
import com.rukavina.gymbuddy.ui.exercise.muscleLabel
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.theme.FullShape
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Raw size (DESIGN.md §4): the pick mark on Picker A. */
private val PickMark = 28.dp

/**
 * Exercise picker (Figma Picker A 93:13183, search 93:14148). × drops
 * the picks; lime "Add N" hands them back in pick order.
 */
@Composable
fun ExercisePickerScreen(
    onBack: () -> Unit,
    onAdd: (List<String>) -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val creating by viewModel.creating.collectAsStateWithLifecycle()
    var quickName by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.created.collect { ok ->
            if (ok) quickName = null else snackbar.showSnackbar(context.getString(R.string.exercises_create_failed))
        }
    }
    ExercisePickerContent(
        state = state,
        onBack = onBack,
        onQuery = viewModel::setQuery,
        onMuscle = viewModel::setMuscle,
        onToggle = viewModel::toggle,
        onAdd = { onAdd(viewModel.picked) },
        onCreateNamed = { quickName = it },
        snackbar = snackbar,
    )
    quickName?.let { name ->
        QuickCreateScreen(initialName = name, busy = creating, onCreate = viewModel::quickCreate, onDismiss = { quickName = null })
    }
}

@Composable
internal fun ExercisePickerContent(
    state: ExercisePickerUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onMuscle: (MuscleGroup?) -> Unit,
    onToggle: (String) -> Unit,
    onAdd: () -> Unit,
    onCreateNamed: (String) -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize().imePadding()) {
            BackTopBar(onBack = onBack, close = true, title = stringResource(R.string.picker_title))
            SearchField(
                value = state.query,
                onValueChange = onQuery,
                placeholder = stringResource(R.string.picker_search),
                modifier = Modifier.padding(horizontal = d.insetScreen),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = d.insetScreen, vertical = d.stackTight),
                horizontalArrangement = Arrangement.spacedBy(d.stackTight),
            ) {
                item { AppChip(stringResource(R.string.picker_all), state.muscle == null, { onMuscle(null) }) }
                items(ListMuscleChips) { m ->
                    AppChip(stringResource(muscleLabel(m)), state.muscle == m, { onMuscle(if (state.muscle == m) null else m) })
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = d.stackTight, vertical = d.hairline),
                verticalArrangement = Arrangement.spacedBy(d.hairline),
            ) {
                if (state.loaded && state.rows.isEmpty()) {
                    item(key = "none") {
                        if (state.query.isNotBlank()) {
                            CreateNamed(state.query.trim(), onCreateNamed)
                        } else {
                            Text(
                                stringResource(R.string.picker_none, state.query),
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.padding(d.insetScreen),
                            )
                        }
                    }
                }
                items(state.rows, key = { it.exercise.id }) { row -> PickerRowItem(row, onToggle) }
            }
            SnackbarHost(snackbar) { AppSnackbar(it) }
            PrimaryButton(
                text = if (state.pickCount > 0) stringResource(R.string.picker_add, state.pickCount)
                else stringResource(R.string.picker_add_none),
                onClick = onAdd,
                enabled = state.pickCount > 0,
                modifier = Modifier.fillMaxWidth().padding(horizontal = d.insetScreen, vertical = d.stack),
            )
        }
    }
}

@Composable
private fun PickerRowItem(row: PickerRow, onToggle: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val c = GymBuddyTheme.colors
    val d = GymBuddyTheme.dimens
    val picked = row.pickNumber != null
    val pickedText = row.pickNumber?.let { stringResource(R.string.cd_picked, it) } ?: stringResource(R.string.cd_not_picked)
    val e = row.exercise
    val subtitle = joinDot(
        e.primaryMuscles.firstOrNull()?.let { stringResource(muscleLabel(it)) },
        e.equipmentNeeded.firstOrNull()?.let { stringResource(equipmentLabel(it)) },
    )
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (picked) c.cobaltTint else cs.background,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = picked, role = Role.Checkbox, onValueChange = { onToggle(e.id) })
            .semantics { stateDescription = pickedText },
    ) {
        Row(
            Modifier.padding(horizontal = d.insetScreen, vertical = d.stack),
            horizontalArrangement = Arrangement.spacedBy(d.stack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                Text(e.name, style = MaterialTheme.typography.titleSmall)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            if (row.inTemplate) {
                Text(stringResource(R.string.picker_in_template), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            Surface(
                shape = FullShape,
                color = if (picked) c.cobalt else cs.background.copy(alpha = 0f),
                border = if (picked) null else BorderStroke(d.strokeOutline, cs.outline),
                modifier = Modifier.size(PickMark),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    row.pickNumber?.let {
                        Text("$it", style = MaterialTheme.typography.labelMedium, color = cs.onSurface)
                    }
                }
            }
        }
    }
}

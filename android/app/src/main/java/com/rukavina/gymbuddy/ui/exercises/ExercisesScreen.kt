package com.rukavina.gymbuddy.ui.exercises

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.ui.design.AppChip
import com.rukavina.gymbuddy.ui.design.AppIconButton
import com.rukavina.gymbuddy.ui.design.AppMenu
import com.rukavina.gymbuddy.ui.design.AppMenuItem
import com.rukavina.gymbuddy.ui.design.LargeTitleTopBar
import com.rukavina.gymbuddy.ui.design.PrimaryButton
import com.rukavina.gymbuddy.ui.design.SearchField
import com.rukavina.gymbuddy.ui.design.focusRing
import com.rukavina.gymbuddy.ui.design.rememberLargeTitleState
import com.rukavina.gymbuddy.ui.exercise.equipmentLabel
import com.rukavina.gymbuddy.ui.exercise.muscleLabel
import com.rukavina.gymbuddy.ui.exercises.create.ExerciseForm
import com.rukavina.gymbuddy.ui.exercises.create.QuickCreateSheet
import com.rukavina.gymbuddy.ui.template.joinDot
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme

/** Muscle chips on the list and the picker, in Figma order ("Other" has no chip). */
val ListMuscleChips = listOf(
    MuscleGroup.CHEST, MuscleGroup.BACK, MuscleGroup.LEGS, MuscleGroup.SHOULDERS, MuscleGroup.ARMS, MuscleGroup.CORE,
)

/**
 * Exercises tab root (Figma 85:10376): large title with + and ⋯, search,
 * muscle chips, one row per exercise. No match → "Create "<text>"" (R13-6).
 */
@Composable
fun ExercisesScreen(
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onHidden: () -> Unit,
    onMessage: (String) -> Unit,
    viewModel: ExercisesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val creating by viewModel.creating.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var quickName by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExercisesEvent.Created -> {
                    quickName = null
                    onMessage(context.getString(R.string.exercises_created, event.name))
                }
                ExercisesEvent.CreateFailed -> onMessage(context.getString(R.string.exercises_create_failed))
            }
        }
    }
    ExercisesContent(
        state = state,
        query = query,
        onQuery = viewModel::setQuery,
        onMuscle = viewModel::setMuscle,
        onOpen = onOpen,
        onNew = onNew,
        onHidden = onHidden,
        onCreateNamed = { quickName = it },
    )
    quickName?.let { name ->
        QuickCreateSheet(
            initialName = name,
            busy = creating,
            onCreate = viewModel::quickCreate,
            onDismiss = { quickName = null },
        )
    }
}

@Composable
internal fun ExercisesContent(
    state: ExercisesUiState,
    query: String,
    onQuery: (String) -> Unit,
    onMuscle: (MuscleGroup?) -> Unit,
    onOpen: (String) -> Unit,
    onNew: () -> Unit,
    onHidden: () -> Unit,
    onCreateNamed: (String) -> Unit,
) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    val titleState = rememberLargeTitleState()
    var menuOpen by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Column(Modifier.fillMaxSize()) {
            LargeTitleTopBar(title = stringResource(R.string.exercises_title), state = titleState) {
                AppIconButton(AppIcons.Ui.Add, stringResource(R.string.exercises_new), onNew)
                Box {
                    AppIconButton(AppIcons.Ui.More, stringResource(R.string.cd_more), { menuOpen = true })
                    AppMenu(expanded = menuOpen, onDismiss = { menuOpen = false }) {
                        AppMenuItem(stringResource(R.string.exercises_hidden), { menuOpen = false; onHidden() }, AppIcons.Ui.Hide)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(titleState.nestedScrollConnection),
                contentPadding = PaddingValues(bottom = d.section),
                verticalArrangement = Arrangement.spacedBy(d.hairline),
            ) {
                item(key = "search") {
                    SearchField(
                        value = query,
                        onValueChange = onQuery,
                        placeholder = stringResource(R.string.exercises_search),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = d.insetScreen),
                    )
                }
                item(key = "chips") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = d.insetScreen, vertical = d.stackTight),
                        horizontalArrangement = Arrangement.spacedBy(d.stackTight),
                    ) {
                        item { AppChip(stringResource(R.string.picker_all), state.muscle == null, { onMuscle(null) }) }
                        items(ListMuscleChips) { m ->
                            AppChip(stringResource(muscleLabel(m)), state.muscle == m, { onMuscle(if (state.muscle == m) null else m) })
                        }
                    }
                }
                if (state.loaded && state.rows.isEmpty() && query.isNotBlank()) {
                    item(key = "none") { CreateNamed(query.trim(), onCreateNamed) }
                }
                items(state.rows, key = { it.id }) { e -> ExerciseRow(e) { onOpen(e.id) } }
            }
        }
    }
}

/** R13-6: "No exercise called "x"", "Add it as your own.", lime Create "x". */
@Composable
internal fun CreateNamed(text: String, onCreate: (String) -> Unit, modifier: Modifier = Modifier) {
    val d = GymBuddyTheme.dimens
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().padding(horizontal = d.insetScreen, vertical = d.section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d.stackTight),
    ) {
        Text(stringResource(R.string.exercises_none, text), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(stringResource(R.string.exercises_none_body), style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        PrimaryButton(
            text = stringResource(R.string.exercises_create_named, text),
            onClick = { onCreate(text) },
            icon = AppIcons.Ui.Add,
            modifier = Modifier.padding(top = d.stackTight),
        )
    }
}

/** Row: name over "Muscle · Equipment", chevron. */
@Composable
private fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val d = GymBuddyTheme.dimens
    val shape = MaterialTheme.shapes.small
    val interaction = remember { MutableInteractionSource() }
    val subtitle = exerciseSubtitle(exercise)
    Surface(
        onClick = onClick,
        shape = shape,
        color = cs.background,
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().padding(horizontal = d.stackTight).focusRing(interaction, shape),
    ) {
        Row(
            Modifier.heightIn(min = d.row).padding(horizontal = d.stackTight + d.hairline, vertical = d.stackTight),
            horizontalArrangement = Arrangement.spacedBy(d.stack),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(d.hairline)) {
                Text(exercise.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
            Icon(painterResource(AppIcons.Ui.ChevronRight), null, Modifier.size(d.iconSmall), tint = cs.onSurfaceVariant)
        }
    }
}

/** "Chest · Barbell": first main muscle and first equipment. */
@Composable
fun exerciseSubtitle(exercise: Exercise): String = joinDot(
    exercise.primaryMuscles.firstOrNull()?.let { stringResource(muscleLabel(it)) },
    exercise.equipmentNeeded.firstOrNull()?.let { stringResource(equipmentLabel(it)) },
)

package com.rukavina.gymbuddy.ui.exercises.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rukavina.gymbuddy.R
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.ui.design.AppChip
import com.rukavina.gymbuddy.ui.exercise.equipmentLabel
import com.rukavina.gymbuddy.ui.exercise.muscleLabel
import com.rukavina.gymbuddy.ui.exercise.trackingLabel
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.workout.SetFields
import com.rukavina.gymbuddy.ui.workout.WorkoutSet
import com.rukavina.gymbuddy.ui.workout.active.setValueText
import com.rukavina.gymbuddy.utils.UnitConverter

/** Small muted label over a group of chips ("What you log", "Main muscle"). */
@Composable
internal fun FormLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = GymBuddyTheme.dimens.stack).semantics { heading() },
    )
}

/**
 * What you log: Weight + reps, Reps only, Time, then "More" opens the
 * other three. A picked type behind More shows it open.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrackingChips(selected: ExerciseTrackingType, onSelect: (ExerciseTrackingType) -> Unit) {
    val d = GymBuddyTheme.dimens
    var more by rememberSaveable { mutableStateOf(selected !in MainTracking) }
    val shown = if (more) ExerciseTrackingType.entries.toList() else MainTracking
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.stackTight), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
        shown.forEach { type ->
            AppChip(stringResource(trackingLabel(type)), selected == type, { onSelect(type) })
        }
        if (!more) AppChip(stringResource(R.string.create_more), false, { more = true }, dropdown = true)
    }
}

/** One main muscle (tap again to clear). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MuscleChips(selected: MuscleGroup?, onSelect: (MuscleGroup?) -> Unit) {
    val d = GymBuddyTheme.dimens
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.stackTight), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
        CreateMuscles.forEach { m ->
            AppChip(stringResource(muscleLabel(m)), selected == m, { onSelect(if (selected == m) null else m) })
        }
    }
}

/** Several muscles (Other muscles under More details). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MultiMuscleChips(selected: Set<MuscleGroup>, exclude: MuscleGroup?, onToggle: (MuscleGroup) -> Unit) {
    val d = GymBuddyTheme.dimens
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.stackTight), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
        CreateMuscles.filter { it != exclude }.forEach { m ->
            AppChip(stringResource(muscleLabel(m)), m in selected, { onToggle(m) })
        }
    }
}

/** Equipment, several allowed; "More" opens the full list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EquipmentChips(selected: Set<Equipment>, onToggle: (Equipment) -> Unit) {
    val d = GymBuddyTheme.dimens
    var more by rememberSaveable { mutableStateOf(selected.any { it !in MainEquipment }) }
    val shown = if (more) MainEquipment + Equipment.entries.filter { it !in MainEquipment } else MainEquipment
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(d.stackTight), verticalArrangement = Arrangement.spacedBy(d.stackTight)) {
        shown.forEach { e -> AppChip(stringResource(equipmentLabel(e)), e in selected, { onToggle(e) }) }
        if (!more) AppChip(stringResource(R.string.create_more), false, { more = true }, dropdown = true)
    }
}

/** "Each set: 40 kg × 12" under What you log (R13-4). */
@Composable
internal fun EachSetLine(type: ExerciseTrackingType, units: PreferredUnits) {
    val sample = WorkoutSet(
        id = "sample",
        setNumber = 1,
        weight = UnitConverter.weightToDisplayUnit(SAMPLE_KG, units),
        reps = "12",
        duration = "60",
        distance = SetFields.format(SAMPLE_METRES),
    )
    Text(
        stringResource(R.string.create_each_set, setValueText(type, sample, units)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val SAMPLE_KG = 40f
private const val SAMPLE_METRES = 400f

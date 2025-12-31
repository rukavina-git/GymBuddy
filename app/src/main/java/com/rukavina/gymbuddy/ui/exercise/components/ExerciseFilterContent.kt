package com.rukavina.gymbuddy.ui.exercise.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.MuscleGroup
import com.rukavina.gymbuddy.ui.components.FilterChipGroup

/**
 * Exercise-specific filter content using muscle groups and equipment.
 * Uses the generic FilterChipGroup component for each filter type.
 *
 * @param selectedMuscles Set of currently selected muscle groups
 * @param selectedEquipment Set of currently selected equipment
 * @param onMusclesChange Callback when muscle selection changes
 * @param onEquipmentChange Callback when equipment selection changes
 * @param modifier Modifier to be applied to the container
 */
@Composable
fun ExerciseFilterContent(
    selectedMuscles: Set<MuscleGroup>,
    selectedEquipment: Set<Equipment>,
    onMusclesChange: (Set<MuscleGroup>) -> Unit,
    onEquipmentChange: (Set<Equipment>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Muscle Groups section
    FilterChipGroup(
        items = MuscleGroup.entries,
        selectedItems = selectedMuscles,
        onSelectionChange = onMusclesChange,
        itemLabel = { it.name.replace("_", " ") },
        title = "Muscle Groups",
        modifier = modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Equipment section
    FilterChipGroup(
        items = Equipment.entries,
        selectedItems = selectedEquipment,
        onSelectionChange = onEquipmentChange,
        itemLabel = { equipment ->
            equipment.name.replace("_", " ").lowercase()
                .replaceFirstChar { it.uppercase() }
        },
        title = "Equipment",
        modifier = modifier.fillMaxWidth()
    )
}

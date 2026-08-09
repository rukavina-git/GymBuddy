package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.data.model.MuscleGroup
import javax.inject.Inject

/**
 * Use case for filtering exercises based on search query, muscle groups, and equipment.
 * Implements AND logic: exercises must match ALL criteria.
 */
class FilterExercisesUseCase @Inject constructor() {
    /**
     * Filter exercises based on multiple criteria.
     *
     * @param exercises List of exercises to filter
     * @param searchQuery Search query (matches name, description, or muscle names)
     * @param selectedMuscles Must have ALL selected muscle groups (AND logic)
     * @param selectedEquipment Must have ALL selected equipment (AND logic)
     * @return Filtered list of exercises matching all criteria
     */
    operator fun invoke(
        exercises: List<Exercise>,
        searchQuery: String,
        selectedMuscles: Set<MuscleGroup>,
        selectedEquipment: Set<Equipment>
    ): List<Exercise> {
        return exercises.filter { exercise ->
            // Search matches name, description, or muscles
            val matchesSearch = searchQuery.isBlank() ||
                exercise.name.contains(searchQuery, ignoreCase = true) ||
                exercise.description?.contains(searchQuery, ignoreCase = true) == true ||
                exercise.primaryMuscles.any { it.name.contains(searchQuery, ignoreCase = true) }

            // Must have ALL selected muscle groups (AND logic)
            val matchesMuscles = selectedMuscles.isEmpty() ||
                selectedMuscles.all { selectedMuscle ->
                    exercise.primaryMuscles.contains(selectedMuscle) ||
                    exercise.secondaryMuscles.contains(selectedMuscle)
                }

            // Must have ALL selected equipment (AND logic)
            val matchesEquipment = selectedEquipment.isEmpty() ||
                selectedEquipment.all { selectedEquip ->
                    exercise.equipmentNeeded.contains(selectedEquip)
                }

            matchesSearch && matchesMuscles && matchesEquipment
        }
    }
}

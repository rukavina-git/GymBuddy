package com.rukavina.gymbuddy.ui.exercise

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.rukavina.gymbuddy.data.model.DifficultyLevel
import com.rukavina.gymbuddy.data.model.Equipment
import com.rukavina.gymbuddy.data.model.Exercise
import com.rukavina.gymbuddy.data.model.ExerciseCategory
import com.rukavina.gymbuddy.data.model.ExerciseType
import com.rukavina.gymbuddy.data.model.MuscleGroup

/**
 * Form state holder for exercise creation/editing.
 * Separates business logic from UI layer.
 *
 * Responsibilities:
 * - Hold form field state
 * - Track touched state for validation
 * - Validate form data
 * - Transform form state to Exercise domain model
 *
 * @param initialExercise Existing exercise to edit, or null for new exercise
 */
@Stable
class ExerciseFormState(initialExercise: Exercise? = null) {
    // Form fields
    var name by mutableStateOf(initialExercise?.name ?: "")
    var description by mutableStateOf(initialExercise?.description ?: "")
    var difficulty by mutableStateOf(initialExercise?.difficulty ?: DifficultyLevel.BEGINNER)
    var category by mutableStateOf(initialExercise?.category ?: ExerciseCategory.STRENGTH)
    var exerciseType by mutableStateOf(initialExercise?.exerciseType ?: ExerciseType.COMPOUND)
    var primaryMuscles by mutableStateOf(initialExercise?.primaryMuscles ?: emptyList<MuscleGroup>())
    var secondaryMuscles by mutableStateOf(initialExercise?.secondaryMuscles ?: emptyList<MuscleGroup>())
    var equipmentNeeded by mutableStateOf(initialExercise?.equipmentNeeded ?: emptyList<Equipment>())
    val instructions: SnapshotStateList<String> = mutableStateListOf<String>().apply {
        addAll(initialExercise?.instructions ?: emptyList())
    }
    val tips: SnapshotStateList<String> = mutableStateListOf<String>().apply {
        addAll(initialExercise?.tips ?: emptyList())
    }
    var videoUrl by mutableStateOf(initialExercise?.videoUrl ?: "")
    var thumbnailUrl by mutableStateOf(initialExercise?.thumbnailUrl ?: "")

    // Focus tracking for validation (only show errors after field loses focus)
    private var nameHasBeenFocused by mutableStateOf(false)
    var nameTouched by mutableStateOf(false)
        private set
    var primaryMusclesTouched by mutableStateOf(false)

    /**
     * Called when name field focus changes.
     * Only marks as touched when field loses focus AFTER having been focused.
     */
    fun onNameFocusChanged(isFocused: Boolean) {
        if (isFocused) {
            nameHasBeenFocused = true
        } else if (nameHasBeenFocused) {
            nameTouched = true
        }
    }

    // Progressive disclosure state
    var detailsExpanded by mutableStateOf(false)
    var advancedExpanded by mutableStateOf(false)

    // Store initial exercise for preserving immutable fields
    private val initialExercise = initialExercise

    /**
     * Validates the form data.
     * @return true if form is valid and can be saved
     */
    fun isValid(): Boolean {
        return name.isNotBlank() && primaryMuscles.isNotEmpty()
    }

    /**
     * Checks if name field should show error.
     */
    fun shouldShowNameError(): Boolean {
        return nameTouched && name.isBlank()
    }

    /**
     * Checks if primary muscles should show error.
     */
    fun shouldShowPrimaryMusclesError(): Boolean {
        return primaryMusclesTouched && primaryMuscles.isEmpty()
    }

    /**
     * Transforms form state to Exercise domain model.
     * Includes business logic for data transformation and defaults.
     *
     * @return Exercise object ready to be saved
     */
    fun toExercise(): Exercise {
        return Exercise(
            id = initialExercise?.id ?: 0,
            name = name.trim(),
            primaryMuscles = primaryMuscles,
            secondaryMuscles = secondaryMuscles,
            description = description.trim().ifBlank { null },
            instructions = instructions.filter { it.isNotBlank() },
            tips = tips.filter { it.isNotBlank() },
            note = initialExercise?.note,
            difficulty = difficulty,
            equipmentNeeded = equipmentNeeded,
            category = category,
            exerciseType = exerciseType,
            videoUrl = videoUrl.trim().ifBlank { null },
            thumbnailUrl = thumbnailUrl.trim().ifBlank { null },
            isCustom = initialExercise?.isCustom ?: true,
            createdBy = initialExercise?.createdBy,
            isHidden = initialExercise?.isHidden ?: false
        )
    }

}

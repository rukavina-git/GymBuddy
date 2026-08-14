package com.rukavina.gymbuddy.ui.workout

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType

/**
 * User input for a single set, collected by WorkoutScreen's dialogs.
 *
 * existingId carries forward the id of a set already in the session being
 * edited; null means this is a new set. Presentation only relays ids it
 * already had - it never mints one. The ViewModel mints a fresh id with
 * IdGenerator for every null existingId when it constructs the real
 * WorkoutSet (see WorkoutSessionViewModel).
 */
data class WorkoutSetDraft(
    val existingId: String?,
    val weightKg: Float?,
    val reps: Int?,
    val durationSeconds: Int?,
    val distanceMeters: Float?,
    val orderIndex: Int
)

/**
 * User input for a single performed exercise. See WorkoutSetDraft for the
 * existingId convention. exerciseTrackingType is the real, resolved value
 * from the exercise library (picked via the exercise dropdown), not user
 * input - exerciseName is the resolved display name, used only so the
 * in-progress exercise list in WorkoutSessionFormDialog has something to
 * show before the session is saved and re-stamped.
 */
data class PerformedExerciseDraft(
    val existingId: String?,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseTrackingType: ExerciseTrackingType,
    val orderIndex: Int,
    val sets: List<WorkoutSetDraft>
)

/**
 * User input for a whole workout session edit, returned by
 * WorkoutSessionFormDialog. The ViewModel constructs the real
 * WorkoutSession/PerformedExercise/WorkoutSet entities from this - see
 * WorkoutSessionViewModel.createWorkoutSession/updateWorkoutSession.
 */
data class WorkoutSessionDraft(
    val startedAt: Long,
    val endedAt: Long?,
    val durationSeconds: Int,
    val title: String,
    val notes: String?,
    val templateId: String?,
    val templateTitle: String?,
    val performedExercises: List<PerformedExerciseDraft>
)

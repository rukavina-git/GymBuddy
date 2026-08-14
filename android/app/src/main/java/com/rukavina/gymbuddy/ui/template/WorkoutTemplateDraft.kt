package com.rukavina.gymbuddy.ui.template

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType

/**
 * User input for a single planned exercise, collected by
 * WorkoutTemplateFormDialog.
 *
 * localKey is a Compose-only list key (not a domain id) used purely to
 * keep list-item identity stable across reorder/delete while the template
 * is being edited; it is discarded once the draft is submitted.
 *
 * existingId carries forward the id of a TemplateExercise already in the
 * template being edited; null means this is a new exercise. Presentation
 * only relays ids it already had - it never mints one. The ViewModel
 * mints a fresh id with IdGenerator for every null existingId when it
 * constructs the real TemplateExercise (see WorkoutTemplateViewModel).
 */
data class TemplateExerciseDraft(
    val localKey: Int,
    val existingId: String?,
    val exerciseId: String,
    val exerciseName: String,
    val exerciseTrackingType: ExerciseTrackingType,
    val plannedSets: Int,
    val plannedReps: Int?,
    val orderIndex: Int,
    val restSeconds: Int?,
    val notes: String?
)

/**
 * User input for a whole template edit, returned by
 * WorkoutTemplateFormDialog. The ViewModel constructs the real
 * WorkoutTemplate/TemplateExercise entities from this - see
 * WorkoutTemplateViewModel.createTemplate/updateTemplate.
 */
data class WorkoutTemplateDraft(
    val title: String,
    val exercises: List<TemplateExerciseDraft>
)

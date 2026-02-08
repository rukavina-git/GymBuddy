package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for WorkoutTemplate table.
 * TemplateExercises are stored in a separate table with foreign key relationship.
 *
 * This entity represents only the template metadata (id and title).
 * The associated exercises are stored in TemplateExerciseEntity and
 * loaded using the WorkoutTemplateWithExercises relation class.
 */
@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    /**
     * Primary key - unique identifier for the template.
     */
    @PrimaryKey
    val id: String,

    /**
     * User-defined title for the template.
     * Examples: "Push Day", "Pull Day", "Leg Day"
     */
    val title: String,

    /**
     * Whether this is a default/bundled template.
     * Default templates cannot be edited or deleted, only hidden.
     */
    val isDefault: Boolean = false,

    /**
     * Whether this template is hidden by the user.
     * Hidden templates don't appear in the main list but can be restored.
     */
    val isHidden: Boolean = false
)

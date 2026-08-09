package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rukavina.gymbuddy.domain.model.EntitySource

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
     * Whether this is a default/bundled template. Default templates cannot
     * be edited or deleted, only hidden.
     */
    val source: EntitySource = EntitySource.CUSTOM,

    /**
     * User ID of the creator, for CUSTOM templates. Null for DEFAULT rows.
     */
    val ownerId: String? = null,

    /**
     * Id of the DEFAULT template this one was duplicated from. Not used yet.
     */
    val derivedFromId: String? = null,

    /**
     * Whether this template has been superseded and should no longer
     * appear in lists, search, or filters. Existing references to it
     * remain resolvable. See docs/adr/0002-deprecation-over-deletion.md.
     */
    val deprecated: Boolean = false
)

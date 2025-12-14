package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation class for querying WorkoutTemplate with its TemplateExercises.
 * Used for one-to-many relationship queries.
 *
 * This class is returned by DAO queries and provides a convenient way to
 * access both the template metadata and all its associated exercises in a single query.
 *
 * Example usage in DAO:
 * @Transaction
 * @Query("SELECT * FROM workout_templates WHERE id = :id")
 * suspend fun getTemplateById(id: String): WorkoutTemplateWithExercises?
 */
data class WorkoutTemplateWithExercises(
    /**
     * The template metadata (id, title).
     * Embedded means these fields are part of the parent SELECT query.
     */
    @Embedded
    val template: WorkoutTemplateEntity,

    /**
     * The list of exercises in this template.
     * Room automatically performs a join based on the parent-child relationship.
     */
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val templateExercises: List<TemplateExerciseEntity>
)

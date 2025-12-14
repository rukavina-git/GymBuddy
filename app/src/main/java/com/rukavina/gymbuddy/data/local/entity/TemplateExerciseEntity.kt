package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for TemplateExercise table.
 * Has foreign key relationship with WorkoutTemplate (cascade delete).
 * Has reference to Exercise (no cascade - exercises can exist independently).
 *
 * Indices are created on:
 * - templateId: For fast lookup of all exercises in a template
 * - exerciseId: For fast lookup of which templates use a specific exercise
 * - orderIndex: For efficient ordering of exercises within a template
 */
@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE // Delete template exercises when template is deleted
        )
    ],
    indices = [
        Index(value = ["templateId"]), // Index for faster queries by template
        Index(value = ["exerciseId"]), // Index for faster queries by exercise
        Index(value = ["orderIndex"]) // Index for ordering exercises
    ]
)
data class TemplateExerciseEntity(
    /**
     * Primary key - unique identifier for this template exercise.
     */
    @PrimaryKey
    val id: String,

    /**
     * Foreign key to WorkoutTemplateEntity.
     * References the template this exercise belongs to.
     */
    val templateId: String,

    /**
     * Reference to ExerciseEntity.
     * Points to the exercise definition (e.g., "Bench Press", "Squat").
     */
    val exerciseId: String,

    /**
     * Planned number of sets for this exercise.
     */
    val plannedSets: Int,

    /**
     * Planned number of repetitions per set.
     */
    val plannedReps: Int,

    /**
     * Order/position of this exercise in the template (0-indexed).
     * Used for maintaining consistent exercise sequence.
     */
    val orderIndex: Int,

    /**
     * Optional rest time in seconds between sets.
     * Null if no specific rest time is defined.
     */
    val restSeconds: Int?,

    /**
     * Optional notes/instructions for this exercise.
     * Can include form cues, tempo instructions, etc.
     */
    val notes: String?
)

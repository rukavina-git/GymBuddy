package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType

/**
 * Room entity for TemplateExercise table.
 * Has foreign key relationship with WorkoutTemplate (cascade delete).
 * Has reference to Exercise (no cascade - exercises can exist independently).
 *
 * exerciseId is a soft reference to the exercises table, not a foreign key:
 * it must tolerate the referenced exercise being re-seeded, deprecated, or
 * (for a custom exercise) deleted entirely. exerciseName is a snapshot of
 * Exercise.name written once at creation (or re-stamped on update) and
 * never rewritten by an unrelated library refresh or exercise rename.
 *
 * Indices are created on:
 * - templateId: For fast lookup of all exercises in a template
 * - exerciseId: For fast lookup of which templates use a specific exercise
 * - orderIndex: For efficient ordering of exercises within a template
 *
 * No updatedAt/deletedAt/revision/syncState columns - a child of the
 * WorkoutTemplate aggregate, governed by the parent's sync metadata.
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
    @PrimaryKey(autoGenerate = false)
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
     * Snapshot of Exercise.name. See class KDoc.
     */
    val exerciseName: String,

    /**
     * Snapshot of Exercise.trackingType. Same write-once rule as exerciseName.
     */
    val exerciseTrackingType: ExerciseTrackingType,

    /**
     * Planned number of sets for this exercise.
     */
    val plannedSets: Int,

    /**
     * Planned number of repetitions per set.
     * Null when the exercise's tracking type has no rep target.
     */
    val plannedReps: Int?,

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
     * Planned/target duration in seconds (e.g. a 60s plank). Mirrors
     * WorkoutSetEntity.durationSeconds. Null when not applicable.
     */
    val plannedDurationSeconds: Int? = null,

    /**
     * Planned/target distance in meters (e.g. a 2000m row). Mirrors
     * WorkoutSetEntity.distanceMeters. Null when not applicable.
     */
    val plannedDistanceMeters: Float? = null,

    /**
     * Planned/target weight in kilograms. Mirrors WorkoutSetEntity.weightKg.
     * Null when not applicable or not planned.
     */
    val plannedWeightKg: Float? = null,

    /**
     * Optional notes/instructions for this exercise.
     * Can include form cues, tempo instructions, etc.
     */
    val notes: String?
)

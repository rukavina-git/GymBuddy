package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.rukavina.gymbuddy.data.local.entity.BodyWeightEntryEntity
import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.UserExerciseStateEntity
import com.rukavina.gymbuddy.data.local.entity.UserTemplateStateEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSetEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity

/**
 * One-shot reads for the data export (ADR 0015). Live rows only, in a
 * stable order so the same data always gives the same file.
 */
@Dao
interface ExportDao {

    @Query("SELECT * FROM body_weight_entries WHERE deletedAt IS NULL ORDER BY measuredAt, id")
    suspend fun bodyWeightEntries(): List<BodyWeightEntryEntity>

    @Query("SELECT * FROM exercises WHERE source = 'CUSTOM' AND deletedAt IS NULL ORDER BY id")
    suspend fun customExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM workout_templates WHERE source = 'CUSTOM' AND deletedAt IS NULL ORDER BY id")
    suspend fun customTemplates(): List<WorkoutTemplateEntity>

    @Query(
        "SELECT te.* FROM template_exercises te JOIN workout_templates t ON t.id = te.templateId " +
            "WHERE t.source = 'CUSTOM' AND t.deletedAt IS NULL ORDER BY te.templateId, te.orderIndex, te.id"
    )
    suspend fun customTemplateExercises(): List<TemplateExerciseEntity>

    @Query("SELECT * FROM workout_sessions WHERE deletedAt IS NULL ORDER BY startedAt, id")
    suspend fun sessions(): List<WorkoutSessionEntity>

    @Query(
        "SELECT pe.* FROM performed_exercises pe JOIN workout_sessions s ON s.id = pe.workoutSessionId " +
            "WHERE s.deletedAt IS NULL ORDER BY pe.workoutSessionId, pe.orderIndex, pe.id"
    )
    suspend fun performedExercises(): List<PerformedExerciseEntity>

    @Query(
        "SELECT ws.* FROM workout_sets ws " +
            "JOIN performed_exercises pe ON pe.id = ws.performedExerciseId " +
            "JOIN workout_sessions s ON s.id = pe.workoutSessionId " +
            "WHERE s.deletedAt IS NULL ORDER BY ws.performedExerciseId, ws.orderIndex, ws.id"
    )
    suspend fun sets(): List<WorkoutSetEntity>

    @Query("SELECT * FROM user_exercise_state ORDER BY exerciseId")
    suspend fun exerciseStates(): List<UserExerciseStateEntity>

    @Query("SELECT * FROM user_template_state ORDER BY templateId")
    suspend fun templateStates(): List<UserTemplateStateEntity>
}

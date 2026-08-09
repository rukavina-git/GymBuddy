package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseWithSets
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WorkoutSession and PerformedExercise operations.
 * Handles queries for both tables due to their relationship.
 */
@Dao
interface WorkoutSessionDao {
    /**
     * Get all workout sessions.
     * Ordered by startedAt descending (most recent first).
     */
    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    fun getAllWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    /**
     * Get a single workout session by ID.
     */
    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getWorkoutSessionById(id: String): WorkoutSessionEntity?

    /**
     * Get workout sessions within a date range.
     * @param startDate Unix timestamp in milliseconds
     * @param endDate Unix timestamp in milliseconds
     */
    @Query("SELECT * FROM workout_sessions WHERE startedAt BETWEEN :startDate AND :endDate ORDER BY startedAt DESC")
    fun getWorkoutSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSessionEntity>>

    /**
     * Insert a new workout session.
     * Use with insertPerformedExercises for complete workout session creation.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(workoutSession: WorkoutSessionEntity)

    /**
     * Insert performed exercises for a workout session.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformedExercises(exercises: List<PerformedExerciseEntity>)

    /**
     * Insert a single performed exercise.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformedExercise(exercise: PerformedExerciseEntity)

    /**
     * Update a workout session.
     */
    @Update
    suspend fun updateWorkoutSession(workoutSession: WorkoutSessionEntity)

    /**
     * Update a performed exercise.
     */
    @Update
    suspend fun updatePerformedExercise(exercise: PerformedExerciseEntity)

    /**
     * Delete a workout session by ID.
     * Performed exercises will be cascade deleted due to foreign key.
     */
    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteWorkoutSession(id: String)

    /**
     * Delete performed exercises for a workout session.
     * Used when updating workout session to replace all performed exercises.
     */
    @Query("DELETE FROM performed_exercises WHERE workoutSessionId = :workoutSessionId")
    suspend fun deletePerformedExercisesByWorkoutSessionId(workoutSessionId: String)

    /**
     * Delete a specific performed exercise.
     */
    @Query("DELETE FROM performed_exercises WHERE id = :id")
    suspend fun deletePerformedExercise(id: String)

    /**
     * Delete all workout sessions.
     * Useful for testing or clearing data.
     */
    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllWorkoutSessions()

    /**
     * Transaction to insert workout session with performed exercises atomically.
     */
    @Transaction
    suspend fun insertWorkoutSessionWithExercises(
        workoutSession: WorkoutSessionEntity,
        performedExercises: List<PerformedExerciseEntity>
    ) {
        insertWorkoutSession(workoutSession)
        insertPerformedExercises(performedExercises)
    }

    /**
     * Transaction to update workout session with performed exercises atomically.
     * Replaces all performed exercises.
     */
    @Transaction
    suspend fun updateWorkoutSessionWithExercises(
        workoutSession: WorkoutSessionEntity,
        performedExercises: List<PerformedExerciseEntity>
    ) {
        updateWorkoutSession(workoutSession)
        deletePerformedExercisesByWorkoutSessionId(workoutSession.id)
        insertPerformedExercises(performedExercises)
    }

    /**
     * Get performed exercises for a workout session, ordered by orderIndex.
     * Not annotated with @Relation: Room's @Relation has no ORDER BY support,
     * so ordering is done here in SQL instead.
     */
    @Query("SELECT * FROM performed_exercises WHERE workoutSessionId = :workoutSessionId ORDER BY orderIndex ASC")
    suspend fun getPerformedExerciseEntitiesForSession(workoutSessionId: String): List<PerformedExerciseEntity>

    /**
     * Get the sets for a performed exercise, ordered by orderIndex.
     */
    @Query("SELECT * FROM workout_sets WHERE performedExerciseId = :performedExerciseId ORDER BY orderIndex ASC")
    suspend fun getWorkoutSetsForPerformedExercise(performedExerciseId: String): List<WorkoutSetEntity>

    /**
     * Get performed exercises with their sets for a workout session, both
     * ordered by orderIndex in SQL so ordering is correct regardless of caller.
     */
    @Transaction
    suspend fun getPerformedExercisesWithSets(workoutSessionId: String): List<PerformedExerciseWithSets> {
        return getPerformedExerciseEntitiesForSession(workoutSessionId).map { performedExercise ->
            PerformedExerciseWithSets(
                performedExercise = performedExercise,
                sets = getWorkoutSetsForPerformedExercise(performedExercise.id)
            )
        }
    }

    /**
     * Insert workout sets for a performed exercise.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSets(sets: List<WorkoutSetEntity>)

    /**
     * Delete workout sets for a performed exercise.
     */
    @Query("DELETE FROM workout_sets WHERE performedExerciseId = :performedExerciseId")
    suspend fun deleteWorkoutSetsByPerformedExerciseId(performedExerciseId: String)

    /**
     * Transaction to insert performed exercise with its sets atomically.
     */
    @Transaction
    suspend fun insertPerformedExerciseWithSets(
        exercise: PerformedExerciseEntity,
        sets: List<WorkoutSetEntity>
    ) {
        insertPerformedExercise(exercise)
        insertWorkoutSets(sets)
    }
}

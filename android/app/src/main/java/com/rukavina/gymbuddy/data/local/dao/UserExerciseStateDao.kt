package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rukavina.gymbuddy.data.local.entity.UserExerciseStateEntity

/**
 * DAO for per-user Exercise overlay state (user_exercise_state).
 * Upsert-only - see UserExerciseStateEntity for why rows are never
 * hard-deleted, even once cleared back to defaults.
 */
@Dao
interface UserExerciseStateDao {
    @Query("SELECT * FROM user_exercise_state WHERE exerciseId = :exerciseId")
    suspend fun getState(exerciseId: String): UserExerciseStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: UserExerciseStateEntity)

    /**
     * Set the hidden flag for an exercise, creating the sparse overlay row
     * if the user has never expressed an opinion about this exercise before.
     */
    @Transaction
    suspend fun setHidden(exerciseId: String, hidden: Boolean) {
        val current = getState(exerciseId) ?: UserExerciseStateEntity(exerciseId = exerciseId)
        upsert(current.copy(isHidden = hidden))
    }

    /**
     * Set the personal note for an exercise, creating the sparse overlay
     * row if none exists yet.
     */
    @Transaction
    suspend fun setNote(exerciseId: String, note: String?) {
        val current = getState(exerciseId) ?: UserExerciseStateEntity(exerciseId = exerciseId)
        upsert(current.copy(note = note))
    }

    /**
     * Unhide every exercise that currently has an overlay row marking it
     * hidden. Exercises with no overlay row are already not hidden.
     */
    @Query("UPDATE user_exercise_state SET isHidden = 0 WHERE isHidden = 1")
    suspend fun unhideAll()
}

package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.rukavina.gymbuddy.data.local.entity.ActiveWorkoutDraftEntity

@Dao
interface ActiveWorkoutDraftDao {
    @Query("SELECT * FROM active_workout_draft WHERE id = 0")
    suspend fun get(): ActiveWorkoutDraftEntity?

    @Upsert
    suspend fun upsert(draft: ActiveWorkoutDraftEntity)

    @Query("DELETE FROM active_workout_draft")
    suspend fun delete()
}

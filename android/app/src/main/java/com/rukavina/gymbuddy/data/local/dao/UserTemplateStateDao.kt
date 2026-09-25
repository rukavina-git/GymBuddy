package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rukavina.gymbuddy.data.local.entity.UserTemplateStateEntity
import com.rukavina.gymbuddy.domain.model.SyncState

/**
 * DAO for per-user WorkoutTemplate overlay state (user_template_state).
 * Upsert-only - see UserExerciseStateEntity for the full rationale, which
 * applies identically here.
 */
@Dao
interface UserTemplateStateDao {
    @Query("SELECT * FROM user_template_state WHERE templateId = :templateId")
    suspend fun getState(templateId: String): UserTemplateStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: UserTemplateStateEntity)

    /**
     * Set the hidden flag for a template, creating the sparse overlay row
     * if the user has never expressed an opinion about this template before.
     * updatedAt is supplied by the caller (from the injected Clock) since
     * a Room DAO can't take constructor-injected dependencies.
     */
    @Transaction
    suspend fun setHidden(templateId: String, hidden: Boolean, updatedAt: Long) {
        val current = getState(templateId) ?: UserTemplateStateEntity(templateId = templateId)
        upsert(current.copy(isHidden = hidden, updatedAt = updatedAt, syncState = SyncState.PENDING))
    }

    /**
     * Unhide every template that currently has an overlay row marking it
     * hidden. Templates with no overlay row are already not hidden.
     */
    @Query("SELECT templateId FROM user_template_state WHERE isHidden = 1")
    suspend fun getHiddenIds(): List<String>

    @Query("UPDATE user_template_state SET isHidden = 0, updatedAt = :updatedAt, syncState = 'PENDING' WHERE isHidden = 1")
    suspend fun unhideAll(updatedAt: Long)
}

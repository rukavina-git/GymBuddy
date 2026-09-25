package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.OutboxEntryEntity
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.entity.SyncLogEntity
import com.rukavina.gymbuddy.data.local.entity.SyncMetaEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.UserProfileEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity
import com.rukavina.gymbuddy.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

/**
 * Sync-engine persistence: the outbox, the discarded-change log, the
 * pull cursor, and the reads/writes the engine needs that ordinary
 * feature DAOs deliberately don't offer - above all reads that *include*
 * tombstoned rows, since a delete has to be pushed like any other change.
 */
@Dao
interface SyncDao {

    // ---- outbox ----

    @Query("SELECT * FROM sync_outbox WHERE entityType = :type AND entityId = :id")
    suspend fun getOutboxEntry(type: SyncEntityType, id: String): OutboxEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putOutboxEntry(entry: OutboxEntryEntity)

    /** Adds a pending change, or bumps the existing one's changeVersion. Call inside the entity write's transaction. */
    @Transaction
    suspend fun enqueue(type: SyncEntityType, id: String, operation: OutboxOperation, now: Long) {
        val existing = getOutboxEntry(type, id)
        putOutboxEntry(
            OutboxEntryEntity(
                entityType = type,
                entityId = id,
                operation = operation,
                changeVersion = (existing?.changeVersion ?: 0L) + 1,
                enqueuedAt = now
            )
        )
    }

    @Query("SELECT * FROM sync_outbox ORDER BY enqueuedAt ASC")
    suspend fun getOutbox(): List<OutboxEntryEntity>

    @Query("SELECT COUNT(*) FROM sync_outbox")
    suspend fun outboxCount(): Int

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun observeOutboxCount(): Flow<Int>

    @Query("DELETE FROM sync_outbox WHERE entityType = :type AND entityId = :id")
    suspend fun deleteOutboxEntry(type: SyncEntityType, id: String)

    /** Clears the entry only if no newer local write has bumped it since the push captured [changeVersion]. Returns rows deleted. */
    @Query("DELETE FROM sync_outbox WHERE entityType = :type AND entityId = :id AND changeVersion = :changeVersion")
    suspend fun deleteOutboxEntryIfUnchanged(type: SyncEntityType, id: String, changeVersion: Long): Int

    // ---- discarded-change log ----

    @Insert
    suspend fun insertLog(entry: SyncLogEntity)

    @Query("SELECT * FROM sync_log ORDER BY id ASC")
    suspend fun getLog(): List<SyncLogEntity>

    // ---- cursor ----

    @Query("SELECT value FROM sync_meta WHERE `key` = :key")
    suspend fun getMeta(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMeta(meta: SyncMetaEntity)

    // ---- reads including tombstones ----

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSessionIncludingDeleted(id: String): WorkoutSessionEntity?

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseIncludingDeleted(id: String): ExerciseEntity?

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateIncludingDeleted(id: String): WorkoutTemplateEntity?

    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY orderIndex ASC")
    suspend fun getTemplateExercises(templateId: String): List<TemplateExerciseEntity>

    @Query("SELECT * FROM user_profile WHERE uid = :uid")
    suspend fun getProfileIncludingDeleted(uid: String): UserProfileEntity?

    // ---- server-response metadata ----

    @Query("UPDATE workout_sessions SET revision = :revision, updatedAt = :updatedAt, syncState = :syncState WHERE id = :id")
    suspend fun setSessionSyncMeta(id: String, revision: Int, updatedAt: Long, syncState: SyncState)

    @Query("UPDATE exercises SET revision = :revision, updatedAt = :updatedAt, syncState = :syncState WHERE id = :id")
    suspend fun setExerciseSyncMeta(id: String, revision: Int, updatedAt: Long, syncState: SyncState)

    @Query("UPDATE workout_templates SET revision = :revision, updatedAt = :updatedAt, syncState = :syncState WHERE id = :id")
    suspend fun setTemplateSyncMeta(id: String, revision: Int, updatedAt: Long, syncState: SyncState)

    @Query("UPDATE user_exercise_state SET revision = :revision, updatedAt = :updatedAt, syncState = :syncState WHERE exerciseId = :id")
    suspend fun setExerciseStateSyncMeta(id: String, revision: Int, updatedAt: Long, syncState: SyncState)

    @Query("UPDATE user_template_state SET revision = :revision, updatedAt = :updatedAt, syncState = :syncState WHERE templateId = :id")
    suspend fun setTemplateStateSyncMeta(id: String, revision: Int, updatedAt: Long, syncState: SyncState)

    @Query("UPDATE user_profile SET revision = :revision, updatedAt = :updatedAt, syncState = :syncState WHERE uid = :id")
    suspend fun setProfileSyncMeta(id: String, revision: Int, updatedAt: Long, syncState: SyncState)

    @Query("UPDATE workout_sessions SET syncState = :syncState WHERE id = :id")
    suspend fun setSessionSyncState(id: String, syncState: SyncState)

    @Query("UPDATE exercises SET syncState = :syncState WHERE id = :id")
    suspend fun setExerciseSyncState(id: String, syncState: SyncState)

    @Query("UPDATE workout_templates SET syncState = :syncState WHERE id = :id")
    suspend fun setTemplateSyncState(id: String, syncState: SyncState)

    @Query("UPDATE user_exercise_state SET syncState = :syncState WHERE exerciseId = :id")
    suspend fun setExerciseStateSyncState(id: String, syncState: SyncState)

    @Query("UPDATE user_template_state SET syncState = :syncState WHERE templateId = :id")
    suspend fun setTemplateStateSyncState(id: String, syncState: SyncState)

    @Query("UPDATE user_profile SET syncState = :syncState WHERE uid = :id")
    suspend fun setProfileSyncState(id: String, syncState: SyncState)

    // ---- aggregate replacement (pull) ----

    @Query("DELETE FROM performed_exercises WHERE workoutSessionId = :sessionId")
    suspend fun deletePerformedExercisesForSession(sessionId: String)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteTemplateExercisesForTemplate(templateId: String)

    // ---- reference data ----

    @Query("DELETE FROM workout_templates WHERE source = 'DEFAULT'")
    suspend fun deleteDefaultTemplates()

    // ---- wipe (logout, cursor expiry) ----

    @Query("DELETE FROM workout_sessions")
    suspend fun wipeSessions()

    @Query("DELETE FROM exercises WHERE source = 'CUSTOM'")
    suspend fun wipeCustomExercises()

    @Query("DELETE FROM workout_templates WHERE source = 'CUSTOM'")
    suspend fun wipeCustomTemplates()

    @Query("DELETE FROM user_exercise_state")
    suspend fun wipeExerciseStates()

    @Query("DELETE FROM user_template_state")
    suspend fun wipeTemplateStates()

    @Query("DELETE FROM user_profile")
    suspend fun wipeProfiles()

    @Query("DELETE FROM sync_outbox")
    suspend fun wipeOutbox()

    @Query("DELETE FROM sync_log")
    suspend fun wipeLog()

    @Query("DELETE FROM sync_meta")
    suspend fun wipeMeta()

    /**
     * Removes everything that belongs to the signed-in user, leaving the
     * DEFAULT reference library (and its version rows) in place. Session
     * and template children go via their ON DELETE CASCADE foreign keys.
     */
    @Transaction
    suspend fun wipeLocalUserData() {
        wipeSessions()
        wipeCustomExercises()
        wipeCustomTemplates()
        wipeExerciseStates()
        wipeTemplateStates()
        wipeProfiles()
        wipeOutbox()
        wipeLog()
        wipeMeta()
    }
}

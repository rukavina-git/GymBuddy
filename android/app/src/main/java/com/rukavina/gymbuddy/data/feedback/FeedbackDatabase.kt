package com.rukavina.gymbuddy.data.feedback

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

/** File name of [FeedbackDatabase]. */
const val FEEDBACK_DATABASE_NAME = "feedback_db"

/**
 * A feedback message not yet accepted by the server (Plan 10 step 06b).
 * [uid] is who was signed in when Send was pressed; null for a guest.
 */
@Entity(tableName = "pending_feedback")
data class PendingFeedbackEntity(
    @PrimaryKey val id: String,
    val message: String,
    val uid: String?,
    val appVersion: String?,
    val osVersion: String?,
    val deviceModel: String?,
    val createdAt: Long,
)

@Dao
interface PendingFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: PendingFeedbackEntity)

    @Query("SELECT * FROM pending_feedback ORDER BY createdAt")
    suspend fun all(): List<PendingFeedbackEntity>

    @Query("SELECT COUNT(*) FROM pending_feedback WHERE id = :id")
    suspend fun exists(id: String): Int

    @Query("DELETE FROM pending_feedback WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_feedback WHERE uid = :uid")
    suspend fun deleteForUid(uid: String)
}

/**
 * Unsent feedback, in its own file. The app database is one file per
 * account (ADR 0014) and only the current one is open, so a message
 * queued there could be stranded by a sign-in or sign-out before it
 * went out. This file never switches.
 *
 * Schema change: bump [version], export the schema, add a Migration
 * (same rule as AppDatabase).
 */
@Database(entities = [PendingFeedbackEntity::class], version = 1, exportSchema = true)
abstract class FeedbackDatabase : RoomDatabase() {
    abstract fun pendingFeedbackDao(): PendingFeedbackDao
}

package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rukavina.gymbuddy.data.local.entity.BodyWeightEntryEntity
import com.rukavina.gymbuddy.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyWeightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BodyWeightEntryEntity)

    @Query("SELECT * FROM body_weight_entries WHERE deletedAt IS NULL ORDER BY measuredAt DESC, id DESC")
    fun observeLive(): Flow<List<BodyWeightEntryEntity>>

    /** The newest live measurement: the one that sets the profile's weight. */
    @Query("SELECT * FROM body_weight_entries WHERE deletedAt IS NULL ORDER BY measuredAt DESC, id DESC LIMIT 1")
    suspend fun getNewest(): BodyWeightEntryEntity?

    @Query("SELECT * FROM body_weight_entries WHERE id = :id")
    suspend fun getIncludingDeleted(id: String): BodyWeightEntryEntity?

    @Query("SELECT revision FROM body_weight_entries WHERE id = :id")
    suspend fun getRevision(id: String): Int?

    @Query("UPDATE body_weight_entries SET deletedAt = :deletedAt, updatedAt = :updatedAt, syncState = 'PENDING' WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: Long, updatedAt: Long)

    @Query("UPDATE body_weight_entries SET revision = :revision, updatedAt = :updatedAt, syncState = :state WHERE id = :id")
    suspend fun setSyncMeta(id: String, revision: Int, updatedAt: Long, state: SyncState)

    @Query("UPDATE body_weight_entries SET syncState = :state WHERE id = :id")
    suspend fun setSyncState(id: String, state: SyncState)
}

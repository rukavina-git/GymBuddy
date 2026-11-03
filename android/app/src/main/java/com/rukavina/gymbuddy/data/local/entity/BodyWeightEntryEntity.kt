package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rukavina.gymbuddy.domain.model.SyncState

/** Room row for BodyWeightEntry (api/openapi.yaml), with the usual sync metadata. */
@Entity(tableName = "body_weight_entries")
data class BodyWeightEntryEntity(
    @PrimaryKey
    val id: String,
    val measuredAt: Long,
    val weightKg: Float,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val revision: Int = 0,
    val syncState: SyncState = SyncState.PENDING
)

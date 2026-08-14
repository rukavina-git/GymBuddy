package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rukavina.gymbuddy.domain.model.*

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val profileImageUrl: String? = null,
    val birthDate: Long? = null,
    val weight: Float? = null,
    val height: Float? = null,
    val gender: Gender? = null,
    val fitnessGoal: FitnessGoal? = null,
    val activityLevel: ActivityLevel? = null,
    val targetWeight: Float? = null,
    val joinedDate: Long,
    val bio: String? = null,
    val updatedAt: Long = 0L,
    val deletedAt: Long? = null,
    val revision: Int = 0,
    val syncState: SyncState = SyncState.PENDING
)
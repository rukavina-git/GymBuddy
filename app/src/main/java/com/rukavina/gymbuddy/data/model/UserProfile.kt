package com.rukavina.gymbuddy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val uid: String, // FirebaseAuth UID
    val name: String,
    val email: String,
    val profileImageUrl: String? = null,
    val age: Int? = null,
    val weight: Float? = null,
    val height: Float? = null,
    val gender: Gender? = null,
    val fitnessGoal: FitnessGoal? = null,
    val activityLevel: ActivityLevel? = null,
    val targetWeight: Float? = null,
    val preferredUnits: PreferredUnits = PreferredUnits.METRIC,
    val joinedDate: Long,
    val bio: String? = null
)
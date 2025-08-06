package com.rukavina.gymbuddy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val uid: String, // FirebaseAuth UID
    val firstName: String,
    val lastName: String,
    val email: String,
    val age: Int?,
    val weight: Float?,
    val height: Float?,
    val gender: String?,
    val goal: String
)
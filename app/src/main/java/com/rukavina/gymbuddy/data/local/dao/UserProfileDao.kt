package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rukavina.gymbuddy.data.model.UserProfile

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("SELECT * FROM user_profile WHERE uid = :uid LIMIT 1")
    suspend fun getUserProfile(uid: String): UserProfile?

    @Update
    suspend fun updateUserProfile(profile: UserProfile)

    @Delete
    suspend fun deleteUserProfile(profile: UserProfile)
}
package com.rukavina.gymbuddy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.rukavina.gymbuddy.data.local.entity.UserProfileEntity

@Dao
interface UserProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE uid = :uid AND deletedAt IS NULL LIMIT 1")
    suspend fun getUserProfile(uid: String): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE uid = :uid AND deletedAt IS NULL LIMIT 1")
    fun observeUserProfile(uid: String): Flow<UserProfileEntity?>

    /** Stored revision, tombstoned row included. See WorkoutSessionDao.getRevision. */
    @Query("SELECT revision FROM user_profile WHERE uid = :uid")
    suspend fun getRevision(uid: String): Int?

}
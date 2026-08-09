package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.UserProfileDao
import com.rukavina.gymbuddy.data.model.UserProfile
import jakarta.inject.Inject

class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao
) {

    suspend fun saveProfile(profile: UserProfile) {
        dao.insertUserProfile(profile)
    }

    suspend fun getProfile(uid: String): UserProfile? {
        return dao.getUserProfile(uid)
    }

}
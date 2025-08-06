package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.UserProfileDao
import com.rukavina.gymbuddy.data.model.UserProfile

class UserProfileRepository(private val dao: UserProfileDao) {

    suspend fun saveProfile(profile: UserProfile) {
        dao.insertUserProfile(profile)
    }

    suspend fun getProfile(uid: String): UserProfile? {
        return dao.getUserProfile(uid)
    }

    suspend fun updateProfile(profile: UserProfile) {
        dao.updateUserProfile(profile)
    }

    suspend fun deleteProfile(profile: UserProfile) {
        dao.deleteUserProfile(profile)
    }
}
package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.UserProfileDao
import com.rukavina.gymbuddy.data.local.mapper.UserProfileMapper
import com.rukavina.gymbuddy.domain.model.UserProfile
import javax.inject.Inject

class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao
) {

    suspend fun saveProfile(profile: UserProfile) {
        dao.insertUserProfile(UserProfileMapper.toEntity(profile))
    }

    suspend fun getProfile(uid: String): UserProfile? =
        dao.getUserProfile(uid)?.let { UserProfileMapper.toDomain(it) }

}
package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.UserProfileDao
import com.rukavina.gymbuddy.data.local.mapper.UserProfileMapper
import com.rukavina.gymbuddy.domain.model.UserProfile
import java.time.Clock
import javax.inject.Inject

class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao,
    private val clock: Clock
) {

    suspend fun saveProfile(profile: UserProfile) {
        val entity = UserProfileMapper.toEntity(profile).copy(updatedAt = clock.millis())
        dao.insertUserProfile(entity)
    }

    suspend fun getProfile(uid: String): UserProfile? =
        dao.getUserProfile(uid)?.let { UserProfileMapper.toDomain(it) }

}
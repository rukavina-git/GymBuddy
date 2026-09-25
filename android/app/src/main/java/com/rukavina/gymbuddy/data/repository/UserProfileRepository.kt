package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.UserProfileDao
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.mapper.UserProfileMapper
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.domain.model.UserProfile
import java.time.Clock
import javax.inject.Inject

/**
 * Profile writes go through [OutboxRecorder] and keep the stored
 * revision - see WorkoutSessionRepositoryImpl.
 */
class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao,
    private val outbox: OutboxRecorder,
    private val clock: Clock
) {

    suspend fun saveProfile(profile: UserProfile) {
        outbox.record(SyncEntityType.USER_PROFILE, profile.uid) {
            val revision = dao.getRevision(profile.uid) ?: 0
            val entity = UserProfileMapper.toEntity(profile)
                .copy(updatedAt = clock.millis(), revision = revision, syncState = SyncState.PENDING)
            dao.insertUserProfile(entity)
        }
    }

    suspend fun getProfile(uid: String): UserProfile? =
        dao.getUserProfile(uid)?.let { UserProfileMapper.toDomain(it) }

}
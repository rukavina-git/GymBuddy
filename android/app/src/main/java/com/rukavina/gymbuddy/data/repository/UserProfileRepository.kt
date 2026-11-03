package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
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
    private val holder: DatabaseHolder,
    private val outbox: OutboxRecorder,
    private val clock: Clock
) {

    suspend fun saveProfile(profile: UserProfile) {
        outbox.record(SyncEntityType.USER_PROFILE, profile.uid) { db ->
            val dao = db.userProfileDao()
            val revision = dao.getRevision(profile.uid) ?: 0
            val entity = UserProfileMapper.toEntity(profile)
                .copy(updatedAt = clock.millis(), revision = revision, syncState = SyncState.PENDING)
            dao.insertUserProfile(entity)
        }
    }

    /** The profile key of whoever owns the open data: the account's uid, or [GUEST_UID]. */
    val ownerUid: String get() = holder.owner ?: GUEST_UID

    /** No account owns the open data (ADR 0014). */
    val isGuest: Boolean get() = holder.owner == null

    suspend fun getProfile(uid: String): UserProfile? =
        holder.db.userProfileDao().getUserProfile(uid)?.let { UserProfileMapper.toDomain(it) }

    companion object {
        /**
         * The guest's profile key (ADR 0014). Local only: a guest never
         * pushes, and a merge re-keys the row to the account's uid.
         */
        const val GUEST_UID = "guest"
    }
}

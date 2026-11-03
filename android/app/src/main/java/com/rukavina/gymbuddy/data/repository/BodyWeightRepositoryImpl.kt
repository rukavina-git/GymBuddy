package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.BodyWeightEntryEntity
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.domain.model.BodyWeightEntry
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.domain.repository.BodyWeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock

/**
 * Body weight history (Plan 6 step 04). Writes go through
 * [OutboxRecorder] and keep the stored revision, like every synced row.
 *
 * Decision 1A: the newest entry is also the profile's weight. Saving the
 * newest entry updates the profile row (if there is one) in the same
 * transaction and queues it too. Deleting or back-dating never touches
 * the profile: the profile weight stays what it was.
 */
class BodyWeightRepositoryImpl(
    private val holder: DatabaseHolder,
    private val outbox: OutboxRecorder,
    private val clock: Clock
) : BodyWeightRepository {

    override fun observeEntries(): Flow<List<BodyWeightEntry>> =
        holder.observe { it.bodyWeightDao().observeLive() }.map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(entry: BodyWeightEntry) {
        require(entry.weightKg > 0f && entry.weightKg <= MAX_WEIGHT_KG) { "weightKg out of range" }
        val profileUid = holder.owner ?: UserProfileRepository.GUEST_UID
        outbox.record(SyncEntityType.BODY_WEIGHT_ENTRY, entry.id) { db ->
            val dao = db.bodyWeightDao()
            val now = clock.millis()
            dao.upsert(
                BodyWeightEntryEntity(
                    id = entry.id,
                    measuredAt = entry.measuredAt,
                    weightKg = entry.weightKg,
                    updatedAt = now,
                    revision = dao.getRevision(entry.id) ?: 0,
                    syncState = SyncState.PENDING
                )
            )
            if (dao.getNewest()?.id == entry.id) {
                val profiles = db.userProfileDao()
                val profile = profiles.getUserProfile(profileUid)
                if (profile != null && profile.weight != entry.weightKg) {
                    profiles.insertUserProfile(profile.copy(weight = entry.weightKg, updatedAt = now, syncState = SyncState.PENDING))
                    db.syncDao().enqueue(SyncEntityType.USER_PROFILE, profileUid, OutboxOperation.UPSERT, now)
                }
            }
        }
    }

    override suspend fun delete(id: String) {
        val now = clock.millis()
        outbox.record(SyncEntityType.BODY_WEIGHT_ENTRY, id, OutboxOperation.DELETE) { db ->
            db.bodyWeightDao().markDeleted(id, deletedAt = now, updatedAt = now)
        }
    }

    private fun BodyWeightEntryEntity.toDomain() = BodyWeightEntry(id = id, measuredAt = measuredAt, weightKg = weightKg)

    private companion object {
        const val MAX_WEIGHT_KG = 700f
    }
}

package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.UserProfileSyncDto
import com.rukavina.gymbuddy.domain.EntityType
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.persistence.UserProfileRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * Push handling for UserProfile. There is no ownership check (the row
 * is always keyed by the caller's own uid, never anything from the
 * payload - see auth/CurrentUser.kt) and, in practice, no insert path
 * either: FirebaseAuthenticationFilter's just-in-time provisioning
 * guarantees a user_profile row exists at revision 0 before any
 * authenticated request reaches here, so the very first push always
 * lands in RevisionChecker.Update (client revision 0 == stored revision
 * 0), not Insert. The Insert branch is kept for defensiveness only.
 */
@Service
class ProfileSyncService(
    private val repository: UserProfileRepository,
    private val revisionChecker: RevisionChecker,
    private val changeLogWriter: ChangeLogWriter,
    private val advisoryLock: AdvisoryLock,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(ProfileSyncService::class.java)

    fun apply(uid: String, dto: UserProfileSyncDto, traceId: String): MutationResultDto {
        validate(dto)?.let { reason ->
            SyncAudit.log(log, traceId, uid, EntityType.USER_PROFILE, uid, SyncStatus.INVALID, null, null, reason)
            return MutationResultDto(EntityType.USER_PROFILE, uid, SyncStatus.INVALID, reason = reason)
        }

        return try {
            val (result, revisionBefore) = transactionTemplate.execute {
                advisoryLock.acquireForUser(uid)
                val stored = repository.findByOwner(uid)
                val storedContent = stored?.let {
                    UserProfileSyncDto(
                        name = it.name, email = it.email, profileImageUrl = it.profileImageUrl,
                        birthDate = it.birthDate, weight = it.weight, height = it.height, gender = it.gender,
                        fitnessGoal = it.fitnessGoal, activityLevel = it.activityLevel, targetWeight = it.targetWeight,
                        joinedDate = it.joinedDate, bio = it.bio, revision = 0,
                    )
                }

                val now = System.currentTimeMillis()
                val decision = revisionChecker.decide(
                    storedRevision = stored?.revision,
                    clientRevision = dto.revision,
                    storedContent = storedContent,
                    incomingContent = dto.copy(revision = 0),
                )

                val outcome = when (decision) {
                    is RevisionDecision.Insert -> {
                        repository.insertIfAbsent(uid, dto.name, dto.email, dto.profileImageUrl)
                        repository.updateForSync(uid, dto, newRevision = 1, now = now)
                        changeLogWriter.record(uid, EntityType.USER_PROFILE, uid)
                        MutationResultDto(EntityType.USER_PROFILE, uid, SyncStatus.APPLIED, updatedAt = now, revision = 1)
                    }

                    is RevisionDecision.Update -> {
                        repository.updateForSync(uid, dto, decision.resultingRevision, now)
                        changeLogWriter.record(uid, EntityType.USER_PROFILE, uid)
                        MutationResultDto(EntityType.USER_PROFILE, uid, SyncStatus.APPLIED, updatedAt = now, revision = decision.resultingRevision)
                    }

                    is RevisionDecision.IdempotentReplay ->
                        MutationResultDto(EntityType.USER_PROFILE, uid, SyncStatus.APPLIED, updatedAt = stored!!.updatedAt, revision = decision.storedRevision)

                    is RevisionDecision.Conflict ->
                        MutationResultDto(
                            EntityType.USER_PROFILE, uid, SyncStatus.CONFLICT,
                            reason = "Server has revision ${decision.storedRevision}; client sent ${dto.revision}.",
                        )
                }
                outcome to stored?.revision
            }!!

            SyncAudit.log(log, traceId, uid, EntityType.USER_PROFILE, uid, result.status, revisionBefore, result.revision, result.reason)
            result
        } catch (e: Exception) {
            log.error("sync push [traceId={}] uid={} entityType=USER_PROFILE entityId={} unexpected failure", traceId, uid, uid, e)
            MutationResultDto(EntityType.USER_PROFILE, uid, SyncStatus.ERROR, reason = "Unexpected server error while processing this entity. Retry later.")
        }
    }

    private fun validate(dto: UserProfileSyncDto): String? {
        if (dto.name.isBlank()) return "name is required"
        if (dto.email.isBlank()) return "email is required"
        if ((dto.weight ?: 0f) < 0f) return "weight cannot be negative"
        if ((dto.height ?: 0f) < 0f) return "height cannot be negative"
        if ((dto.targetWeight ?: 0f) < 0f) return "targetWeight cannot be negative"
        return null
    }
}

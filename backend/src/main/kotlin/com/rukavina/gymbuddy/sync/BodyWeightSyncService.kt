package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.BodyWeightEntryDto
import com.rukavina.gymbuddy.api.dto.BodyWeightEntrySyncDto
import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.forComparison
import com.rukavina.gymbuddy.domain.EntityType
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.persistence.BodyWeightEntryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * Push and pull for BodyWeightEntry (Plan 6 step 04). Same rules as
 * ExerciseSyncService: per-user advisory lock, server-wins revisions,
 * FORBIDDEN for another user's id, a change_log row per applied write.
 */
@Service
class BodyWeightSyncService(
    private val repository: BodyWeightEntryRepository,
    private val revisionChecker: RevisionChecker,
    private val changeLogWriter: ChangeLogWriter,
    private val advisoryLock: AdvisoryLock,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(BodyWeightSyncService::class.java)

    fun findAllByIds(uid: String, ids: Collection<String>): List<BodyWeightEntryDto> = repository.findAllByIds(uid, ids)

    fun apply(uid: String, dto: BodyWeightEntrySyncDto, traceId: String): MutationResultDto {
        val type = EntityType.BODY_WEIGHT_ENTRY
        val isDelete = dto.deletedAt != null
        validate(dto, isDelete)?.let { reason ->
            SyncAudit.log(log, traceId, uid, type, dto.id, SyncStatus.INVALID, null, null, reason)
            return MutationResultDto(type, dto.id, SyncStatus.INVALID, reason = reason)
        }

        return try {
            val (result, revisionBefore) = transactionTemplate.execute {
                advisoryLock.acquireForUser(uid)
                val stored = repository.find(dto.id)

                if (stored != null && stored.ownerId != uid) {
                    return@execute MutationResultDto(type, dto.id, SyncStatus.FORBIDDEN, reason = "Not owned by caller.") to stored.revision
                }

                val now = System.currentTimeMillis()
                val resolvedDeletedAt = if (isDelete) now else null
                val operation = if (isDelete) ChangeOperation.DELETE else ChangeOperation.UPSERT
                val decision = revisionChecker.decide(
                    storedRevision = stored?.revision,
                    clientRevision = dto.revision,
                    storedContent = stored?.dto?.forComparison(),
                    incomingContent = dto.forComparison(),
                )

                val outcome = when (decision) {
                    is RevisionDecision.Insert -> {
                        repository.insert(uid, dto, now, resolvedDeletedAt)
                        changeLogWriter.record(uid, type, dto.id, operation)
                        MutationResultDto(type, dto.id, SyncStatus.APPLIED, updatedAt = now, revision = 1)
                    }

                    is RevisionDecision.Update -> {
                        repository.update(dto, decision.resultingRevision, now, resolvedDeletedAt)
                        changeLogWriter.record(uid, type, dto.id, operation)
                        MutationResultDto(type, dto.id, SyncStatus.APPLIED, updatedAt = now, revision = decision.resultingRevision)
                    }

                    is RevisionDecision.IdempotentReplay ->
                        MutationResultDto(type, dto.id, SyncStatus.APPLIED, updatedAt = stored!!.updatedAt, revision = decision.storedRevision)

                    is RevisionDecision.Conflict ->
                        MutationResultDto(
                            type, dto.id, SyncStatus.CONFLICT,
                            reason = "Server has revision ${decision.storedRevision}; client sent ${dto.revision}.",
                        )
                }
                outcome to stored?.revision
            }!!

            SyncAudit.log(log, traceId, uid, type, dto.id, result.status, revisionBefore, result.revision, result.reason)
            result
        } catch (e: Exception) {
            log.error("sync push [traceId={}] uid={} entityType=BODY_WEIGHT_ENTRY entityId={} unexpected failure", traceId, uid, dto.id, e)
            MutationResultDto(type, dto.id, SyncStatus.ERROR, reason = "Unexpected server error while processing this entity. Retry later.")
        }
    }

    /** A delete-tombstone push skips business-rule validation - see WorkoutSessionSyncService.validate. */
    private fun validate(dto: BodyWeightEntrySyncDto, isDelete: Boolean): String? {
        if (isDelete) return null
        if (!(dto.weightKg > 0f && dto.weightKg <= MAX_WEIGHT_KG)) return "weightKg must be > 0 and <= $MAX_WEIGHT_KG"
        if (dto.measuredAt <= 0L) return "measuredAt is required"
        return null
    }

    private companion object {
        const val MAX_WEIGHT_KG = 700f
    }
}

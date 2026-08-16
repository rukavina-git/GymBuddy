package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.ExerciseSyncDto
import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.forComparison
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.EntityType
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.persistence.ExerciseSyncRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * Push handling for custom Exercise rows. Default exercises are
 * server-owned reference data (see docs/adr/0002) and are rejected
 * outright before any DB access, regardless of who owns the id and
 * regardless of whether the push is a create/update or a delete.
 */
@Service
class ExerciseSyncService(
    private val repository: ExerciseSyncRepository,
    private val revisionChecker: RevisionChecker,
    private val changeLogWriter: ChangeLogWriter,
    private val advisoryLock: AdvisoryLock,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(ExerciseSyncService::class.java)

    fun apply(uid: String, dto: ExerciseSyncDto, traceId: String): MutationResultDto {
        if (dto.source == EntitySource.DEFAULT) {
            val reason = "Default exercises are server-owned and cannot be pushed."
            SyncAudit.log(log, traceId, uid, EntityType.EXERCISE, dto.id, SyncStatus.FORBIDDEN, null, null, reason)
            return MutationResultDto(EntityType.EXERCISE, dto.id, SyncStatus.FORBIDDEN, reason = reason)
        }
        val isDelete = dto.deletedAt != null
        validate(dto, isDelete)?.let { reason ->
            SyncAudit.log(log, traceId, uid, EntityType.EXERCISE, dto.id, SyncStatus.INVALID, null, null, reason)
            return MutationResultDto(EntityType.EXERCISE, dto.id, SyncStatus.INVALID, reason = reason)
        }

        return try {
            val (result, revisionBefore) = transactionTemplate.execute {
                advisoryLock.acquireForUser(uid)
                val stored = repository.find(dto.id)

                if (stored != null && stored.ownerId != uid) {
                    return@execute MutationResultDto(EntityType.EXERCISE, dto.id, SyncStatus.FORBIDDEN, reason = "Not owned by caller.") to stored.revision
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
                        changeLogWriter.record(uid, EntityType.EXERCISE, dto.id, operation)
                        MutationResultDto(EntityType.EXERCISE, dto.id, SyncStatus.APPLIED, updatedAt = now, revision = 1)
                    }

                    is RevisionDecision.Update -> {
                        repository.update(dto, decision.resultingRevision, now, resolvedDeletedAt)
                        changeLogWriter.record(uid, EntityType.EXERCISE, dto.id, operation)
                        MutationResultDto(EntityType.EXERCISE, dto.id, SyncStatus.APPLIED, updatedAt = now, revision = decision.resultingRevision)
                    }

                    is RevisionDecision.IdempotentReplay ->
                        MutationResultDto(EntityType.EXERCISE, dto.id, SyncStatus.APPLIED, updatedAt = stored!!.updatedAt, revision = decision.storedRevision)

                    is RevisionDecision.Conflict ->
                        MutationResultDto(
                            EntityType.EXERCISE, dto.id, SyncStatus.CONFLICT,
                            reason = "Server has revision ${decision.storedRevision}; client sent ${dto.revision}.",
                        )
                }
                outcome to stored?.revision
            }!!

            SyncAudit.log(log, traceId, uid, EntityType.EXERCISE, dto.id, result.status, revisionBefore, result.revision, result.reason)
            result
        } catch (e: Exception) {
            log.error("sync push [traceId={}] uid={} entityType=EXERCISE entityId={} unexpected failure", traceId, uid, dto.id, e)
            MutationResultDto(EntityType.EXERCISE, dto.id, SyncStatus.ERROR, reason = "Unexpected server error while processing this entity. Retry later.")
        }
    }

    /** A delete-tombstone push (deletedAt set) skips business-rule validation - see WorkoutSessionSyncService.validate. */
    private fun validate(dto: ExerciseSyncDto, isDelete: Boolean): String? {
        if (isDelete) return null
        if (dto.name.isBlank()) return "name is required"
        return null
    }
}

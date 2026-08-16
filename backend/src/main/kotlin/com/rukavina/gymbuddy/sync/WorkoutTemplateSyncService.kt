package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.WorkoutTemplateSyncDto
import com.rukavina.gymbuddy.api.dto.forComparison
import com.rukavina.gymbuddy.domain.EntitySource
import com.rukavina.gymbuddy.domain.EntityType
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.persistence.WorkoutTemplateSyncRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * Push handling for the WorkoutTemplate aggregate (template + template
 * exercises). Same shape as WorkoutSessionSyncService: default
 * templates are server-owned and rejected outright (including a delete
 * attempt), aggregate children are replaced wholesale on every
 * insert/update.
 */
@Service
class WorkoutTemplateSyncService(
    private val repository: WorkoutTemplateSyncRepository,
    private val revisionChecker: RevisionChecker,
    private val changeLogWriter: ChangeLogWriter,
    private val advisoryLock: AdvisoryLock,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(WorkoutTemplateSyncService::class.java)

    fun apply(uid: String, dto: WorkoutTemplateSyncDto, traceId: String): MutationResultDto {
        if (dto.source == EntitySource.DEFAULT) {
            val reason = "Default templates are server-owned and cannot be pushed."
            SyncAudit.log(log, traceId, uid, EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.FORBIDDEN, null, null, reason)
            return MutationResultDto(EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.FORBIDDEN, reason = reason)
        }
        val isDelete = dto.deletedAt != null
        validate(dto, isDelete)?.let { reason ->
            SyncAudit.log(log, traceId, uid, EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.INVALID, null, null, reason)
            return MutationResultDto(EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.INVALID, reason = reason)
        }

        return try {
            val (result, revisionBefore) = transactionTemplate.execute {
                advisoryLock.acquireForUser(uid)
                val stored = repository.find(dto.id)

                if (stored != null && stored.ownerId != uid) {
                    return@execute MutationResultDto(EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.FORBIDDEN, reason = "Not owned by caller.") to stored.revision
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
                        changeLogWriter.record(uid, EntityType.WORKOUT_TEMPLATE, dto.id, operation)
                        MutationResultDto(EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.APPLIED, updatedAt = now, revision = 1)
                    }

                    is RevisionDecision.Update -> {
                        repository.update(dto, decision.resultingRevision, now, resolvedDeletedAt)
                        changeLogWriter.record(uid, EntityType.WORKOUT_TEMPLATE, dto.id, operation)
                        MutationResultDto(EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.APPLIED, updatedAt = now, revision = decision.resultingRevision)
                    }

                    is RevisionDecision.IdempotentReplay ->
                        MutationResultDto(EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.APPLIED, updatedAt = stored!!.updatedAt, revision = decision.storedRevision)

                    is RevisionDecision.Conflict ->
                        MutationResultDto(
                            EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.CONFLICT,
                            reason = "Server has revision ${decision.storedRevision}; client sent ${dto.revision}.",
                        )
                }
                outcome to stored?.revision
            }!!

            SyncAudit.log(log, traceId, uid, EntityType.WORKOUT_TEMPLATE, dto.id, result.status, revisionBefore, result.revision, result.reason)
            result
        } catch (e: Exception) {
            log.error("sync push [traceId={}] uid={} entityType=WORKOUT_TEMPLATE entityId={} unexpected failure", traceId, uid, dto.id, e)
            MutationResultDto(EntityType.WORKOUT_TEMPLATE, dto.id, SyncStatus.ERROR, reason = "Unexpected server error while processing this entity. Retry later.")
        }
    }

    /** A delete-tombstone push (deletedAt set) skips business-rule validation - see WorkoutSessionSyncService.validate. */
    private fun validate(dto: WorkoutTemplateSyncDto, isDelete: Boolean): String? {
        if (isDelete) return null
        if (dto.title.isBlank()) return "title is required"
        for (te in dto.templateExercises) {
            if (te.plannedSets <= 0) return "templateExercises[${te.id}].plannedSets must be greater than 0"
        }
        return null
    }
}

package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.WorkoutSessionSyncDto
import com.rukavina.gymbuddy.api.dto.forComparison
import com.rukavina.gymbuddy.domain.EntityType
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.domain.validation.WorkoutSetValidationResult
import com.rukavina.gymbuddy.domain.validation.WorkoutSetValidator
import com.rukavina.gymbuddy.persistence.WorkoutSessionSyncRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * Push handling for the WorkoutSession aggregate (session + performed
 * exercises + sets). Owns its own validation, aggregate replacement and
 * ownership check; delegates the revision decision, the change_log
 * write, and per-user serialisation to the shared
 * RevisionChecker/ChangeLogWriter/AdvisoryLock - the only implementation
 * of any of those three, reused identically by every other sync service
 * in this package.
 */
@Service
class WorkoutSessionSyncService(
    private val repository: WorkoutSessionSyncRepository,
    private val revisionChecker: RevisionChecker,
    private val changeLogWriter: ChangeLogWriter,
    private val advisoryLock: AdvisoryLock,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(WorkoutSessionSyncService::class.java)

    fun apply(uid: String, dto: WorkoutSessionSyncDto, traceId: String): MutationResultDto {
        val isDelete = dto.deletedAt != null
        validate(dto, isDelete)?.let { reason ->
            SyncAudit.log(log, traceId, uid, EntityType.WORKOUT_SESSION, dto.id, SyncStatus.INVALID, null, null, reason)
            return MutationResultDto(EntityType.WORKOUT_SESSION, dto.id, SyncStatus.INVALID, reason = reason)
        }

        return try {
            val (result, revisionBefore) = transactionTemplate.execute {
                advisoryLock.acquireForUser(uid)
                val stored = repository.find(dto.id)

                if (stored != null && stored.ownerId != uid) {
                    return@execute MutationResultDto(EntityType.WORKOUT_SESSION, dto.id, SyncStatus.FORBIDDEN, reason = "Not owned by caller.") to stored.revision
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
                        changeLogWriter.record(uid, EntityType.WORKOUT_SESSION, dto.id, operation)
                        MutationResultDto(EntityType.WORKOUT_SESSION, dto.id, SyncStatus.APPLIED, updatedAt = now, revision = 1)
                    }

                    is RevisionDecision.Update -> {
                        repository.update(dto, decision.resultingRevision, now, resolvedDeletedAt)
                        changeLogWriter.record(uid, EntityType.WORKOUT_SESSION, dto.id, operation)
                        MutationResultDto(EntityType.WORKOUT_SESSION, dto.id, SyncStatus.APPLIED, updatedAt = now, revision = decision.resultingRevision)
                    }

                    is RevisionDecision.IdempotentReplay ->
                        MutationResultDto(EntityType.WORKOUT_SESSION, dto.id, SyncStatus.APPLIED, updatedAt = stored!!.updatedAt, revision = decision.storedRevision)

                    is RevisionDecision.Conflict ->
                        MutationResultDto(
                            EntityType.WORKOUT_SESSION, dto.id, SyncStatus.CONFLICT,
                            reason = "Server has revision ${decision.storedRevision}; client sent ${dto.revision}.",
                        )
                }
                outcome to stored?.revision
            }!!

            SyncAudit.log(log, traceId, uid, EntityType.WORKOUT_SESSION, dto.id, result.status, revisionBefore, result.revision, result.reason)
            result
        } catch (e: Exception) {
            log.error("sync push [traceId={}] uid={} entityType=WORKOUT_SESSION entityId={} unexpected failure", traceId, uid, dto.id, e)
            MutationResultDto(EntityType.WORKOUT_SESSION, dto.id, SyncStatus.ERROR, reason = "Unexpected server error while processing this entity. Retry later.")
        }
    }

    /**
     * A delete-tombstone push (deletedAt set) skips business-rule
     * validation entirely: the data is being discarded, so a stale or
     * locally-invalid copy must not block the delete from applying.
     */
    private fun validate(dto: WorkoutSessionSyncDto, isDelete: Boolean): String? {
        if (isDelete) return null
        if (dto.title.isBlank()) return "title is required"
        if (dto.durationSeconds < 0) return "durationSeconds cannot be negative"
        for (pe in dto.performedExercises) {
            if (pe.exerciseName.isBlank()) return "performedExercises[${pe.id}].exerciseName is required"
            for (set in pe.sets) {
                val result = WorkoutSetValidator.validate(set, pe.exerciseTrackingType)
                if (result is WorkoutSetValidationResult.Invalid) {
                    return "performedExercises[${pe.id}].sets[${set.id}]: ${result.reason}"
                }
            }
        }
        return null
    }
}

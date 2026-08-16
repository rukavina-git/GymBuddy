package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.UserExerciseStateSyncDto
import com.rukavina.gymbuddy.api.dto.UserTemplateStateSyncDto
import com.rukavina.gymbuddy.domain.EntityType
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.persistence.OverlaySyncRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

/**
 * Push handling for both overlay tables (user_exercise_state,
 * user_template_state) - sparse, upsert-only, no aggregate children, no
 * deletedAt. Their primary key is (owner_id, <ref>_id) so a row can
 * never belong to anyone but the caller; there is no ownership check to
 * perform, unlike the other four sync services.
 */
@Service
class OverlaySyncService(
    private val repository: OverlaySyncRepository,
    private val revisionChecker: RevisionChecker,
    private val changeLogWriter: ChangeLogWriter,
    private val advisoryLock: AdvisoryLock,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(OverlaySyncService::class.java)

    fun applyExerciseState(uid: String, dto: UserExerciseStateSyncDto, traceId: String): MutationResultDto {
        validateExerciseState(dto)?.let { reason ->
            SyncAudit.log(log, traceId, uid, EntityType.USER_EXERCISE_STATE, dto.exerciseId, SyncStatus.INVALID, null, null, reason)
            return MutationResultDto(EntityType.USER_EXERCISE_STATE, dto.exerciseId, SyncStatus.INVALID, reason = reason)
        }

        return try {
            val (result, revisionBefore) = transactionTemplate.execute {
                advisoryLock.acquireForUser(uid)
                val stored = repository.findExerciseState(uid, dto.exerciseId)
                val now = System.currentTimeMillis()
                val decision = revisionChecker.decide(
                    storedRevision = stored?.revision,
                    clientRevision = dto.revision,
                    storedContent = stored?.dto?.copy(revision = 0),
                    incomingContent = dto.copy(revision = 0),
                )

                val outcome = when (decision) {
                    is RevisionDecision.Insert -> {
                        repository.insertExerciseState(uid, dto, now)
                        changeLogWriter.record(uid, EntityType.USER_EXERCISE_STATE, dto.exerciseId)
                        MutationResultDto(EntityType.USER_EXERCISE_STATE, dto.exerciseId, SyncStatus.APPLIED, updatedAt = now, revision = 1)
                    }

                    is RevisionDecision.Update -> {
                        repository.updateExerciseState(uid, dto, decision.resultingRevision, now)
                        changeLogWriter.record(uid, EntityType.USER_EXERCISE_STATE, dto.exerciseId)
                        MutationResultDto(EntityType.USER_EXERCISE_STATE, dto.exerciseId, SyncStatus.APPLIED, updatedAt = now, revision = decision.resultingRevision)
                    }

                    is RevisionDecision.IdempotentReplay ->
                        MutationResultDto(EntityType.USER_EXERCISE_STATE, dto.exerciseId, SyncStatus.APPLIED, updatedAt = stored!!.updatedAt, revision = decision.storedRevision)

                    is RevisionDecision.Conflict ->
                        MutationResultDto(
                            EntityType.USER_EXERCISE_STATE, dto.exerciseId, SyncStatus.CONFLICT,
                            reason = "Server has revision ${decision.storedRevision}; client sent ${dto.revision}.",
                        )
                }
                outcome to stored?.revision
            }!!

            SyncAudit.log(log, traceId, uid, EntityType.USER_EXERCISE_STATE, dto.exerciseId, result.status, revisionBefore, result.revision, result.reason)
            result
        } catch (e: Exception) {
            log.error("sync push [traceId={}] uid={} entityType=USER_EXERCISE_STATE entityId={} unexpected failure", traceId, uid, dto.exerciseId, e)
            MutationResultDto(EntityType.USER_EXERCISE_STATE, dto.exerciseId, SyncStatus.ERROR, reason = "Unexpected server error while processing this entity. Retry later.")
        }
    }

    fun applyTemplateState(uid: String, dto: UserTemplateStateSyncDto, traceId: String): MutationResultDto {
        return try {
            val (result, revisionBefore) = transactionTemplate.execute {
                advisoryLock.acquireForUser(uid)
                val stored = repository.findTemplateState(uid, dto.templateId)
                val now = System.currentTimeMillis()
                val decision = revisionChecker.decide(
                    storedRevision = stored?.revision,
                    clientRevision = dto.revision,
                    storedContent = stored?.dto?.copy(revision = 0),
                    incomingContent = dto.copy(revision = 0),
                )

                val outcome = when (decision) {
                    is RevisionDecision.Insert -> {
                        repository.insertTemplateState(uid, dto, now)
                        changeLogWriter.record(uid, EntityType.USER_TEMPLATE_STATE, dto.templateId)
                        MutationResultDto(EntityType.USER_TEMPLATE_STATE, dto.templateId, SyncStatus.APPLIED, updatedAt = now, revision = 1)
                    }

                    is RevisionDecision.Update -> {
                        repository.updateTemplateState(uid, dto, decision.resultingRevision, now)
                        changeLogWriter.record(uid, EntityType.USER_TEMPLATE_STATE, dto.templateId)
                        MutationResultDto(EntityType.USER_TEMPLATE_STATE, dto.templateId, SyncStatus.APPLIED, updatedAt = now, revision = decision.resultingRevision)
                    }

                    is RevisionDecision.IdempotentReplay ->
                        MutationResultDto(EntityType.USER_TEMPLATE_STATE, dto.templateId, SyncStatus.APPLIED, updatedAt = stored!!.updatedAt, revision = decision.storedRevision)

                    is RevisionDecision.Conflict ->
                        MutationResultDto(
                            EntityType.USER_TEMPLATE_STATE, dto.templateId, SyncStatus.CONFLICT,
                            reason = "Server has revision ${decision.storedRevision}; client sent ${dto.revision}.",
                        )
                }
                outcome to stored?.revision
            }!!

            SyncAudit.log(log, traceId, uid, EntityType.USER_TEMPLATE_STATE, dto.templateId, result.status, revisionBefore, result.revision, result.reason)
            result
        } catch (e: Exception) {
            log.error("sync push [traceId={}] uid={} entityType=USER_TEMPLATE_STATE entityId={} unexpected failure", traceId, uid, dto.templateId, e)
            MutationResultDto(EntityType.USER_TEMPLATE_STATE, dto.templateId, SyncStatus.ERROR, reason = "Unexpected server error while processing this entity. Retry later.")
        }
    }

    private fun validateExerciseState(dto: UserExerciseStateSyncDto): String? {
        if ((dto.defaultRestSeconds ?: 0) < 0) return "defaultRestSeconds cannot be negative"
        return null
    }
}

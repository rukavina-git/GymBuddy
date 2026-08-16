package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.api.dto.ErrorDto
import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.currentUid
import com.rukavina.gymbuddy.sync.ExerciseSyncService
import com.rukavina.gymbuddy.sync.OverlaySyncService
import com.rukavina.gymbuddy.sync.ProfileSyncService
import com.rukavina.gymbuddy.sync.WorkoutSessionSyncService
import com.rukavina.gymbuddy.sync.WorkoutTemplateSyncService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * POST /v1/sync/push per api/openapi.yaml. Thin by design: it does not
 * validate, mutate, or touch the database itself - it dispatches each
 * array in the batch to the per-type service that owns that entity's
 * validation, aggregate handling, and ownership checks, and concatenates
 * their per-entity results. A bug in one entity type's handling cannot
 * corrupt another's, because there is nothing shared here to corrupt.
 *
 * Every entity is applied in its own transaction inside the per-type
 * services, so one bad entity never aborts the batch - this method
 * always returns 200 with a result per entity, even if every one of
 * them failed. See sync/AdvisoryLock.kt, sync/RevisionChecker.kt and
 * sync/ChangeLogWriter.kt for the shared single-implementation pieces
 * every one of those services delegates to.
 */
@RestController
class SyncController(
    private val workoutSessionSyncService: WorkoutSessionSyncService,
    private val exerciseSyncService: ExerciseSyncService,
    private val workoutTemplateSyncService: WorkoutTemplateSyncService,
    private val overlaySyncService: OverlaySyncService,
    private val profileSyncService: ProfileSyncService,
    @Value("\${gymbuddy.sync.read-only:false}") private val readOnly: Boolean,
) {
    private val log = LoggerFactory.getLogger(SyncController::class.java)

    @PostMapping("/v1/sync/push")
    fun push(@RequestBody request: PushRequestDto): ResponseEntity<Any> {
        val uid = currentUid()

        if (readOnly) {
            val traceId = UUID.randomUUID().toString()
            log.warn("503 SYNC_READ_ONLY [traceId={}] uid={}", traceId, uid)
            val body = ErrorDto(error = "SYNC_READ_ONLY", message = "Sync writes are temporarily disabled.", traceId = traceId)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body)
        }

        val traceId = UUID.randomUUID().toString()
        val results = mutableListOf<MutationResultDto>()

        request.workoutSessions.forEach { results += workoutSessionSyncService.apply(uid, it, traceId) }
        request.exercises.forEach { results += exerciseSyncService.apply(uid, it, traceId) }
        request.workoutTemplates.forEach { results += workoutTemplateSyncService.apply(uid, it, traceId) }
        request.userExerciseStates.forEach { results += overlaySyncService.applyExerciseState(uid, it, traceId) }
        request.userTemplateStates.forEach { results += overlaySyncService.applyTemplateState(uid, it, traceId) }
        request.userProfile?.let { results += profileSyncService.apply(uid, it, traceId) }

        return ResponseEntity.ok(PushResponseDto(results))
    }
}

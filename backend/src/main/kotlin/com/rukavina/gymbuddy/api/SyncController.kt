package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.api.dto.ErrorDto
import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.PullResponseDto
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.currentUid
import com.rukavina.gymbuddy.sync.ExerciseSyncService
import com.rukavina.gymbuddy.sync.OverlaySyncService
import com.rukavina.gymbuddy.sync.ProfileSyncService
import com.rukavina.gymbuddy.sync.SyncPullOrchestrator
import com.rukavina.gymbuddy.sync.WorkoutSessionSyncService
import com.rukavina.gymbuddy.sync.WorkoutTemplateSyncService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * POST /v1/sync/push and GET /v1/sync/pull per api/openapi.yaml. Thin by
 * design: it does not validate, mutate, or read the database itself -
 * push dispatches each array in the batch to the per-type service that
 * owns that entity's validation, aggregate handling, and ownership
 * checks, and pull delegates entirely to SyncPullOrchestrator. A bug in
 * one entity type's handling cannot corrupt another's, because there is
 * nothing shared here to corrupt.
 *
 * Every entity is applied in its own transaction inside the per-type
 * services, so one bad entity never aborts the push batch - that method
 * always returns 200 with a result per entity, even if every one of
 * them failed. See sync/AdvisoryLock.kt, sync/RevisionChecker.kt and
 * sync/ChangeLogWriter.kt for the shared single-implementation pieces
 * every push service delegates to; see sync/CursorCodec.kt and
 * sync/ChangeLogReader.kt for pull's equivalents.
 */
@RestController
class SyncController(
    private val workoutSessionSyncService: WorkoutSessionSyncService,
    private val exerciseSyncService: ExerciseSyncService,
    private val workoutTemplateSyncService: WorkoutTemplateSyncService,
    private val overlaySyncService: OverlaySyncService,
    private val profileSyncService: ProfileSyncService,
    private val syncPullOrchestrator: SyncPullOrchestrator,
    @Value("\${gymbuddy.sync.read-only:false}") private val readOnly: Boolean,
    @Value("\${gymbuddy.sync.pull-page-size-default:200}") private val defaultPullLimit: Int,
    @Value("\${gymbuddy.sync.pull-page-size-max:500}") private val maxPullLimit: Int,
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

    /**
     * GET /v1/sync/pull per api/openapi.yaml. Thin like push: cursor
     * decoding, the delta-vs-full-sync decision, expiry, and assembling
     * the per-type arrays all live in SyncPullOrchestrator (and the
     * shared CursorCodec/ChangeLogReader/FullSyncEntityDiscovery it
     * delegates to) - this method only resolves the caller and the
     * effective page size. Not affected by read-only mode: that
     * safeguard only stops writes (see push above).
     *
     * limit is silently clamped into [1, pull-page-size-max] rather than
     * rejected outright - an out-of-range value from an older client
     * after a server-side config change shouldn't hard-fail a read.
     */
    @GetMapping("/v1/sync/pull")
    fun pull(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false) limit: Int?,
    ): PullResponseDto {
        val uid = currentUid()
        val effectiveLimit = (limit ?: defaultPullLimit).coerceIn(1, maxPullLimit)
        return syncPullOrchestrator.pull(uid, cursor, effectiveLimit)
    }
}

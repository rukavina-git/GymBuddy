package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.api.dto.PullResponseDto
import com.rukavina.gymbuddy.domain.EntityType
import org.springframework.stereotype.Service

/**
 * All of GET /v1/sync/pull's decision-making: delta vs. full sync,
 * cursor expiry, and assembling the per-type results into one response.
 * PullController stays a thin dispatcher (mirroring SyncController for
 * push) by delegating everything here - written once, since none of
 * this logic is per-type.
 *
 * Full-sync continuation: a completed full sync (point 4) hands back a
 * real Cursor.Delta wrapping change_log's current max seq, so the
 * client transitions to ordinary delta pulls afterwards without knowing
 * anything changed. But full sync doesn't enumerate entities via
 * change_log at all (point 4's whole reason for existing - see
 * FullSyncEntityDiscovery), so an IN-PROGRESS full sync has nothing
 * change_log-shaped to hand back for its next page; it hands back a
 * Cursor.FullSync instead, carrying the offset into
 * FullSyncEntityDiscovery's (type, id) order. See CursorCodec for why
 * that's an explicit variant in the wire format rather than a sentinel
 * folded into Cursor.Delta.seq.
 */
@Service
class SyncPullOrchestrator(
    private val cursorCodec: CursorCodec,
    private val changeLogReader: ChangeLogReader,
    private val fullSyncEntityDiscovery: FullSyncEntityDiscovery,
    private val workoutSessionPullService: WorkoutSessionPullService,
    private val exercisePullService: ExercisePullService,
    private val workoutTemplatePullService: WorkoutTemplatePullService,
    private val overlayPullService: OverlayPullService,
    private val profilePullService: ProfilePullService,
) {

    fun pull(uid: String, cursorParam: String?, limit: Int): PullResponseDto {
        return when (val cursor = cursorParam?.let { cursorCodec.decode(it) }) {
            null -> fullSync(uid, offset = 0, limit)
            is Cursor.FullSync -> fullSync(uid, cursor.offset, limit)
            is Cursor.Delta -> deltaPull(uid, cursor.seq, limit)
        }
    }

    private fun deltaPull(uid: String, afterSeq: Long, limit: Int): PullResponseDto {
        if (changeLogReader.isCursorExpired(uid, afterSeq)) {
            throw CursorExpiredException("Cursor (seq=$afterSeq) predates the retained change log for uid=$uid.")
        }
        val page = changeLogReader.fetchDeltaPage(uid, afterSeq, limit)
        return assemble(uid, page.entities, cursorCodec.encode(Cursor.Delta(page.nextCursorSeq)), page.hasMore)
    }

    private fun fullSync(uid: String, offset: Long, limit: Int): PullResponseDto {
        val page = fullSyncEntityDiscovery.fetchPage(uid, offset, limit)
        val nextCursor = if (page.hasMore) {
            cursorCodec.encode(Cursor.FullSync(offset + page.entities.size))
        } else {
            cursorCodec.encode(Cursor.Delta(changeLogReader.currentMaxSeq(uid) ?: 0L))
        }
        return assemble(uid, page.entities, nextCursor, page.hasMore)
    }

    private fun assemble(uid: String, refs: List<EntityRef>, nextCursor: String, hasMore: Boolean): PullResponseDto {
        val idsByType = refs.groupBy(keySelector = { it.entityType }, valueTransform = { it.entityId })

        return PullResponseDto(
            workoutSessions = workoutSessionPullService.findAllByIds(uid, idsByType[EntityType.WORKOUT_SESSION].orEmpty()),
            exercises = exercisePullService.findAllByIds(uid, idsByType[EntityType.EXERCISE].orEmpty()),
            workoutTemplates = workoutTemplatePullService.findAllByIds(uid, idsByType[EntityType.WORKOUT_TEMPLATE].orEmpty()),
            userExerciseStates = overlayPullService.findAllExerciseStatesByIds(uid, idsByType[EntityType.USER_EXERCISE_STATE].orEmpty()),
            userTemplateStates = overlayPullService.findAllTemplateStatesByIds(uid, idsByType[EntityType.USER_TEMPLATE_STATE].orEmpty()),
            userProfile = if (idsByType.containsKey(EntityType.USER_PROFILE)) profilePullService.find(uid) else null,
            nextCursor = nextCursor,
            hasMore = hasMore,
        )
    }
}

package com.rukavina.gymbuddy.data.sync

import androidx.room.withTransaction
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.entity.ExerciseVersionEntity
import com.rukavina.gymbuddy.data.local.entity.OutboxEntryEntity
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.entity.SyncLogEntity
import com.rukavina.gymbuddy.data.local.entity.SyncLogOutcome
import com.rukavina.gymbuddy.data.local.entity.SyncMetaEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateVersionEntity
import com.rukavina.gymbuddy.data.remote.generated.infrastructure.Serializer
import com.rukavina.gymbuddy.data.remote.generated.models.EntitySource
import com.rukavina.gymbuddy.data.remote.generated.models.Exercise
import com.rukavina.gymbuddy.data.remote.generated.models.PullResponse
import com.rukavina.gymbuddy.data.remote.generated.models.UserExerciseState
import com.rukavina.gymbuddy.data.remote.generated.models.UserProfile
import com.rukavina.gymbuddy.data.remote.generated.models.UserTemplateState
import com.rukavina.gymbuddy.data.remote.generated.models.WorkoutSession
import com.rukavina.gymbuddy.data.remote.generated.models.WorkoutTemplate
import com.rukavina.gymbuddy.domain.model.SyncState
import java.time.Clock

/** One outbox entry loaded and serialised for a push. */
sealed class PreparedMutation(val entry: OutboxEntryEntity) {
    abstract val payloadJson: String

    class Session(entry: OutboxEntryEntity, val dto: WorkoutSession) : PreparedMutation(entry) {
        override val payloadJson: String get() = json(dto)
    }
    class ExerciseRow(entry: OutboxEntryEntity, val dto: Exercise) : PreparedMutation(entry) {
        override val payloadJson: String get() = json(dto)
    }
    class Template(entry: OutboxEntryEntity, val dto: WorkoutTemplate) : PreparedMutation(entry) {
        override val payloadJson: String get() = json(dto)
    }
    class ExerciseState(entry: OutboxEntryEntity, val dto: UserExerciseState) : PreparedMutation(entry) {
        override val payloadJson: String get() = json(dto)
    }
    class TemplateState(entry: OutboxEntryEntity, val dto: UserTemplateState) : PreparedMutation(entry) {
        override val payloadJson: String get() = json(dto)
    }
    class Profile(entry: OutboxEntryEntity, val dto: UserProfile) : PreparedMutation(entry) {
        override val payloadJson: String get() = json(dto)
    }

    companion object {
        inline fun <reified T> json(value: T): String = Serializer.moshi.adapter(T::class.java).toJson(value)
    }
}

/** What preparing an outbox entry produced. */
sealed class PrepareOutcome {
    data class Ready(val mutation: PreparedMutation) : PrepareOutcome()
    /** The entity no longer exists locally (e.g. wiped); the entry is stale. */
    data object Missing : PrepareOutcome()
    /** The entity can't be expressed as a DTO (e.g. a non-UUID id); it can never be pushed. */
    data class Unsendable(val reason: String) : PrepareOutcome()
}

/** Per-type counts of entities a pull wrote locally. A deletion counts as an ordinary apply. */
data class PageApplyCounts(val applied: Map<SyncEntityType, Int>, val skippedPending: Int)

/**
 * All of the sync engine's Room access. Every method that writes runs
 * in (or is) one transaction, so the engine never leaves a half-applied
 * push result or pull page behind.
 */
class SyncLocalStore(private val db: AppDatabase, private val clock: Clock) {

    private val syncDao get() = db.syncDao()

    suspend fun <R> transaction(block: suspend () -> R): R = db.withTransaction { block() }

    // ---- outbox ----

    suspend fun outbox(): List<OutboxEntryEntity> = syncDao.getOutbox()

    suspend fun outboxCount(): Int = syncDao.outboxCount()

    suspend fun pendingCountsByType(): Map<SyncEntityType, Int> =
        syncDao.getOutbox().groupingBy { it.entityType }.eachCount()

    suspend fun dropEntry(entry: OutboxEntryEntity) = syncDao.deleteOutboxEntry(entry.entityType, entry.entityId)

    suspend fun prepare(entry: OutboxEntryEntity): PrepareOutcome = try {
        load(entry)?.let { PrepareOutcome.Ready(it) } ?: PrepareOutcome.Missing
    } catch (e: IllegalArgumentException) {
        PrepareOutcome.Unsendable(e.message ?: e.toString())
    }

    private suspend fun load(entry: OutboxEntryEntity): PreparedMutation? {
        val id = entry.entityId
        return when (entry.entityType) {
            SyncEntityType.WORKOUT_SESSION -> syncDao.getSessionIncludingDeleted(id)?.let {
                PreparedMutation.Session(entry, SyncDtoMapper.toDto(it, db.workoutSessionDao().getPerformedExercisesWithSets(id)))
            }
            SyncEntityType.EXERCISE -> syncDao.getExerciseIncludingDeleted(id)?.let {
                PreparedMutation.ExerciseRow(entry, SyncDtoMapper.toDto(it))
            }
            SyncEntityType.WORKOUT_TEMPLATE -> syncDao.getTemplateIncludingDeleted(id)?.let {
                PreparedMutation.Template(entry, SyncDtoMapper.toDto(it, syncDao.getTemplateExercises(id)))
            }
            SyncEntityType.USER_EXERCISE_STATE -> db.userExerciseStateDao().getState(id)?.let {
                PreparedMutation.ExerciseState(entry, SyncDtoMapper.toDto(it))
            }
            SyncEntityType.USER_TEMPLATE_STATE -> db.userTemplateStateDao().getState(id)?.let {
                PreparedMutation.TemplateState(entry, SyncDtoMapper.toDto(it))
            }
            SyncEntityType.USER_PROFILE -> syncDao.getProfileIncludingDeleted(id)?.let {
                PreparedMutation.Profile(entry, SyncDtoMapper.toDto(it))
            }
        }
    }

    /** Current local state as JSON, for the discarded-change log. Null if the row is gone or unmappable. */
    private suspend fun currentPayload(type: SyncEntityType, id: String): String? {
        val entry = OutboxEntryEntity(type, id, OutboxOperation.UPSERT, 0, 0)
        return runCatching { load(entry)?.payloadJson }.getOrNull()
    }

    // ---- push results ----

    /**
     * APPLIED: adopt the server's revision and updatedAt. The outbox row
     * is cleared only if nothing was written locally while the push was
     * in flight; otherwise the newer edit stays queued (on top of the
     * revision just adopted) and the row stays PENDING.
     */
    suspend fun markApplied(entry: OutboxEntryEntity, revision: Int, updatedAt: Long) {
        val cleared = syncDao.deleteOutboxEntryIfUnchanged(entry.entityType, entry.entityId, entry.changeVersion) == 1
        val state = if (cleared) SyncState.SYNCED else SyncState.PENDING
        val id = entry.entityId
        when (entry.entityType) {
            SyncEntityType.WORKOUT_SESSION -> syncDao.setSessionSyncMeta(id, revision, updatedAt, state)
            SyncEntityType.EXERCISE -> syncDao.setExerciseSyncMeta(id, revision, updatedAt, state)
            SyncEntityType.WORKOUT_TEMPLATE -> syncDao.setTemplateSyncMeta(id, revision, updatedAt, state)
            SyncEntityType.USER_EXERCISE_STATE -> syncDao.setExerciseStateSyncMeta(id, revision, updatedAt, state)
            SyncEntityType.USER_TEMPLATE_STATE -> syncDao.setTemplateStateSyncMeta(id, revision, updatedAt, state)
            SyncEntityType.USER_PROFILE -> syncDao.setProfileSyncMeta(id, revision, updatedAt, state)
        }
    }

    /**
     * CONFLICT, INVALID, FORBIDDEN or unsendable: the local change is
     * discarded. It's logged with the entity's current local state, the
     * outbox row is dropped with no retry, and the row is marked
     * CONFLICTED until a pull overwrites it with the server's version.
     */
    suspend fun markRejected(type: SyncEntityType, id: String, outcome: SyncLogOutcome, reason: String?) {
        log(type, id, outcome, reason, currentPayload(type, id))
        syncDao.deleteOutboxEntry(type, id)
        setSyncState(type, id, SyncState.CONFLICTED)
    }

    suspend fun syncStateOf(type: SyncEntityType, id: String): SyncState? = when (type) {
        SyncEntityType.WORKOUT_SESSION -> syncDao.getSessionIncludingDeleted(id)?.syncState
        SyncEntityType.EXERCISE -> syncDao.getExerciseIncludingDeleted(id)?.syncState
        SyncEntityType.WORKOUT_TEMPLATE -> syncDao.getTemplateIncludingDeleted(id)?.syncState
        SyncEntityType.USER_EXERCISE_STATE -> db.userExerciseStateDao().getState(id)?.syncState
        SyncEntityType.USER_TEMPLATE_STATE -> db.userTemplateStateDao().getState(id)?.syncState
        SyncEntityType.USER_PROFILE -> syncDao.getProfileIncludingDeleted(id)?.syncState
    }

    private suspend fun setSyncState(type: SyncEntityType, id: String, state: SyncState) = when (type) {
        SyncEntityType.WORKOUT_SESSION -> syncDao.setSessionSyncState(id, state)
        SyncEntityType.EXERCISE -> syncDao.setExerciseSyncState(id, state)
        SyncEntityType.WORKOUT_TEMPLATE -> syncDao.setTemplateSyncState(id, state)
        SyncEntityType.USER_EXERCISE_STATE -> syncDao.setExerciseStateSyncState(id, state)
        SyncEntityType.USER_TEMPLATE_STATE -> syncDao.setTemplateStateSyncState(id, state)
        SyncEntityType.USER_PROFILE -> syncDao.setProfileSyncState(id, state)
    }

    private suspend fun log(type: SyncEntityType, id: String, outcome: SyncLogOutcome, reason: String?, payload: String?) {
        syncDao.insertLog(
            SyncLogEntity(
                entityType = type,
                entityId = id,
                outcome = outcome,
                reason = reason,
                discardedPayload = payload,
                loggedAt = clock.millis()
            )
        )
    }

    // ---- pull ----

    suspend fun cursor(): String? = syncDao.getMeta(KEY_CURSOR)

    suspend fun setCursor(cursor: String?) = syncDao.putMeta(SyncMetaEntity(KEY_CURSOR, cursor))

    /**
     * Writes one pulled page. Every entity, deleted or not, goes through
     * the same per-type upsert: a deletion is just an entity whose
     * deletedAt is set. Aggregates are replaced whole.
     *
     * The one case a pulled entity is not written: this device has an
     * unpushed change to it whose base revision is at least the pulled
     * one. That change was made on top of what's being delivered, so it
     * is pushed next and the server decides. A pulled revision newer than
     * the local base means the server moved on without this device: the
     * server wins, and the discarded local change is logged.
     */
    suspend fun applyPage(page: PullResponse): PageApplyCounts {
        val applied = mutableMapOf<SyncEntityType, Int>()
        var skipped = 0
        suspend fun upsert(type: SyncEntityType, id: String, revision: Int, write: suspend () -> Unit) {
            if (shouldApply(type, id, revision)) {
                write()
                applied[type] = (applied[type] ?: 0) + 1
            } else {
                skipped++
            }
        }

        page.exercises.orEmpty().forEach { dto ->
            upsert(SyncEntityType.EXERCISE, dto.id.toString(), dto.revision) {
                db.exerciseDao().insertExercise(SyncDtoMapper.toEntity(dto))
            }
        }
        page.workoutTemplates.orEmpty().forEach { dto ->
            upsert(SyncEntityType.WORKOUT_TEMPLATE, dto.id.toString(), dto.revision) {
                val aggregate = SyncDtoMapper.toEntities(dto)
                syncDao.deleteTemplateExercisesForTemplate(aggregate.template.id)
                db.workoutTemplateDao().insertTemplate(aggregate.template)
                db.workoutTemplateDao().insertTemplateExercises(aggregate.templateExercises)
            }
        }
        page.workoutSessions.orEmpty().forEach { dto ->
            upsert(SyncEntityType.WORKOUT_SESSION, dto.id.toString(), dto.revision) {
                val aggregate = SyncDtoMapper.toEntities(dto)
                // Explicit, rather than relying on REPLACE's implicit row
                // delete to fire the cascade. Sets go with their performed
                // exercises via ON DELETE CASCADE.
                syncDao.deletePerformedExercisesForSession(aggregate.session.id)
                db.workoutSessionDao().insertWorkoutSession(aggregate.session)
                db.workoutSessionDao().insertPerformedExercises(aggregate.performedExercises)
                db.workoutSessionDao().insertWorkoutSets(aggregate.sets)
            }
        }
        page.userExerciseStates.orEmpty().forEach { dto ->
            upsert(SyncEntityType.USER_EXERCISE_STATE, dto.exerciseId.toString(), dto.revision) {
                db.userExerciseStateDao().upsert(SyncDtoMapper.toEntity(dto))
            }
        }
        page.userTemplateStates.orEmpty().forEach { dto ->
            upsert(SyncEntityType.USER_TEMPLATE_STATE, dto.templateId.toString(), dto.revision) {
                db.userTemplateStateDao().upsert(SyncDtoMapper.toEntity(dto))
            }
        }
        page.userProfile?.let { dto ->
            upsert(SyncEntityType.USER_PROFILE, dto.uid, dto.revision) {
                db.userProfileDao().insertUserProfile(SyncDtoMapper.toEntity(dto))
            }
        }
        return PageApplyCounts(applied, skipped)
    }

    private suspend fun shouldApply(type: SyncEntityType, id: String, pulledRevision: Int): Boolean {
        val pending = syncDao.getOutboxEntry(type, id) ?: return true
        val localRevision = localRevision(type, id) ?: 0
        if (pulledRevision <= localRevision) return false
        log(type, id, SyncLogOutcome.PULL_OVERWRITE, "Server revision $pulledRevision replaced an unpushed change based on revision $localRevision.", currentPayload(type, id))
        syncDao.deleteOutboxEntry(pending.entityType, pending.entityId)
        return true
    }

    private suspend fun localRevision(type: SyncEntityType, id: String): Int? = when (type) {
        SyncEntityType.WORKOUT_SESSION -> syncDao.getSessionIncludingDeleted(id)?.revision
        SyncEntityType.EXERCISE -> syncDao.getExerciseIncludingDeleted(id)?.revision
        SyncEntityType.WORKOUT_TEMPLATE -> syncDao.getTemplateIncludingDeleted(id)?.revision
        SyncEntityType.USER_EXERCISE_STATE -> db.userExerciseStateDao().getState(id)?.revision
        SyncEntityType.USER_TEMPLATE_STATE -> db.userTemplateStateDao().getState(id)?.revision
        SyncEntityType.USER_PROFILE -> syncDao.getProfileIncludingDeleted(id)?.revision
    }

    // ---- wipe ----

    /** Everything the signed-in user owns, plus the cursor and outbox. Reference data and the discarded-change log stay. */
    suspend fun wipeUserData() = syncDao.wipeLocalUserData()

    /** [wipeUserData] plus the discarded-change log - for logout, when nothing of this user may remain. */
    suspend fun wipeEverythingForLogout() = transaction {
        syncDao.wipeLocalUserData()
        syncDao.wipeLog()
    }

    // ---- reference data ----

    suspend fun exerciseLibraryVersion(): Int? = db.exerciseVersionDao().getCurrentVersion()?.version

    suspend fun templateLibraryVersion(): Int? = db.templateVersionDao().getCurrentVersion()?.version

    /**
     * Replaces every DEFAULT exercise with the server's library in one
     * transaction. CUSTOM exercises and the user_exercise_state overlay
     * are untouched - overlay rows are keyed by exercise id and simply
     * reattach to the new rows.
     */
    suspend fun replaceDefaultExercises(version: Int, exercises: List<Exercise>) = transaction {
        db.exerciseDao().deleteAllDefaultExercises()
        db.exerciseDao().insertExercises(
            exercises.filter { it.source == EntitySource.DEFAULT }.map { SyncDtoMapper.toEntity(it) }
        )
        db.exerciseVersionDao().setVersion(ExerciseVersionEntity(version = version, loadedAt = clock.millis()))
    }

    /** Same as [replaceDefaultExercises] for templates; template_exercises go via ON DELETE CASCADE. */
    suspend fun replaceDefaultTemplates(version: Int, templates: List<WorkoutTemplate>) = transaction {
        syncDao.deleteDefaultTemplates()
        templates.filter { it.source == EntitySource.DEFAULT }.forEach { dto ->
            val aggregate = SyncDtoMapper.toEntities(dto)
            db.workoutTemplateDao().insertTemplate(aggregate.template)
            db.workoutTemplateDao().insertTemplateExercises(aggregate.templateExercises)
        }
        db.templateVersionDao().setVersion(TemplateVersionEntity(version = version, loadedAt = clock.millis()))
    }

    suspend fun log(): List<SyncLogEntity> = syncDao.getLog()

    private companion object {
        const val KEY_CURSOR = "pull_cursor"
    }
}

package com.rukavina.gymbuddy.data.sync

import android.util.Log
import com.rukavina.gymbuddy.data.local.entity.OutboxEntryEntity
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.entity.SyncLogOutcome
import com.rukavina.gymbuddy.data.remote.generated.models.MutationResult
import com.rukavina.gymbuddy.data.remote.generated.models.PushRequest
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.domain.model.SyncState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response

/** A sync step that couldn't complete. Local state is left consistent; nothing is lost. */
class SyncException(message: String, val httpStatus: Int? = null, cause: Throwable? = null) : Exception(message, cause)

data class PushSummary(
    val requests: Int = 0,
    val outcomes: Map<MutationResult.Status, Int> = emptyMap(),
    val unsendable: Int = 0
) {
    fun count(status: MutationResult.Status) = outcomes[status] ?: 0
    operator fun plus(other: PushSummary) = PushSummary(
        requests + other.requests,
        (outcomes.keys + other.outcomes.keys).associateWith { count(it) + other.count(it) },
        unsendable + other.unsendable
    )
}

data class PullSummary(
    val pages: Int = 0,
    val applied: Map<SyncEntityType, Int> = emptyMap(),
    val skippedPending: Int = 0,
    /** True if the cursor expired and local user data was wiped and pulled again from scratch. */
    val cursorExpired: Boolean = false,
    /** True if a push CONFLICT wasn't resolved by the delta pull and a full pull was needed. */
    val fullResync: Boolean = false
)

data class ReferenceSummary(val exercisesReplaced: Boolean, val templatesReplaced: Boolean)

data class SyncSummary(val push: PushSummary, val pull: PullSummary, val reference: ReferenceSummary?)

/** Test seam: runs inside each pull page's transaction, after the page is applied and before the cursor is saved. */
fun interface PullPageObserver {
    suspend fun beforeCursorCommit(pageIndex: Int, nextCursor: String)
}

/**
 * The Android sync engine.
 *
 * - The server resolves every conflict and assigns every timestamp and
 *   revision. The client never merges. It adopts what the server says.
 * - One cycle is push, then pull, then reference data, serialised by
 *   [mutex]. A push and a pull never overlap, including across
 *   triggers.
 * - Pushes are idempotent per entity id. A retried push of an
 *   already-applied change comes back APPLIED, not duplicated.
 * - Aggregates (session tree, template tree) travel and are replaced
 *   whole.
 */
class SyncEngine(
    private val store: SyncLocalStore,
    private val apis: SyncApis,
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
    private val pushBatchSize: Int = DEFAULT_PUSH_BATCH,
    private val pageObserver: PullPageObserver = PullPageObserver { _, _ -> }
) {
    private val mutex = Mutex()

    /** A full cycle: push, pull, reference data. */
    suspend fun sync(): SyncSummary = mutex.withLock {
        val conflicted = mutableSetOf<Pair<SyncEntityType, String>>()
        val push = pushLocked(conflicted)
        var pull = pullLocked(fromScratch = false)
        if (conflicted.any { (type, id) -> store.syncStateOf(type, id) == SyncState.CONFLICTED }) {
            // A CONFLICT means the server holds a newer revision, which a
            // delta pull normally delivers. If it didn't (the change sits
            // behind this device's cursor), pull everything again. That
            // overwrites local state without wiping anything.
            Log.w(TAG, "push conflicts not resolved by delta pull; running full pull")
            pull = pullLocked(fromScratch = true).copy(fullResync = true)
        }
        val reference = runCatching { referenceLocked() }
            .onFailure { Log.w(TAG, "reference data check failed", it) }
            .getOrNull()
        SyncSummary(push, pull, reference)
    }

    /** Push only, until the outbox is empty or no further progress is possible. Returns true if it's empty. */
    suspend fun flushOutbox(): Boolean = mutex.withLock {
        pushLocked(mutableSetOf())
        store.outboxCount() == 0
    }

    suspend fun push(): PushSummary = mutex.withLock { pushLocked(mutableSetOf()) }

    suspend fun pull(): PullSummary = mutex.withLock { pullLocked(fromScratch = false) }

    suspend fun syncReferenceData(): ReferenceSummary = mutex.withLock { referenceLocked() }

    // ---- push ----

    private suspend fun pushLocked(conflicted: MutableSet<Pair<SyncEntityType, String>>): PushSummary {
        var summary = PushSummary()
        repeat(MAX_PUSH_ROUNDS) {
            val batch = selectBatch(store.outbox())
            if (batch.isEmpty()) return summary

            val prepared = mutableListOf<PreparedMutation>()
            var unsendable = 0
            for (entry in batch) {
                when (val outcome = store.prepare(entry)) {
                    is PrepareOutcome.Ready -> prepared += outcome.mutation
                    PrepareOutcome.Missing -> store.dropEntry(entry)
                    is PrepareOutcome.Unsendable -> {
                        Log.w(TAG, "dropping unsendable ${entry.entityType} ${entry.entityId}: ${outcome.reason}")
                        store.transaction { store.markRejected(entry.entityType, entry.entityId, SyncLogOutcome.UNSENDABLE, outcome.reason) }
                        unsendable++
                    }
                }
            }
            if (prepared.isEmpty()) {
                summary += PushSummary(unsendable = unsendable)
                return@repeat
            }

            val response = call("push") { apis.sync.pushChanges(requestOf(prepared)) }
            val results = response.results.associateBy { it.entityType.name to it.entityId.lowercase() }
            val outcomes = mutableMapOf<MutationResult.Status, Int>()
            store.transaction {
                for (mutation in prepared) {
                    val entry = mutation.entry
                    val result = results[entry.entityType.name to entry.entityId.lowercase()] ?: continue
                    outcomes[result.status] = (outcomes[result.status] ?: 0) + 1
                    when (result.status) {
                        MutationResult.Status.APPLIED ->
                            store.markApplied(entry, result.revision ?: 0, result.updatedAt ?: 0L)
                        MutationResult.Status.CONFLICT -> {
                            store.markRejected(entry.entityType, entry.entityId, SyncLogOutcome.CONFLICT, result.reason)
                            conflicted += entry.entityType to entry.entityId
                        }
                        MutationResult.Status.INVALID ->
                            store.markRejected(entry.entityType, entry.entityId, SyncLogOutcome.INVALID, result.reason)
                        MutationResult.Status.FORBIDDEN ->
                            store.markRejected(entry.entityType, entry.entityId, SyncLogOutcome.FORBIDDEN, result.reason)
                        // Transient server failure: keep the entry and retry on a later push.
                        MutationResult.Status.ERROR -> Unit
                    }
                }
            }
            summary += PushSummary(requests = 1, outcomes = outcomes, unsendable = unsendable)
            // Only ERRORs came back: retrying right now would just repeat them.
            if (outcomes.keys == setOf(MutationResult.Status.ERROR)) return summary
        }
        return summary
    }

    /**
     * Up to [pushBatchSize] entries, oldest first. The API has one
     * profile slot per request, so a second profile entry waits for the
     * next round.
     */
    private fun selectBatch(outbox: List<OutboxEntryEntity>) =
        outbox.filterIndexed { index, entry ->
            entry.entityType != SyncEntityType.USER_PROFILE ||
                outbox.indexOfFirst { it.entityType == SyncEntityType.USER_PROFILE } == index
        }.take(pushBatchSize)

    /** Groups prepared mutations into the request's typed arrays. */
    private fun requestOf(prepared: List<PreparedMutation>) = PushRequest(
        workoutSessions = prepared.filterIsInstance<PreparedMutation.Session>().map { it.dto }.ifEmpty { null },
        exercises = prepared.filterIsInstance<PreparedMutation.ExerciseRow>().map { it.dto }.ifEmpty { null },
        workoutTemplates = prepared.filterIsInstance<PreparedMutation.Template>().map { it.dto }.ifEmpty { null },
        userExerciseStates = prepared.filterIsInstance<PreparedMutation.ExerciseState>().map { it.dto }.ifEmpty { null },
        userTemplateStates = prepared.filterIsInstance<PreparedMutation.TemplateState>().map { it.dto }.ifEmpty { null },
        userProfile = prepared.filterIsInstance<PreparedMutation.Profile>().firstOrNull()?.dto
    )

    // ---- pull ----

    private suspend fun pullLocked(fromScratch: Boolean): PullSummary {
        var cursor = if (fromScratch) null else store.cursor()
        var pages = 0
        val applied = mutableMapOf<SyncEntityType, Int>()
        var skipped = 0
        while (true) {
            val response = try {
                apis.sync.pullChanges(cursor = cursor, limit = pageSize)
            } catch (e: java.io.IOException) {
                throw SyncException("pull failed: ${e.message}", cause = e)
            }
            if (response.code() == HTTP_GONE) return handleCursorExpired()
            val page = bodyOf("pull", response)

            val pageIndex = pages
            // The page and the cursor that follows it commit together. A
            // crash anywhere in here rolls back both, so the next pull
            // re-requests this page rather than skipping it.
            val counts = store.transaction {
                val counts = store.applyPage(page)
                pageObserver.beforeCursorCommit(pageIndex, page.nextCursor)
                store.setCursor(page.nextCursor)
                counts
            }
            pages++
            counts.applied.forEach { (type, n) -> applied[type] = (applied[type] ?: 0) + n }
            skipped += counts.skippedPending
            cursor = page.nextCursor
            if (!page.hasMore) break
        }
        return PullSummary(pages = pages, applied = applied, skippedPending = skipped)
    }

    /**
     * 410 CURSOR_EXPIRED: the server has pruned history this device never
     * saw, so a delta can no longer be trusted. Flush local changes first
     * so nothing unpushed is lost, then wipe local user data and pull
     * everything from scratch.
     */
    private suspend fun handleCursorExpired(): PullSummary {
        Log.w(TAG, "pull cursor expired; flushing outbox, wiping local user data, resyncing")
        pushLocked(mutableSetOf())
        val remaining = store.outboxCount()
        if (remaining > 0) {
            throw SyncException("Cursor expired but $remaining local changes could not be pushed; not wiping.")
        }
        store.wipeUserData()
        return pullLocked(fromScratch = true).copy(cursorExpired = true)
    }

    // ---- reference data ----

    private suspend fun referenceLocked(): ReferenceSummary {
        val server = call("reference version") { apis.reference.getReferenceVersion() }
        var exercisesReplaced = false
        var templatesReplaced = false
        if (server.exerciseLibraryVersion != store.exerciseLibraryVersion()) {
            val library = call("reference exercises") { apis.reference.getReferenceExercises() }
            store.replaceDefaultExercises(library.version, library.exercises)
            exercisesReplaced = true
        }
        if (server.templateLibraryVersion != store.templateLibraryVersion()) {
            val library = call("reference templates") { apis.reference.getReferenceTemplates() }
            store.replaceDefaultTemplates(library.version, library.templates)
            templatesReplaced = true
        }
        return ReferenceSummary(exercisesReplaced, templatesReplaced)
    }

    // ---- http ----

    private suspend fun <T> call(what: String, request: suspend () -> Response<T>): T {
        val response = try {
            request()
        } catch (e: java.io.IOException) {
            throw SyncException("$what failed: ${e.message}", cause = e)
        }
        return bodyOf(what, response)
    }

    private fun <T> bodyOf(what: String, response: Response<T>): T {
        if (!response.isSuccessful) throw SyncException("$what failed: HTTP ${response.code()}", response.code())
        return response.body() ?: throw SyncException("$what failed: empty body", response.code())
    }

    companion object {
        private const val TAG = "SyncEngine"
        const val DEFAULT_PAGE_SIZE = 200
        const val DEFAULT_PUSH_BATCH = 100
        private const val MAX_PUSH_ROUNDS = 20
        private const val HTTP_GONE = 410
    }
}

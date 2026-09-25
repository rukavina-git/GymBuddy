package com.rukavina.gymbuddy.data.sync

import androidx.room.withTransaction
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one way a local write to a synced entity reaches the outbox.
 * Wraps the write and the outbox enqueue in one Room transaction, so a
 * change can't be persisted without being queued (or queued without
 * being persisted).
 *
 * Enqueuing never triggers a sync. Syncs run on the triggers in
 * SyncScheduler, not per write.
 */
@Singleton
class OutboxRecorder @Inject constructor(
    private val database: AppDatabase,
    private val clock: Clock
) {
    suspend fun <R> record(
        type: SyncEntityType,
        id: String,
        operation: OutboxOperation = OutboxOperation.UPSERT,
        write: suspend () -> R
    ): R = recordAll(type, listOf(id), operation, write)

    /** Same as [record] for a write that touches several rows of one type (e.g. "unhide all"). */
    suspend fun <R> recordAll(
        type: SyncEntityType,
        ids: List<String>,
        operation: OutboxOperation = OutboxOperation.UPSERT,
        write: suspend () -> R
    ): R = database.withTransaction {
        val result = write()
        val now = clock.millis()
        ids.forEach { database.syncDao().enqueue(type, it, operation, now) }
        result
    }
}

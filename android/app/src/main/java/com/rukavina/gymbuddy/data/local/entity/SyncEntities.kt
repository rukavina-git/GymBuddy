package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The six synced entity types, matching the API's EntityType enum by name. */
enum class SyncEntityType {
    WORKOUT_SESSION,
    EXERCISE,
    WORKOUT_TEMPLATE,
    USER_EXERCISE_STATE,
    USER_TEMPLATE_STATE,
    USER_PROFILE
}

enum class OutboxOperation { UPSERT, DELETE }

/**
 * One pending local change per synced entity. The entity's current row
 * is serialised at push time, so repeated edits to one entity coalesce
 * into a single row. The push is idempotent per entity id either way.
 *
 * changeVersion goes up on every enqueue. A push captures it and only
 * clears the row if it is unchanged when the response arrives. An edit
 * made while the push was in flight keeps its row and goes out on the
 * next push.
 */
@Entity(tableName = "sync_outbox", primaryKeys = ["entityType", "entityId"])
data class OutboxEntryEntity(
    val entityType: SyncEntityType,
    val entityId: String,
    val operation: OutboxOperation,
    val changeVersion: Long,
    val enqueuedAt: Long
)

enum class SyncLogOutcome {
    /** Push returned CONFLICT: the local change is discarded, the next pull delivers the server's version. */
    CONFLICT,
    /** Push returned INVALID: dropped, never retried. */
    INVALID,
    /** Push returned FORBIDDEN: dropped, never retried. */
    FORBIDDEN,
    /** The entity couldn't be serialised for push (e.g. a non-UUID id). Dropped, never retried. */
    UNSENDABLE,
    /** A pull delivered a newer server revision over an unpushed local change. */
    PULL_OVERWRITE
}

/**
 * Record of every local change the sync engine discarded, with the
 * discarded state as JSON. The server is authoritative, so nothing here
 * is ever re-applied; it exists so a lost edit is never lost silently.
 */
@Entity(tableName = "sync_log")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: SyncEntityType,
    val entityId: String,
    val outcome: SyncLogOutcome,
    val reason: String?,
    val discardedPayload: String?,
    val loggedAt: Long
)

/**
 * Key/value sync bookkeeping (the pull cursor). Kept in Room, not
 * DataStore, so a cursor update commits in the same transaction as the
 * page it follows.
 */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey
    val key: String,
    val value: String?
)

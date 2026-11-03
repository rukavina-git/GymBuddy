package com.rukavina.gymbuddy.data.sync

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.domain.model.SyncState
import kotlinx.coroutines.sync.withLock

/** What a user sees counted: live workouts, own templates, own exercises. */
data class LocalDataCounts(val workouts: Int, val templates: Int, val exercises: Int) {
    val isEmpty: Boolean get() = workouts == 0 && templates == 0 && exercises == 0
}

/** Result of [GuestMerge.mergeInto]: what moved, and whether the account's own profile was kept. */
data class GuestMergeReceipt(val added: LocalDataCounts, val keptAccountProfile: Boolean)

/**
 * The guest's data (owner null, ADR 0014) and its one-way merge into an
 * account's file. Everything here runs under the holder's lock, which is
 * the sync engine's, so no cycle and no switch runs alongside.
 *
 * The merge copies rows table by table with INSERT OR IGNORE: a row the
 * account already has wins (server-wins, D-12). Guest ids are client
 * UUIDv7s, so in practice only the profile ever meets an existing row.
 * The guest's outbox comes along, so the next sync pushes it all.
 *
 * [keepAccountAsGuest] is the reverse, for an account deleted elsewhere.
 */
class GuestMerge(private val holder: DatabaseHolder) {

    /**
     * The guest's counts if the guest has changed anything (non-empty
     * outbox: every user write is queued and a guest never pushes), else
     * null. Only asked while an account is open.
     */
    suspend fun guestData(): LocalDataCounts? = holder.lock.withLock {
        if (holder.owner == null || !holder.fileExists(null)) return@withLock null
        holder.withOtherFile(null) { guest ->
            if (guest.syncDao().outboxCount() == 0) null else countsOf(guest)
        }
    }

    /** Counts in the open file: the account's before a merge, the guest's before an erase. */
    suspend fun openCounts(): LocalDataCounts = countsOf(holder.db)

    /**
     * Copies the guest's data into [uid]'s open file in one transaction,
     * then deletes the guest file. Does nothing if [uid]'s file isn't the
     * open one (the user switched again meanwhile).
     */
    suspend fun mergeInto(uid: String): GuestMergeReceipt? = holder.lock.withLock {
        if (holder.owner != uid || !holder.fileExists(null)) return@withLock null
        val account = holder.db
        val receipt = holder.withOtherFile(null) { guest ->
            val added = countsOf(guest)
            val from = guest.openHelper.writableDatabase
            var keptAccountProfile = false
            account.withTransaction {
                val to = account.openHelper.writableDatabase
                COPIED_TABLES.forEach { (table, where) -> copy(from, to, table, where) }
                keptAccountProfile = !copyProfile(from, to, uid)
            }
            GuestMergeReceipt(added, keptAccountProfile)
        }
        holder.deleteOtherFile(null)
        receipt
    }

    /**
     * "Keep on this phone" after the account was deleted elsewhere (step 03,
     * decision 1A). Copies the account's live rows into the guest's file as
     * new, never-synced rows: revision 0, PENDING, no owner id, one outbox
     * entry each, so a later account gets all of them. Rows the guest
     * already has win. The account's file is left for [deleteOwnerFile].
     */
    suspend fun keepAccountAsGuest(owner: String) = holder.lock.withLock {
        check(holder.owner == owner) { "the account's file must be open" }
        val account = holder.db
        val from = account.openHelper.writableDatabase
        holder.withOtherFile(null) { guest ->
            guest.withTransaction {
                val to = guest.openHelper.writableDatabase
                val now = System.currentTimeMillis()
                KEPT_TABLES.forEach { kept ->
                    copy(from, to, kept.table, kept.where) { row ->
                        if (kept.reset) {
                            row.put("revision", 0)
                            row.put("syncState", SyncState.PENDING.name)
                        }
                        if (kept.clearOwnerId) row.putNull("ownerId")
                        kept.outboxType?.let { type ->
                            to.insert("sync_outbox", SQLiteDatabase.CONFLICT_IGNORE, outboxRow(type, row.getAsString(kept.idColumn), now))
                        }
                    }
                }
                keepProfile(from, to, owner, now)
            }
        }
    }

    /** Deletes a former owner's file once it's no longer open (after a switch away from it). */
    suspend fun deleteOwnerFile(owner: String) = holder.lock.withLock {
        holder.deleteOtherFile(owner)
    }

    /** "Erase data on this phone" for a guest: the logout wipe, on the guest's own file. */
    suspend fun eraseGuest() = holder.lock.withLock {
        check(holder.owner == null) { "only a guest's data is erased here" }
        val db = holder.db
        db.withTransaction {
            db.syncDao().wipeLocalUserData()
            db.syncDao().wipeLog()
        }
    }

    private suspend fun countsOf(db: AppDatabase): LocalDataCounts {
        val sql = db.openHelper.readableDatabase
        fun count(query: String) = sql.query(query).use { if (it.moveToFirst()) it.getInt(0) else 0 }
        return LocalDataCounts(
            workouts = count("SELECT COUNT(*) FROM workout_sessions WHERE deletedAt IS NULL"),
            templates = count("SELECT COUNT(*) FROM workout_templates WHERE source = 'CUSTOM' AND deletedAt IS NULL"),
            exercises = count("SELECT COUNT(*) FROM exercises WHERE source = 'CUSTOM' AND deletedAt IS NULL")
        )
    }

    /**
     * The guest profile row becomes [uid]'s, with its outbox entry, unless
     * the account already has a profile. Returns false only when the
     * account's profile was kept over the guest's.
     */
    private fun copyProfile(from: SupportSQLiteDatabase, to: SupportSQLiteDatabase, uid: String): Boolean {
        val profile = from.query("SELECT * FROM user_profile WHERE uid = ?", arrayOf(GUEST)).use { cursor ->
            if (cursor.moveToFirst()) rowOf(cursor) else null
        } ?: return true // the guest never saved a profile
        profile.put("uid", uid)
        if (to.insert("user_profile", SQLiteDatabase.CONFLICT_IGNORE, profile) == -1L) return false
        val entry = from.query(
            "SELECT * FROM sync_outbox WHERE entityType = ? AND entityId = ?",
            arrayOf(SyncEntityType.USER_PROFILE.name, GUEST)
        ).use { cursor -> if (cursor.moveToFirst()) rowOf(cursor) else null }
        if (entry != null) {
            entry.put("entityId", uid)
            to.insert("sync_outbox", SQLiteDatabase.CONFLICT_IGNORE, entry)
        }
        return true
    }

    /** The account's profile becomes the guest's, unless the guest already has one. */
    private fun keepProfile(from: SupportSQLiteDatabase, to: SupportSQLiteDatabase, owner: String, now: Long) {
        val profile = from.query("SELECT * FROM user_profile WHERE uid = ?", arrayOf(owner)).use { cursor ->
            if (cursor.moveToFirst()) rowOf(cursor) else null
        } ?: return
        profile.put("uid", GUEST)
        profile.put("revision", 0)
        profile.put("syncState", SyncState.PENDING.name)
        if (to.insert("user_profile", SQLiteDatabase.CONFLICT_IGNORE, profile) != -1L) {
            to.insert("sync_outbox", SQLiteDatabase.CONFLICT_IGNORE, outboxRow(SyncEntityType.USER_PROFILE, GUEST, now))
        }
    }

    private fun outboxRow(type: SyncEntityType, id: String, now: Long) = ContentValues().apply {
        put("entityType", type.name)
        put("entityId", id)
        put("operation", OutboxOperation.UPSERT.name)
        put("changeVersion", 1L)
        put("enqueuedAt", now)
    }

    private fun copy(
        from: SupportSQLiteDatabase,
        to: SupportSQLiteDatabase,
        table: String,
        where: String?,
        transform: (ContentValues) -> Unit = {}
    ) {
        val query = "SELECT * FROM $table" + (where?.let { " WHERE $it" } ?: "")
        from.query(query).use { cursor ->
            while (cursor.moveToNext()) {
                val row = rowOf(cursor).also(transform)
                to.insert(table, SQLiteDatabase.CONFLICT_IGNORE, row)
            }
        }
    }

    /** One row as ContentValues, keeping each column's storage type (no REAL → TEXT round trip). */
    private fun rowOf(cursor: Cursor): ContentValues = ContentValues().apply {
        for (i in 0 until cursor.columnCount) {
            val name = cursor.getColumnName(i)
            when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> putNull(name)
                Cursor.FIELD_TYPE_INTEGER -> put(name, cursor.getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> put(name, cursor.getDouble(i))
                Cursor.FIELD_TYPE_BLOB -> put(name, cursor.getBlob(i))
                else -> put(name, cursor.getString(i))
            }
        }
    }

    /** One table kept as the guest's: which rows, whether to reset sync fields, which outbox entry. */
    private class Kept(
        val table: String,
        val where: String?,
        val reset: Boolean = false,
        val clearOwnerId: Boolean = false,
        val outboxType: SyncEntityType? = null,
        val idColumn: String = "id"
    )

    private companion object {
        const val GUEST = UserProfileRepository.GUEST_UID

        const val LIVE_SESSIONS = "SELECT id FROM workout_sessions WHERE deletedAt IS NULL"
        const val LIVE_CUSTOM_TEMPLATES =
            "SELECT id FROM workout_templates WHERE source = 'CUSTOM' AND deletedAt IS NULL"

        /**
         * Live user rows in foreign-key order. Tombstones stay behind: they
         * were never the guest's, so there is nothing to delete later.
         */
        val KEPT_TABLES = listOf(
            Kept("exercises", "source = 'CUSTOM' AND deletedAt IS NULL", reset = true, clearOwnerId = true, outboxType = SyncEntityType.EXERCISE),
            Kept("workout_templates", "source = 'CUSTOM' AND deletedAt IS NULL", reset = true, clearOwnerId = true, outboxType = SyncEntityType.WORKOUT_TEMPLATE),
            Kept("template_exercises", "templateId IN ($LIVE_CUSTOM_TEMPLATES)"),
            Kept("workout_sessions", "deletedAt IS NULL", reset = true, outboxType = SyncEntityType.WORKOUT_SESSION),
            Kept("performed_exercises", "workoutSessionId IN ($LIVE_SESSIONS)"),
            Kept(
                "workout_sets",
                "performedExerciseId IN (SELECT id FROM performed_exercises WHERE workoutSessionId IN ($LIVE_SESSIONS))"
            ),
            Kept("user_exercise_state", null, reset = true, outboxType = SyncEntityType.USER_EXERCISE_STATE, idColumn = "exerciseId"),
            Kept("user_template_state", null, reset = true, outboxType = SyncEntityType.USER_TEMPLATE_STATE, idColumn = "templateId"),
            Kept("active_workout_draft", null)
        )

        /**
         * User tables in foreign-key order, with the guest rows to take.
         * Not copied: the DEFAULT library (each file seeds its own), the
         * sync log and meta (the cursor is the account's), and the profile
         * (re-keyed in [copyProfile]).
         */
        val COPIED_TABLES = listOf(
            "exercises" to "source = 'CUSTOM'",
            "workout_templates" to "source = 'CUSTOM'",
            "template_exercises" to "templateId IN (SELECT id FROM workout_templates WHERE source = 'CUSTOM')",
            "workout_sessions" to null,
            "performed_exercises" to null,
            "workout_sets" to null,
            "user_exercise_state" to null,
            "user_template_state" to null,
            "active_workout_draft" to null,
            "sync_outbox" to "NOT (entityType = '${SyncEntityType.USER_PROFILE.name}' AND entityId = '$GUEST')"
        )
    }
}

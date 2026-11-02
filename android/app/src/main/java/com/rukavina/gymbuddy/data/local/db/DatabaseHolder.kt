package com.rukavina.gymbuddy.data.local.db

import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * The open Room database, one file per data owner (ADR 0014). The owner
 * is the signed-in uid, or null while signed out (step 02 turns the
 * signed-out file into the guest's).
 *
 * Nothing holds an [AppDatabase] or a DAO across calls: read [db] per
 * call, or [observe] for flows, which resubscribe on a switch.
 *
 * [lock] is the sync engine's lock, so a switch never happens during a
 * sync cycle, a logout wipe or an account deletion. Never call
 * [switchTo] while holding it: the lock isn't reentrant.
 */
class DatabaseHolder(
    initialOwner: String?,
    private val open: (owner: String?) -> AppDatabase,
    val lock: Mutex = Mutex()
) {
    @Volatile
    var owner: String? = initialOwner
        private set

    private val _current = MutableStateFlow(open(initialOwner))
    val current: StateFlow<AppDatabase> = _current.asStateFlow()

    val db: AppDatabase get() = _current.value

    /** Room flow over whichever database is current; switches with it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> observe(query: (AppDatabase) -> Flow<T>): Flow<T> = _current.flatMapLatest(query)

    /** Opens [newOwner]'s file and closes the previous one. Returns false if it was already open. */
    suspend fun switchTo(newOwner: String?): Boolean = lock.withLock {
        if (newOwner == owner) return@withLock false
        val previous = _current.value
        val next = open(newOwner)
        owner = newOwner
        _current.value = next
        previous.close()
        Log.i(TAG, "switched database owner")
        true
    }

    companion object {
        private const val TAG = "DatabaseHolder"
        private const val SIGNED_OUT_FILE = "gym_buddy_signed_out.db"

        /** SQLite's side files, which belong to the main file and move with it. */
        private val SIDE_FILE_SUFFIXES = listOf("", "-wal", "-shm", "-journal")

        fun fileName(owner: String?): String {
            if (owner == null) return SIGNED_OUT_FILE
            require(owner.isNotEmpty() && owner.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
                "not a file-safe owner id"
            }
            return "gym_buddy_$owner.db"
        }

        /** A holder over one fixed database, for tests. */
        fun fixed(db: AppDatabase, owner: String? = "test-uid"): DatabaseHolder = DatabaseHolder(owner, { db })

        /**
         * One-time move from the single pre-Plan-6 file. Signed in: it is
         * renamed to that uid's file. Signed out: it is deleted (logout
         * wiped it). [pathOf] maps a database name to its file.
         */
        fun migrateLegacyFile(pathOf: (String) -> File, signedInUid: String?) {
            val legacy = pathOf(LEGACY_DATABASE_NAME)
            if (!legacy.exists()) return
            if (signedInUid == null) {
                SIDE_FILE_SUFFIXES.forEach { File(legacy.path + it).delete() }
                Log.i(TAG, "deleted legacy database (signed out)")
                return
            }
            val target = pathOf(fileName(signedInUid))
            if (target.exists()) {
                // Can't happen through the app; never overwrite an owner's file.
                Log.w(TAG, "legacy database left in place: owner file already exists")
                return
            }
            SIDE_FILE_SUFFIXES.forEach { suffix ->
                val from = File(legacy.path + suffix)
                if (from.exists() && !from.renameTo(File(target.path + suffix))) {
                    error("could not move legacy database file$suffix")
                }
            }
            Log.i(TAG, "moved legacy database to the signed-in owner's file")
        }
    }
}

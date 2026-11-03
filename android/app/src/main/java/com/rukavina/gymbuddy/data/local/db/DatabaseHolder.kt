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
 * is the signed-in uid, or null for the guest (no account).
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
    val lock: Mutex = Mutex(),
    private val files: OwnerFiles = OwnerFiles.NONE
) {
    /** Whether an owner's file exists, and deleting it with its side files. */
    interface OwnerFiles {
        fun exists(owner: String?): Boolean
        fun delete(owner: String?)

        companion object {
            /** For holders over in-memory or fixed databases: no files. */
            val NONE = object : OwnerFiles {
                override fun exists(owner: String?) = false
                override fun delete(owner: String?) = Unit
            }

            fun inDirectory(pathOf: (String) -> File) = object : OwnerFiles {
                override fun exists(owner: String?) = pathOf(fileName(owner)).exists()
                override fun delete(owner: String?) {
                    val main = pathOf(fileName(owner))
                    // Side files first: a main file without its WAL is still valid.
                    SIDE_FILES.forEach { deleteIfPresent(File(main.path + it)) }
                    deleteIfPresent(main)
                }

                private fun deleteIfPresent(file: File) {
                    if (!file.delete() && file.exists()) Log.w(TAG, "Couldn't delete ${file.name}")
                }
            }
        }
    }

    @Volatile
    var owner: String? = initialOwner
        private set

    private val _owner = MutableStateFlow(initialOwner)

    /** [owner] as a flow, for gating screens on a finished switch. */
    val ownerFlow: StateFlow<String?> = _owner.asStateFlow()

    /**
     * The file switched away from last. Kept open until the next switch,
     * so a read or write already in flight on it can finish.
     */
    private var retired: Pair<String?, AppDatabase>? = null

    private val _current = MutableStateFlow(open(initialOwner))
    val current: StateFlow<AppDatabase> = _current.asStateFlow()

    val db: AppDatabase get() = _current.value

    /** Room flow over whichever database is current; switches with it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun <T> observe(query: (AppDatabase) -> Flow<T>): Flow<T> = _current.flatMapLatest(query)

    /**
     * Opens [newOwner]'s file. The previous one stays open until the next
     * switch (see [retired]). Returns false if it was already open.
     */
    suspend fun switchTo(newOwner: String?): Boolean = lock.withLock {
        if (newOwner == owner) return@withLock false
        closeRetired()
        val previous = _current.value
        val next = open(newOwner)
        retired = owner to previous
        owner = newOwner
        _current.value = next
        _owner.value = newOwner
        Log.i(TAG, "switched database owner")
        true
    }

    /** Whether [other]'s file exists. Call under [lock]. */
    fun fileExists(other: String?): Boolean = files.exists(other)

    /**
     * Opens another owner's file for [block] and closes it after. Call
     * under [lock]; [other] must not be the current owner.
     */
    suspend fun <T> withOtherFile(other: String?, block: suspend (AppDatabase) -> T): T {
        check(other != owner) { "use db for the current owner" }
        if (retired?.first == other) closeRetired()
        val database = open(other)
        return try {
            block(database)
        } finally {
            database.close()
        }
    }

    /** Deletes another owner's file. Call under [lock]. */
    fun deleteOtherFile(other: String?) {
        check(other != owner) { "can't delete the open file" }
        if (retired?.first == other) closeRetired()
        files.delete(other)
        Log.i(TAG, "deleted an owner's database file")
    }

    private fun closeRetired() {
        retired?.second?.close()
        retired = null
    }

    companion object {
        private const val TAG = "DatabaseHolder"
        private const val GUEST_FILE = "gym_buddy_guest.db"

        /** Step 01's name for the signed-out file, which became the guest's. */
        internal const val SIGNED_OUT_FILE = "gym_buddy_signed_out.db"

        /**
         * SQLite's side files, which belong to the main file and move with
         * it. Side files go first: a main file whose WAL went missing is
         * still a valid database, a WAL without its main file is not.
         */
        private val SIDE_FILES = listOf("-wal", "-shm", "-journal")
        private val SIDE_FILE_SUFFIXES = SIDE_FILES + ""

        fun fileName(owner: String?): String {
            if (owner == null) return GUEST_FILE
            require(owner.isNotEmpty() && owner.all { it.isLetterOrDigit() || it == '-' || it == '_' }) {
                "not a file-safe owner id"
            }
            return "gym_buddy_$owner.db"
        }

        /** A holder over one fixed database, for tests. */
        fun fixed(db: AppDatabase, owner: String? = "test-uid"): DatabaseHolder = DatabaseHolder(owner, { db })

        /** One-time rename of step 01's signed-out file to the guest file. */
        fun migrateSignedOutFile(pathOf: (String) -> File) {
            val from = pathOf(SIGNED_OUT_FILE)
            if (!from.exists()) return
            val target = pathOf(GUEST_FILE)
            if (target.exists()) {
                Log.w(TAG, "signed-out database left in place: guest file already exists")
                return
            }
            moveWithSideFiles(from, target)
            Log.i(TAG, "moved signed-out database to the guest file")
        }

        private fun moveWithSideFiles(from: File, to: File) {
            SIDE_FILE_SUFFIXES.forEach { suffix ->
                val source = File(from.path + suffix)
                if (source.exists() && !source.renameTo(File(to.path + suffix))) {
                    error("could not move database file$suffix")
                }
            }
        }

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
            moveWithSideFiles(legacy, target)
            Log.i(TAG, "moved legacy database to the signed-in owner's file")
        }
    }
}

package com.rukavina.gymbuddy.data.local.db

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.sync.FakeAuthSession
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.data.sync.SyncEngine
import com.rukavina.gymbuddy.data.sync.SyncFixtures
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Plan 6 step 01: one Room file per owner (ADR 0014). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseHolderTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val opened = mutableListOf<AppDatabase>()
    private lateinit var holder: DatabaseHolder
    private lateinit var sessions: WorkoutSessionRepositoryImpl
    private lateinit var store: SyncLocalStore

    private fun open(owner: String?): AppDatabase =
        Room.databaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
            File(tmp.root, DatabaseHolder.fileName(owner)).absolutePath
        ).applyAppConfig().allowMainThreadQueries().build().also { opened += it }

    @Before
    fun setUp() {
        holder = DatabaseHolder("uid-a", ::open)
        sessions = WorkoutSessionRepositoryImpl(holder, OutboxRecorder(holder, clock), clock)
        store = SyncLocalStore(holder, clock)
    }

    @After
    fun tearDown() = opened.forEach { if (it.isOpen) it.close() }

    @Test
    fun `each owner has its own file and data comes back when switching back`() = runBlocking {
        val workout = SyncFixtures.session()
        sessions.createWorkoutSession(workout)

        assertTrue(holder.switchTo("uid-b"))
        assertEquals("uid-b", holder.owner)
        assertNull("B never sees A's workout", sessions.getWorkoutSessionById(workout.id))
        assertTrue(sessions.getAllWorkoutSessions().first().isEmpty())
        assertTrue(File(tmp.root, "gym_buddy_uid-a.db").exists())
        assertTrue(File(tmp.root, "gym_buddy_uid-b.db").exists())

        holder.switchTo("uid-a")
        assertNotNull(sessions.getWorkoutSessionById(workout.id))
        assertEquals(listOf(workout.id), sessions.getAllWorkoutSessions().first().map { it.id })
    }

    @Test
    fun `switching to the open owner does nothing`() = runBlocking {
        val before = holder.db
        before.syncDao().outboxCount() // Room opens the file lazily
        assertFalse(holder.switchTo("uid-a"))
        assertTrue(before === holder.db)
        assertTrue(before.isOpen)
    }

    @Test
    fun `the previous file is closed after the switch`() = runBlocking {
        val a = holder.db
        a.syncDao().outboxCount() // Room opens the file lazily
        assertTrue(a.isOpen)
        holder.switchTo(null)
        assertFalse(a.isOpen)
        holder.db.syncDao().outboxCount()
        assertTrue(File(tmp.root, "gym_buddy_signed_out.db").exists())
    }

    @Test
    fun `observed flows follow the switch`() = runBlocking {
        val workout = SyncFixtures.session()
        sessions.createWorkoutSession(workout)
        val counts = holder.observe { it.syncDao().observeOutboxCount() }

        assertEquals(1, counts.first())
        holder.switchTo("uid-b")
        assertEquals(0, counts.first())
    }

    @Test
    fun `A's outbox stays after B signs in and B's sync never sends it`() = runBlocking {
        val workout = SyncFixtures.session()
        sessions.createWorkoutSession(workout)
        val requests = mutableListOf<String>()
        val auth = FakeAuthSession("uid-b")
        val engine = SyncEngine(
            store,
            recordingApis(requests),
            isSignedIn = { auth.currentUid.let { it != null && it == holder.owner } },
            mutex = holder.lock
        )

        // B is signed in but A's file is still open: nothing may go out.
        engine.sync()
        assertFalse(engine.flushOutbox())
        assertTrue("no request under B's token from A's file", requests.isEmpty())

        holder.switchTo("uid-b")
        engine.sync()
        assertFalse("B has nothing to push", requests.any { it.endsWith("/sync/push") })
        assertTrue("B pulls its own data", requests.any { it.endsWith("/sync/pull") })

        holder.switchTo("uid-a")
        assertEquals(1, store.outboxCount())
        assertEquals(mapOf(SyncEntityType.WORKOUT_SESSION to 1), store.pendingCountsByType())
    }

    @Test
    fun `a switch waits for a running sync cycle`() = runBlocking {
        val engine = SyncEngine(store, recordingApis(mutableListOf()), mutex = holder.lock)
        val inCycle = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val cycle = async(Dispatchers.Default) { engine.exclusive { inCycle.complete(Unit); release.await() } }
        inCycle.await()

        val switch = async(Dispatchers.Default) { holder.switchTo("uid-b") }
        delay(200)
        assertFalse("switch waits for the cycle", switch.isCompleted)
        assertEquals("uid-a", holder.owner)

        release.complete(Unit)
        cycle.await()
        assertTrue(switch.await())
        assertEquals("uid-b", holder.owner)
    }

    @Test
    fun `legacy file is renamed to the signed-in owner's file`() {
        val dir = tmp.newFolder("databases")
        File(dir, LEGACY_DATABASE_NAME).writeText("main")
        File(dir, "$LEGACY_DATABASE_NAME-wal").writeText("wal")

        DatabaseHolder.migrateLegacyFile({ File(dir, it) }, "uid-a")

        assertFalse(File(dir, LEGACY_DATABASE_NAME).exists())
        assertFalse(File(dir, "$LEGACY_DATABASE_NAME-wal").exists())
        assertEquals("main", File(dir, "gym_buddy_uid-a.db").readText())
        assertEquals("wal", File(dir, "gym_buddy_uid-a.db-wal").readText())
    }

    @Test
    fun `legacy file is deleted when nobody is signed in`() {
        val dir = tmp.newFolder("databases")
        File(dir, LEGACY_DATABASE_NAME).writeText("main")
        File(dir, "$LEGACY_DATABASE_NAME-shm").writeText("shm")

        DatabaseHolder.migrateLegacyFile({ File(dir, it) }, null)

        assertTrue(dir.listFiles()!!.isEmpty())
    }

    @Test
    fun `legacy file never overwrites an existing owner file`() {
        val dir = tmp.newFolder("databases")
        File(dir, LEGACY_DATABASE_NAME).writeText("legacy")
        File(dir, "gym_buddy_uid-a.db").writeText("owner")

        DatabaseHolder.migrateLegacyFile({ File(dir, it) }, "uid-a")

        assertEquals("owner", File(dir, "gym_buddy_uid-a.db").readText())
        assertTrue(File(dir, LEGACY_DATABASE_NAME).exists())
    }

    @Test
    fun `migrated legacy database opens with its data`() = runBlocking {
        val dir = tmp.newFolder("legacy")
        val legacyPath = File(dir, LEGACY_DATABASE_NAME).absolutePath
        val legacy = Room.databaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java, legacyPath)
            .applyAppConfig().allowMainThreadQueries().build()
        val workout = SyncFixtures.session()
        WorkoutSessionRepositoryImpl(DatabaseHolder.fixed(legacy), OutboxRecorder(DatabaseHolder.fixed(legacy), clock), clock)
            .createWorkoutSession(workout)
        legacy.close()

        DatabaseHolder.migrateLegacyFile({ File(dir, it) }, "uid-a")
        val migrated = Room.databaseBuilder(
            RuntimeEnvironment.getApplication(), AppDatabase::class.java, File(dir, "gym_buddy_uid-a.db").absolutePath
        ).applyAppConfig().allowMainThreadQueries().build().also { opened += it }

        assertNotNull(migrated.syncDao().getSessionIncludingDeleted(workout.id))
        assertEquals(1, migrated.syncDao().outboxCount())
    }

    private fun recordingApis(requests: MutableList<String>): SyncApis {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val path = chain.request().url.encodedPath
            synchronized(requests) { requests += path }
            val pull = path.endsWith("/sync/pull")
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(if (pull) 200 else 500)
                .message("canned")
                .body((if (pull) """{"nextCursor":"c1","hasMore":false}""" else "{}").toResponseBody("application/json".toMediaType()))
                .build()
        }.build()
        return SyncApis.create("http://canned.invalid/", { "token" }, client)
    }
}

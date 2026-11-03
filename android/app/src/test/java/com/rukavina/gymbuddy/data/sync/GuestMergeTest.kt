package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.db.applyAppConfig
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository.Companion.GUEST_UID
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutTemplateRepositoryImpl
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.domain.sync.SyncOutcome
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

/** Plan 6 step 02: guest mode and the guest → account merge (ADR 0014). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuestMergeTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val opened = mutableListOf<AppDatabase>()
    private lateinit var holder: DatabaseHolder
    private lateinit var sessions: WorkoutSessionRepositoryImpl
    private lateinit var exercises: ExerciseRepositoryImpl
    private lateinit var templates: WorkoutTemplateRepositoryImpl
    private lateinit var profiles: UserProfileRepository
    private lateinit var store: SyncLocalStore
    private lateinit var merge: GuestMerge

    private fun file(owner: String?) = File(tmp.root, DatabaseHolder.fileName(owner))

    private fun open(owner: String?): AppDatabase =
        Room.databaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java, file(owner).absolutePath)
            .applyAppConfig().allowMainThreadQueries().build().also { opened += it }

    @Before
    fun setUp() {
        holder = DatabaseHolder(null, ::open, files = DatabaseHolder.OwnerFiles.inDirectory { File(tmp.root, it) })
        val outbox = OutboxRecorder(holder, clock)
        sessions = WorkoutSessionRepositoryImpl(holder, outbox, clock)
        exercises = ExerciseRepositoryImpl(holder, outbox, clock)
        templates = WorkoutTemplateRepositoryImpl(holder, outbox, clock)
        profiles = UserProfileRepository(holder, outbox, clock)
        store = SyncLocalStore(holder, clock)
        merge = GuestMerge(holder)
    }

    @After
    fun tearDown() = opened.forEach { if (it.isOpen) it.close() }

    /** A guest with one of each: workout, custom exercise (hidden), custom template, profile. */
    private suspend fun guestWritesOneOfEach(): GuestRows {
        val workout = SyncFixtures.session()
        sessions.createWorkoutSession(workout)
        val exercise = SyncFixtures.exercise()
        exercises.createExercise(exercise)
        exercises.hideExercise(exercise.id)
        val template = SyncFixtures.template()
        templates.createTemplate(template)
        profiles.saveProfile(SyncFixtures.profile(GUEST_UID, name = "Guest Name"))
        return GuestRows(workout.id, exercise.id, template.id)
    }

    private data class GuestRows(val workoutId: String, val exerciseId: String, val templateId: String)

    private fun coordinator(
        syncOutcome: SyncOutcome = SyncOutcome.SYNCED,
        events: MutableList<String> = mutableListOf()
    ) = GuestMergeCoordinator(
        merge,
        syncNow = { events += "syncNow"; syncOutcome },
        requestSync = { events += "requestSync" },
        currentUid = { holder.owner },
        accountEmail = { "a@example.com" },
        signOut = { events += "signOut" },
        setGuest = { events += "setGuest($it)" }
    )

    @Test
    fun `guest writes stay in the guest file and nothing is pushed`() = runBlocking {
        guestWritesOneOfEach()
        val requests = mutableListOf<String>()
        val auth = FakeAuthSession(currentUid = null)
        val engine = SyncEngine(
            store,
            recordingApis(requests),
            isSignedIn = { auth.currentUid.let { it != null && it == holder.owner } },
            mutex = holder.lock
        )

        engine.sync()
        assertFalse(engine.flushOutbox())

        assertTrue("a guest never talks to the server", requests.isEmpty())
        assertEquals(5, store.outboxCount())
        assertTrue(file(null).exists())
    }

    @Test
    fun `merge into an empty account moves everything and deletes the guest file`() = runBlocking {
        val rows = guestWritesOneOfEach()
        holder.switchTo("uid-a")

        assertEquals(LocalDataCounts(1, 1, 1), merge.guestData())
        val receipt = merge.mergeInto("uid-a")

        assertEquals(GuestMergeReceipt(LocalDataCounts(1, 1, 1), keptAccountProfile = false), receipt)
        val workout = sessions.getWorkoutSessionById(rows.workoutId)
        assertNotNull(workout)
        assertEquals(1, workout!!.performedExercises.single().sets.size)
        assertNotNull(exercises.getExerciseById(rows.exerciseId))
        assertEquals(1, templates.getTemplateById(rows.templateId)!!.templateExercises.size)
        assertEquals("Guest Name", profiles.getProfile("uid-a")!!.name)
        assertNull(profiles.getProfile(GUEST_UID))

        val pending = store.outbox().map { it.entityType to it.entityId }.toSet()
        assertEquals(
            setOf(
                SyncEntityType.WORKOUT_SESSION to rows.workoutId,
                SyncEntityType.EXERCISE to rows.exerciseId,
                SyncEntityType.USER_EXERCISE_STATE to rows.exerciseId,
                SyncEntityType.WORKOUT_TEMPLATE to rows.templateId,
                SyncEntityType.USER_PROFILE to "uid-a"
            ),
            pending
        )
        assertFalse("guest file deleted", file(null).exists())
        assertNull(merge.guestData())
    }

    @Test
    fun `the first sync after a merge pushes the guest's changes`() = runBlocking {
        guestWritesOneOfEach()
        holder.switchTo("uid-a")
        merge.mergeInto("uid-a")
        val requests = mutableListOf<String>()
        val engine = SyncEngine(store, recordingApis(requests), isSignedIn = { holder.owner == "uid-a" }, mutex = holder.lock)

        runCatching { engine.sync() } // canned push answers 500; only the request matters

        assertTrue(requests.any { it.endsWith("/sync/push") })
    }

    @Test
    fun `the account's own profile and rows win`() = runBlocking {
        val clash = SyncFixtures.session(title = "guest version")
        sessions.createWorkoutSession(clash)
        profiles.saveProfile(SyncFixtures.profile(GUEST_UID, name = "Guest Name"))
        holder.switchTo("uid-a")
        profiles.saveProfile(SyncFixtures.profile("uid-a", name = "Account Name"))
        sessions.createWorkoutSession(clash.copy(title = "account version"))

        val receipt = merge.mergeInto("uid-a")

        assertTrue(receipt!!.keptAccountProfile)
        assertEquals("Account Name", profiles.getProfile("uid-a")!!.name)
        assertEquals("account version", sessions.getWorkoutSessionById(clash.id)!!.title)
        assertFalse(store.outbox().any { it.entityId == GUEST_UID })
    }

    @Test
    fun `no guest data when the guest never changed anything`() = runBlocking {
        holder.db.syncDao().outboxCount() // the guest file exists, but is empty
        holder.switchTo("uid-a")
        assertNull(merge.guestData())
    }

    @Test
    fun `erase as a guest empties the guest file`() = runBlocking {
        guestWritesOneOfEach()

        merge.eraseGuest()

        assertTrue(merge.openCounts().isEmpty)
        assertEquals(0, store.outboxCount())
        assertNull(profiles.getProfile(GUEST_UID))
    }

    // ---- coordinator ----

    @Test
    fun `empty account after a good sync merges without asking`() = runBlocking {
        guestWritesOneOfEach()
        holder.switchTo("uid-a")
        val events = mutableListOf<String>()
        val coordinator = coordinator(events = events)

        coordinator.onOwnerChanged("uid-a")

        val done = coordinator.state.value as GuestMergeState.Done
        assertEquals(LocalDataCounts(1, 1, 1), done.receipt.added)
        assertEquals(listOf("syncNow", "setGuest(false)", "requestSync"), events)
        assertFalse(file(null).exists())
    }

    @Test
    fun `account with data asks with counts, and Add merges`() = runBlocking {
        guestWritesOneOfEach()
        holder.switchTo("uid-a")
        sessions.createWorkoutSession(SyncFixtures.session())
        sessions.createWorkoutSession(SyncFixtures.session())
        val coordinator = coordinator()

        coordinator.onOwnerChanged("uid-a")

        assertEquals(
            GuestMergeState.Asking("uid-a", "a@example.com", accountWorkouts = 2, guest = LocalDataCounts(1, 1, 1)),
            coordinator.state.value
        )
        assertTrue("nothing moved before Add", file(null).exists())

        coordinator.add()

        assertTrue(coordinator.state.value is GuestMergeState.Done)
        assertEquals(3, merge.openCounts().workouts)
    }

    @Test
    fun `Cancel signs out and keeps the guest`() = runBlocking {
        guestWritesOneOfEach()
        holder.switchTo("uid-a")
        sessions.createWorkoutSession(SyncFixtures.session())
        val events = mutableListOf<String>()
        val coordinator = coordinator(events = events)
        coordinator.onOwnerChanged("uid-a")

        coordinator.cancel()

        assertEquals(GuestMergeState.Idle, coordinator.state.value)
        assertEquals(listOf("syncNow", "setGuest(true)", "signOut"), events)
        assertEquals(1, merge.openCounts().workouts)
        holder.switchTo(null)
        assertEquals(LocalDataCounts(1, 1, 1), merge.openCounts())
    }

    @Test
    fun `a failed sync asks even when the account looks empty`() = runBlocking {
        guestWritesOneOfEach()
        holder.switchTo("uid-a")
        val coordinator = coordinator(syncOutcome = SyncOutcome.FAILED)

        coordinator.onOwnerChanged("uid-a")

        val asking = coordinator.state.value as GuestMergeState.Asking
        assertEquals(0, asking.accountWorkouts)
        assertTrue(file(null).exists())
    }

    @Test
    fun `no guest data just requests the sign-in sync`() = runBlocking {
        holder.switchTo("uid-a")
        val events = mutableListOf<String>()

        coordinator(events = events).onOwnerChanged("uid-a")

        assertEquals(listOf("requestSync"), events)
    }

    @Test
    fun `A's unsynced changes stay apart when a guest signs in as B`() = runBlocking {
        holder.switchTo("uid-a")
        val accountA = SyncFixtures.session(title = "A's workout")
        sessions.createWorkoutSession(accountA)
        holder.switchTo(null)
        guestWritesOneOfEach()
        holder.switchTo("uid-b")

        coordinator().onOwnerChanged("uid-b")

        assertNull("B never gets A's workout", sessions.getWorkoutSessionById(accountA.id))
        assertEquals(1, merge.openCounts().workouts)
        holder.switchTo("uid-a")
        assertEquals("A's file untouched", 1, merge.openCounts().workouts)
        assertEquals(listOf(SyncEntityType.WORKOUT_SESSION to accountA.id), store.outbox().map { it.entityType to it.entityId })
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

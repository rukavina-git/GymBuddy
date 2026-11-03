package com.rukavina.gymbuddy.data.sync

import androidx.room.Room
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.db.applyAppConfig
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository.Companion.GUEST_UID
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.ui.guest.startsInMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import java.util.concurrent.ExecutionException

/** Plan 6 step 03: the auth gate (roadmap 6.3, ADR 0014). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthGateTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val opened = mutableListOf<AppDatabase>()
    private lateinit var holder: DatabaseHolder
    private lateinit var owners: InMemoryDataOwnerStore
    private lateinit var sessions: WorkoutSessionRepositoryImpl
    private lateinit var exercises: ExerciseRepositoryImpl
    private lateinit var profiles: UserProfileRepository
    private lateinit var store: SyncLocalStore
    private lateinit var merge: GuestMerge
    private lateinit var gate: AuthGate

    // Firebase, faked
    private var firebaseUid: String? = "uid-a"
    private var probeResult = AuthProbe.VALID
    private var lastFailure: AuthProbe? = null
    private var drops = 0
    private val events = mutableListOf<String>()

    private fun file(owner: String?) = File(tmp.root, DatabaseHolder.fileName(owner))

    private fun open(owner: String?): AppDatabase =
        Room.databaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java, file(owner).absolutePath)
            .applyAppConfig().allowMainThreadQueries().build().also { opened += it }

    @Before
    fun setUp() {
        owners = InMemoryDataOwnerStore("uid-a", email = "karlo@example.com")
        holder = DatabaseHolder(owners.owner, ::open, files = DatabaseHolder.OwnerFiles.inDirectory { File(tmp.root, it) })
        val outbox = OutboxRecorder(holder, clock)
        sessions = WorkoutSessionRepositoryImpl(holder, outbox, clock)
        exercises = ExerciseRepositoryImpl(holder, outbox, clock)
        profiles = UserProfileRepository(holder, outbox, clock)
        store = SyncLocalStore(holder, clock)
        merge = GuestMerge(holder)
        gate = AuthGate(
            holder,
            owners,
            merge,
            firebaseUid = { firebaseUid },
            probe = { probeResult.also { if (it == AuthProbe.EXPIRED || it == AuthProbe.DELETED) lastFailure = it } },
            lastFailure = { lastFailure },
            clearFailure = { lastFailure = null },
            firebaseEmail = { "karlo@example.com" },
            dropDeadSession = { drops++ },
            afterSignIn = { events += "afterSignIn($it)" },
            setGuest = { events += "setGuest($it)" },
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
        gate.observeReleases()
    }

    @After
    fun tearDown() = opened.forEach { if (it.isOpen) it.close() }

    private fun engine(requests: MutableList<String>) = SyncEngine(
        store,
        recordingApis(requests),
        isSignedIn = { firebaseUid.let { it != null && it == holder.owner } },
        mutex = holder.lock
    )

    /** Firebase reports a uid, as its listener would. */
    private suspend fun firebase(uid: String?) {
        firebaseUid = uid
        gate.onFirebaseUid(uid)
    }

    // ---- classification ----

    @Test
    fun `token errors are classified by Firebase's error code`() {
        assertEquals(AuthProbe.OFFLINE, AuthProbe.classify(ExecutionException(FirebaseNetworkException("offline"))))
        assertEquals(AuthProbe.DELETED, AuthProbe.classify(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "gone")))
        assertEquals(AuthProbe.DELETED, AuthProbe.classify(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "off")))
        assertEquals(AuthProbe.EXPIRED, AuthProbe.classify(FirebaseAuthInvalidUserException("ERROR_USER_TOKEN_EXPIRED", "old")))
        assertEquals(AuthProbe.EXPIRED, AuthProbe.classify(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "bad")))
        assertEquals(AuthProbe.OFFLINE, AuthProbe.classify(IllegalStateException("unknown")))
    }

    // ---- session expired ----

    @Test
    fun `a dropped session keeps the account's file and its changes push after signing in again`() = runBlocking {
        firebase("uid-a")
        firebase(null) // Firebase dropped the user, nobody signed out

        assertEquals(AuthGateState.SessionExpired("uid-a", "karlo@example.com"), gate.state.value)
        assertEquals("uid-a", holder.owner)
        val workout = SyncFixtures.session()
        sessions.createWorkoutSession(workout) // logging goes on
        val requests = mutableListOf<String>()
        engine(requests).sync()
        assertTrue("nothing goes out while the session is dead", requests.isEmpty())

        firebase("uid-a")

        assertEquals(AuthGateState.SignedIn("uid-a"), gate.state.value)
        runCatching { engine(requests).sync() }
        assertTrue("the waiting change pushes", requests.any { it.endsWith("/sync/push") })
        assertEquals(1, store.outbox().count { it.entityId == workout.id })
    }

    @Test
    fun `a voluntary sign-out releases the owner and opens the guest file`() = runBlocking {
        firebase("uid-a")
        owners.release() // what OwnerReleasingAuthSession.signOut does first
        firebase(null)

        assertEquals(AuthGateState.Guest, gate.state.value)
        assertNull(holder.owner)
    }

    @Test
    fun `logout while the session is dead opens the guest file`() = runBlocking {
        firebase("uid-a")
        firebase(null)
        assertTrue(gate.state.value is AuthGateState.SessionExpired)

        // Logout's sign-out releases the owner first (OwnerReleasingAuthSession);
        // Firebase's uid is already null, so no uid change follows.
        owners.release()

        assertEquals(AuthGateState.Guest, gate.state.value)
        assertNull(holder.owner)
    }

    @Test
    fun `another account signing in while A is expired never sees A's changes`() = runBlocking {
        firebase(null)
        val workoutA = SyncFixtures.session()
        sessions.createWorkoutSession(workoutA)

        firebase("uid-b")

        assertEquals("uid-b", owners.owner)
        assertNull(sessions.getWorkoutSessionById(workoutA.id))
        holder.switchTo("uid-a")
        assertEquals(listOf(SyncEntityType.WORKOUT_SESSION to workoutA.id), store.outbox().map { it.entityType to it.entityId })
    }

    @Test
    fun `a sign-in clears the guest flag and runs the merge step`() = runBlocking {
        owners.release()
        holder.switchTo(null)

        firebase("uid-c")

        assertEquals(listOf("setGuest(false)", "afterSignIn(uid-c)"), events)
        assertEquals("uid-c", holder.owner)
    }

    // ---- sync 401 ----

    @Test
    fun `a 401 with an expired token drops the dead session, which shows as expired`() = runBlocking {
        probeResult = AuthProbe.EXPIRED
        gate.onUnauthorized()
        assertEquals(1, drops)

        firebase(null)
        assertTrue(gate.state.value is AuthGateState.SessionExpired)
    }

    @Test
    fun `a 401 for a deleted user shows the deleted dialog`() = runBlocking {
        probeResult = AuthProbe.DELETED
        gate.checkSession()
        firebase(null)

        assertEquals(AuthGateState.AccountDeleted("uid-a", "karlo@example.com"), gate.state.value)
        assertEquals("uid-a", holder.owner)
    }

    @Test
    fun `a 401 while offline or with a valid token changes nothing`() = runBlocking {
        probeResult = AuthProbe.OFFLINE
        gate.checkSession()
        probeResult = AuthProbe.VALID
        gate.checkSession()

        assertEquals(0, drops)
        assertEquals(AuthGateState.SignedIn("uid-a"), gate.state.value)
    }

    // ---- account deleted ----

    private suspend fun deletedWithData(): Pair<String, String> {
        val workout = SyncFixtures.session()
        sessions.createWorkoutSession(workout)
        val exercise = SyncFixtures.exercise().copy(ownerId = "uid-a")
        exercises.createExercise(exercise)
        profiles.saveProfile(SyncFixtures.profile("uid-a", name = "Karlo"))
        // As if synced: server revisions, nothing pending.
        holder.db.openHelper.writableDatabase.apply {
            execSQL("UPDATE workout_sessions SET revision = 3, syncState = 'SYNCED'")
            execSQL("UPDATE exercises SET revision = 2, syncState = 'SYNCED' WHERE source = 'CUSTOM'")
            execSQL("DELETE FROM sync_outbox")
        }
        probeResult = AuthProbe.DELETED
        gate.checkSession()
        firebase(null)
        return workout.id to exercise.id
    }

    @Test
    fun `keep on this phone turns the account's data into the guest's`() = runBlocking {
        val (workoutId, exerciseId) = deletedWithData()

        gate.keepAsGuest()

        assertEquals(AuthGateState.Guest, gate.state.value)
        assertNull(owners.owner)
        assertNull(holder.owner)
        assertFalse("account file gone", file("uid-a").exists())
        assertEquals("setGuest(true)", events.last())
        val db = holder.db
        val session = db.syncDao().getSessionIncludingDeleted(workoutId)!!
        assertEquals(0, session.revision)
        assertNotNull(sessions.getWorkoutSessionById(workoutId)!!.performedExercises.single().sets.single())
        assertNull(exercises.getExerciseById(exerciseId)!!.ownerId)
        assertEquals("Karlo", profiles.getProfile(GUEST_UID)!!.name)
        assertEquals(
            setOf(
                SyncEntityType.WORKOUT_SESSION to workoutId,
                SyncEntityType.EXERCISE to exerciseId,
                SyncEntityType.USER_PROFILE to GUEST_UID
            ),
            store.outbox().map { it.entityType to it.entityId }.toSet()
        )
    }

    @Test
    fun `keep on this phone adds to a guest file that already has data`() = runBlocking {
        holder.switchTo(null)
        val guestWorkout = SyncFixtures.session(title = "guest")
        sessions.createWorkoutSession(guestWorkout)
        holder.switchTo("uid-a")
        val (workoutId, _) = deletedWithData()

        gate.keepAsGuest()

        assertNotNull(sessions.getWorkoutSessionById(guestWorkout.id))
        assertNotNull(sessions.getWorkoutSessionById(workoutId))
        assertEquals(2, merge.openCounts().workouts)
    }

    @Test
    fun `erase after a deletion deletes the account's file`() = runBlocking {
        deletedWithData()

        gate.eraseDeletedAccount()

        assertEquals(AuthGateState.Guest, gate.state.value)
        assertFalse(file("uid-a").exists())
        assertTrue(merge.openCounts().isEmpty)
        assertEquals("setGuest(false)", events.last())
    }

    // ---- splash ----

    @Test
    fun `splash opens Main for an owner or a guest, else Login`() {
        assertTrue(startsInMain(owner = "uid-a", isGuest = false))
        assertTrue(startsInMain(owner = null, isGuest = true))
        assertFalse(startsInMain(owner = null, isGuest = false))
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

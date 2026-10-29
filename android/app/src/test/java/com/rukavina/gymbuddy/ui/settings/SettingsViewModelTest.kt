package com.rukavina.gymbuddy.ui.settings

import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.sync.FakeAuthSession
import com.rukavina.gymbuddy.data.sync.LogoutCoordinator
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.data.sync.SyncFixtures
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Logout states in Settings. Like ProfileViewModelTest, this needs
 * Robolectric and a FirebaseApp because SettingsViewModel reads
 * FirebaseAuth.getInstance() in its constructor. The coordinator is real,
 * with a fake flush and auth, over an in-memory database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var store: SyncLocalStore
    private val auth = FakeAuthSession()
    private val direct: suspend (suspend () -> Unit) -> Unit = { block -> block() }
    private val failing: suspend (suspend () -> Unit) -> Unit = { throw IllegalStateException("wipe failed") }

    @Before
    fun setUp() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:1:android:1")
                    .setApiKey("test-api-key")
                    .setProjectId("test-project")
                    .build()
            )
        }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = SyncLocalStore(db, clock)
    }

    @After
    fun tearDown() = db.close()

    private fun buildViewModel(coordinator: LogoutCoordinator) = SettingsViewModel(
        repository = UserProfileRepository(db.userProfileDao(), OutboxRecorder(db, clock), clock),
        database = db,
        logoutCoordinator = coordinator,
        appContext = org.robolectric.RuntimeEnvironment.getApplication()
    )

    private suspend fun addPendingExercise() {
        ExerciseRepositoryImpl(db.exerciseDao(), db.userExerciseStateDao(), OutboxRecorder(db, clock), clock)
            .createExercise(SyncFixtures.exercise())
    }

    /** Room runs queries off the main dispatcher, so wait for the state to settle. */
    private suspend fun SettingsViewModel.awaitSettled(): LogoutUiState =
        withTimeout(5_000) { logoutState.first { it != LogoutUiState.Flushing } }

    @Test
    fun `empty outbox signs out and emits the logout event`() = runBlocking {
        val vm = buildViewModel(LogoutCoordinator(store, { true }, auth, direct))
        val event = async(start = CoroutineStart.UNDISPATCHED) { vm.logoutEvent.first() }

        vm.logout()

        withTimeout(5_000) { event.await() }
        assertEquals(1, auth.signOuts)
        assertEquals(LogoutUiState.Idle, vm.logoutState.value)
    }

    @Test
    fun `failed flush shows the unsynced warning with counts`() = runBlocking {
        addPendingExercise()
        val vm = buildViewModel(LogoutCoordinator(store, { false }, auth, direct))

        vm.logout()

        assertEquals(LogoutUiState.UnsyncedWarning(listOf("1 custom exercise")), vm.awaitSettled())
        assertEquals(0, auth.signOuts)
    }

    @Test
    fun `requestLogout throwing ends in Failed, and cancel goes back to Idle`() = runBlocking {
        val vm = buildViewModel(LogoutCoordinator(store, { true }, auth, failing))

        vm.logout()

        val state = vm.awaitSettled()
        assertEquals(false, (state as LogoutUiState.Failed).afterWarning)
        assertEquals(0, auth.signOuts)

        vm.cancelLogout()
        assertEquals(LogoutUiState.Idle, vm.logoutState.value)
    }

    @Test
    fun `logOutAndDelete throwing ends in Failed and retry repeats it`() = runBlocking {
        addPendingExercise()
        var exclusive = failing
        val coordinator = LogoutCoordinator(store, { false }, auth, { block -> exclusive(block) })
        val vm = buildViewModel(coordinator)
        vm.logout()
        vm.awaitSettled()

        vm.confirmLogoutAndDelete()

        val state = vm.awaitSettled()
        assertEquals(true, (state as LogoutUiState.Failed).afterWarning)
        assertEquals(0, auth.signOuts)

        exclusive = direct
        val event = async(start = CoroutineStart.UNDISPATCHED) { vm.logoutEvent.first() }
        vm.retryLogout()

        withTimeout(5_000) { event.await() }
        assertEquals(1, auth.signOuts)
        assertEquals(0, db.syncDao().outboxCount())
    }
}

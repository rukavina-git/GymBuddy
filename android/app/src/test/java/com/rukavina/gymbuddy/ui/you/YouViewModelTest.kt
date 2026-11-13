package com.rukavina.gymbuddy.ui.you

import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutTemplateRepositoryImpl
import com.rukavina.gymbuddy.data.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import com.rukavina.gymbuddy.data.sync.GuestMerge
import com.rukavina.gymbuddy.data.export.DataExporter
import android.net.Uri
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.sync.AccountDeletionCoordinator
import com.rukavina.gymbuddy.data.sync.FakeAuthSession
import com.rukavina.gymbuddy.domain.model.PreferredUnits
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
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Assert.assertTrue
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
 * Sign out, delete and export on the You tab. Like ProfileViewModelTest, this needs
 * Robolectric and a FirebaseApp because YouViewModel reads
 * FirebaseAuth.getInstance() in its constructor. The coordinator is real,
 * with a fake flush and auth, over an in-memory database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class YouViewModelTest {

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
        store = SyncLocalStore(DatabaseHolder.fixed(db), clock)
    }

    @After
    fun tearDown() = db.close()

    private var online = true
    private var syncCycles = 0
    private lateinit var prefs: AppPreferencesRepository
    private var prefsFile = java.io.File("unset")

    private fun buildViewModel(
        coordinator: LogoutCoordinator = LogoutCoordinator(store, { true }, auth, direct),
        deletion: AccountDeletionCoordinator = deletionCoordinator { 204 }
    ): YouViewModel {
        val holder = DatabaseHolder.fixed(db)
        val recorder = OutboxRecorder(holder, clock)
        return YouViewModel(
            repository = UserProfileRepository(holder, recorder, clock),
            logoutCoordinator = coordinator,
            accountDeletionCoordinator = deletion,
            appContext = org.robolectric.RuntimeEnvironment.getApplication(),
            guestMerge = GuestMerge(holder),
            preferences = AppPreferencesRepository(
                PreferenceDataStoreFactory.create(produceFile = { java.io.File.createTempFile("prefs", ".preferences_pb").also { it.delete(); prefsFile = it } })
            ).also { prefs = it },
            dataExporter = DataExporter(holder, "test", clock),
            exercises = ExerciseRepositoryImpl(holder, recorder, clock),
            templates = WorkoutTemplateRepositoryImpl(holder, recorder, clock),
            store = store,
            scheduler = SyncScheduler({ syncCycles++ }, { 0 }, { true }, CoroutineScope(Dispatchers.Unconfined)),
            onlineCheck = object : OnlineCheck(org.robolectric.RuntimeEnvironment.getApplication()) {
                override fun isOnline() = online
            },
            clock = clock,
        )
    }

    private fun deletionCoordinator(deleteOnServer: suspend () -> Int) =
        AccountDeletionCoordinator(store, deleteOnServer, {}, auth, direct)

    private suspend fun addPendingExercise() {
        ExerciseRepositoryImpl(DatabaseHolder.fixed(db), OutboxRecorder(DatabaseHolder.fixed(db), clock), clock)
            .createExercise(SyncFixtures.exercise())
    }

    /** Room runs queries off the main dispatcher, so wait for the state to settle. */
    private suspend fun YouViewModel.awaitSettled(): LogoutUiState =
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
    fun `units and rest buzz are saved`() = runBlocking {
        val vm = buildViewModel()
        // One write at a time, generous timeout: DataStore writes on its own
        // IO scope, and this timed out at 5 s on PR runners (PR #10).
        vm.setUnits(PreferredUnits.IMPERIAL)
        withTimeoutOrNull(15_000) { prefs.preferredUnits.first { it == PreferredUnits.IMPERIAL } }
            ?: fail("units never saved; now ${prefs.preferredUnits.first()}, file ${prefsFile.exists()}")
        vm.setRestBuzz(true)
        withTimeoutOrNull(15_000) { prefs.restBuzz.first { it } }
            ?: fail("rest buzz never saved; now ${prefs.restBuzz.first()}, units ${prefs.preferredUnits.first()}")
        Unit
    }

    @Test
    fun `retry backup asks for a manual sync`() {
        val vm = buildViewModel()
        val before = syncCycles
        vm.retryBackup()
        assertEquals(before + 1, syncCycles)
    }

    @Test
    fun `sign out with nothing waiting signs out at once`() = runBlocking {
        val vm = buildViewModel()
        val event = async(start = CoroutineStart.UNDISPATCHED) { vm.logoutEvent.first() }

        vm.signOut()

        withTimeout(5_000) { event.await() }
        assertEquals(1, auth.signOuts)
    }

    @Test
    fun `sign out with changes waiting asks first, and says when offline`() = runBlocking {
        addPendingExercise()
        val vm = buildViewModel()

        vm.signOut()
        assertEquals(
            LogoutUiState.Confirm(listOf("1 custom exercise"), offline = false),
            withTimeout(5_000) { vm.logoutState.first { it != LogoutUiState.Idle } },
        )
        assertEquals(0, auth.signOuts)

        vm.cancelLogout()
        online = false
        vm.signOut()
        assertEquals(
            LogoutUiState.Confirm(listOf("1 custom exercise"), offline = true),
            withTimeout(5_000) { vm.logoutState.first { it != LogoutUiState.Idle } },
        )
    }

    @Test
    fun `sign out anyway from the question discards and signs out`() = runBlocking {
        addPendingExercise()
        val vm = buildViewModel(LogoutCoordinator(store, { false }, auth, direct))
        vm.signOut()
        withTimeout(5_000) { vm.logoutState.first { it is LogoutUiState.Confirm } }
        val event = async(start = CoroutineStart.UNDISPATCHED) { vm.logoutEvent.first() }

        vm.confirmLogoutAndDelete()

        withTimeout(5_000) { event.await() }
        assertEquals(1, auth.signOuts)
        assertEquals(0, db.syncDao().outboxCount())
    }

    @Test
    fun `ui state shows the waiting change and hidden counts`() = runBlocking {
        val exercises = ExerciseRepositoryImpl(DatabaseHolder.fixed(db), OutboxRecorder(DatabaseHolder.fixed(db), clock), clock)
        val exercise = SyncFixtures.exercise()
        exercises.createExercise(exercise)
        exercises.hideExercise(exercise.id)
        val vm = buildViewModel()

        val state = withTimeout(5_000) { vm.uiState.first { !it.isLoading && it.hiddenExercises == 1 } }

        assertTrue(state.backup is BackupLine.Waiting)
        assertEquals(0, state.hiddenTemplates)
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

    // ---- account deletion ----

    private suspend fun YouViewModel.awaitDeletionSettled(): AccountDeletionUiState =
        withTimeout(5_000) { accountDeletionState.first { it != AccountDeletionUiState.InProgress } }

    @Test
    fun `server failure ends in a closable, retryable state and deletes nothing`() = runBlocking {
        addPendingExercise()
        var status = 500
        val vm = buildViewModel(deletion = deletionCoordinator { status })

        vm.deleteAccount()

        val state = vm.awaitDeletionSettled() as AccountDeletionUiState.Failed
        assertEquals(true, state.canRetry)
        assertEquals(0, auth.signOuts)
        assertEquals(1, db.syncDao().outboxCount())

        status = 204
        val event = async(start = CoroutineStart.UNDISPATCHED) { vm.logoutEvent.first() }
        vm.retryAccountDeletion()

        withTimeout(5_000) { event.await() }
        assertEquals(1, auth.signOuts)
        assertEquals(0, db.syncDao().outboxCount())
    }

    @Test
    fun `401 ends in Failed without retry, and close goes back to Idle`() = runBlocking {
        val vm = buildViewModel(deletion = deletionCoordinator { 401 })

        vm.deleteAccount()

        val state = vm.awaitDeletionSettled() as AccountDeletionUiState.Failed
        assertEquals(false, state.canRetry)
        vm.dismissAccountDeletion()
        assertEquals(AccountDeletionUiState.Idle, vm.accountDeletionState.value)
    }

    @Test
    fun `success emits logoutEvent`() = runBlocking {
        val vm = buildViewModel()
        val event = async(start = CoroutineStart.UNDISPATCHED) { vm.logoutEvent.first() }

        vm.deleteAccount()

        withTimeout(5_000) { event.await() }
        assertEquals(AccountDeletionUiState.Idle, vm.accountDeletionState.value)
        assertEquals(1, auth.signOuts)
    }

    @Test
    fun `the confirmation dialog lists unsynced changes`() = runBlocking {
        addPendingExercise()
        val vm = buildViewModel()

        vm.loadDeletionWarning()

        assertEquals(listOf("1 custom exercise"), withTimeout(5_000) { vm.deletionUnsyncedLines.first { it.isNotEmpty() } })
    }

    @Test
    fun `export writes the JSON to the picked file and reports done`() = runBlocking {
        addPendingExercise()
        val file = java.io.File.createTempFile("export", ".json")
        val vm = buildViewModel()

        vm.export(Uri.fromFile(file))

        assertEquals(ExportUiState.Done, withTimeout(5_000) { vm.exportState.first { it != ExportUiState.Saving } })
        val json = file.readText()
        assertTrue(json.contains("\"formatVersion\": 1"))
        assertTrue(json.contains("Custom Curl"))
        vm.clearExportState()
        assertEquals(ExportUiState.Idle, vm.exportState.value)
    }

    @Test
    fun `export to a file that can't be opened reports failed`() = runBlocking {
        val vm = buildViewModel()

        vm.export(Uri.parse("content://no.such.provider/export.json"))

        assertEquals(ExportUiState.Failed, withTimeout(5_000) { vm.exportState.first { it != ExportUiState.Saving } })
    }

    @Test
    fun `password actions report not signed in without a Firebase user`() {
        val vm = buildViewModel()
        val results = mutableListOf<Pair<Boolean, String?>>()

        vm.setupPassword("secret123") { ok, msg -> results += ok to msg }
        vm.changePassword("old", "new12345") { ok, msg -> results += ok to msg }
        vm.deleteAccountWithPassword("secret123") { ok, msg -> results += ok to msg }

        assertEquals(List(3) { false to "Not signed in" }, results)
    }
}

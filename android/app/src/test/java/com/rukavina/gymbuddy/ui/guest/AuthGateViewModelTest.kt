package com.rukavina.gymbuddy.ui.guest

import android.content.Context
import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.db.applyAppConfig
import com.rukavina.gymbuddy.data.sync.AuthGate
import com.rukavina.gymbuddy.data.sync.AuthGateState
import com.rukavina.gymbuddy.data.sync.AuthProbe
import com.rukavina.gymbuddy.data.sync.GuestMerge
import com.rukavina.gymbuddy.data.sync.InMemoryDataOwnerStore
import com.rukavina.gymbuddy.data.sync.SharedPrefsSignedOutStore
import com.rukavina.gymbuddy.data.sync.SignedOutPrompt
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

/** Plan 9 step 04a: the signed-out dialog rules and the deleted-account actions. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthGateViewModelTest {

    @get:Rule val main = MainDispatcherRule()
    @get:Rule val tmp = TemporaryFolder()

    private val opened = mutableListOf<AppDatabase>()
    private lateinit var holder: DatabaseHolder
    private lateinit var gate: AuthGate
    private lateinit var store: SharedPrefsSignedOutStore
    private lateinit var vm: AuthGateViewModel

    private var firebaseUid: String? = "uid-a"
    private var probeResult = AuthProbe.VALID
    private val failure = MutableStateFlow<AuthProbe?>(null)
    private var now = 1_000_000L
    private val hour = 60 * 60 * 1000L

    private fun open(owner: String?): AppDatabase =
        Room.databaseBuilder(
            RuntimeEnvironment.getApplication(),
            AppDatabase::class.java,
            File(tmp.root, DatabaseHolder.fileName(owner)).absolutePath
        ).applyAppConfig().allowMainThreadQueries().build().also { opened += it }

    @Before
    fun setUp() {
        val owners = InMemoryDataOwnerStore("uid-a", email = "karlo@example.com")
        holder = DatabaseHolder(owners.owner, ::open, files = DatabaseHolder.OwnerFiles.inDirectory { File(tmp.root, it) })
        val merge = GuestMerge(holder)
        gate = AuthGate(
            holder,
            owners,
            merge,
            firebaseUid = { firebaseUid },
            probe = { probeResult.also { if (it == AuthProbe.EXPIRED || it == AuthProbe.DELETED) failure.value = it } },
            failure = failure,
            clearFailure = { failure.value = null },
            firebaseEmail = { "karlo@example.com" },
            dropDeadSession = {},
            afterSignIn = {},
            setGuest = {},
            scope = CoroutineScope(Dispatchers.Unconfined),
            failureWaitMs = 20
        )
        val prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences(SignedOutPrompt.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = SharedPrefsSignedOutStore(prefs)
        vm = AuthGateViewModel(gate, merge, SignedOutPrompt(store) { now })
    }

    @After
    fun tearDown() = opened.forEach { if (it.isOpen) it.close() }

    private suspend fun firebase(uid: String?) {
        firebaseUid = uid
        gate.onFirebaseUid(uid)
    }

    @Test
    fun `an expired session opens the dialog once and Continue counts`() = runBlocking {
        firebase("uid-a")
        assertFalse(vm.signedOutDialog.value)

        firebase(null)
        assertTrue(vm.state.value is AuthGateState.SessionExpired)
        assertTrue(vm.signedOutDialog.value)

        vm.continueWithoutAccount()
        assertFalse(vm.signedOutDialog.value)
        assertEquals(1, store.continues)
        assertEquals(now, store.lastContinueAt)
    }

    @Test
    fun `within 48 hours only a marker tap opens it, and that tap doesn't count`() = runBlocking {
        firebase(null)
        vm.continueWithoutAccount()

        now += 47 * hour
        firebase("uid-a") // signed in again closes it
        firebase(null)
        assertFalse(vm.signedOutDialog.value)

        vm.openSignedOutDialog()
        assertTrue(vm.signedOutDialog.value)
        vm.continueWithoutAccount()
        assertFalse(vm.signedOutDialog.value)
        assertEquals(1, store.continues)
    }

    @Test
    fun `a marker tap does nothing while signed in`() = runBlocking {
        firebase("uid-a")
        vm.openSignedOutDialog()
        assertFalse(vm.signedOutDialog.value)
    }

    @Test
    fun `back from Sign in without signing in brings the dialog back`() = runBlocking {
        firebase(null)
        vm.leaveForSignIn()
        assertFalse(vm.signedOutDialog.value)

        vm.onMainResumed()
        assertTrue(vm.signedOutDialog.value)

        vm.onMainResumed() // a second resume without leaving changes nothing
        assertTrue(vm.signedOutDialog.value)
    }

    @Test
    fun `signing in from the dialog closes it for good`() = runBlocking {
        firebase(null)
        vm.leaveForSignIn()
        firebase("uid-a")

        vm.onMainResumed()
        assertFalse(vm.signedOutDialog.value)
    }

    @Test
    fun `a deleted account shows counts, Keep makes the data the guest's`() = runBlocking {
        probeResult = AuthProbe.DELETED
        gate.checkSession()
        firebase(null)
        assertTrue(vm.state.value is AuthGateState.AccountDeleted)

        vm.loadCounts()
        assertNotNull(withTimeout(5_000) { vm.counts.first { it != null } })

        vm.keep()
        assertEquals(AuthGateState.Guest, withTimeout(5_000) { vm.state.first { it is AuthGateState.Guest } })
    }

    @Test
    fun `Erase after a deletion leaves to Login`() = runBlocking {
        probeResult = AuthProbe.DELETED
        gate.checkSession()
        firebase(null)

        val erased = async(Dispatchers.Unconfined) { withTimeout(5_000) { vm.erased.first() } }
        vm.erase()
        erased.await()
        assertEquals(AuthGateState.Guest, vm.state.value)
    }
}

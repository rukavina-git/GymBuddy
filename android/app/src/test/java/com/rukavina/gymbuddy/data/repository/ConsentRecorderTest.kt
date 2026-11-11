package com.rukavina.gymbuddy.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConsentRecorderTest {

    private val clock = Clock.fixed(Instant.ofEpochMilli(9_000L), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private val preferences = AppPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
            File.createTempFile("consent", ".preferences_pb").apply { delete(); deleteOnExit() }
        }
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = db.close()

    private fun recorder(owner: String?): Pair<ConsentRecorder, UserProfileRepository> {
        val holder = DatabaseHolder.fixed(db, owner)
        val profiles = UserProfileRepository(holder, OutboxRecorder(holder, clock), clock)
        return ConsentRecorder(preferences, profiles, clock) to profiles
    }

    @Test
    fun `a pending consent lands on the account's profile, keeping the rest`() = runBlocking {
        val (recorder, profiles) = recorder("uid-1")
        profiles.saveProfile(UserProfile(uid = "uid-1", name = "Kim", email = "", joinedDate = 1L, experienceLevel = ExperienceLevel.SOME))
        preferences.setPendingConsentAt(4_000L)

        recorder.applyTo("uid-1")

        val saved = profiles.getProfile("uid-1")!!
        assertEquals(ConsentRecorder.CONSENT_VERSION, saved.healthDataConsentVersion)
        assertEquals(4_000L, saved.healthDataConsentAt)
        assertEquals("Kim", saved.name)
        assertEquals(ExperienceLevel.SOME, saved.experienceLevel)
        assertNull(preferences.pendingConsentAt.first())
    }

    @Test
    fun `a new account without a profile gets a bare one`() = runBlocking {
        val (recorder, profiles) = recorder("uid-1")
        preferences.setPendingConsentAt(4_000L)
        recorder.applyTo("uid-1")
        assertEquals(4_000L, profiles.getProfile("uid-1")!!.healthDataConsentAt)
        assertEquals(9_000L, profiles.getProfile("uid-1")!!.joinedDate)
    }

    @Test
    fun `nothing pending or another owner's file changes nothing`() = runBlocking {
        val (recorder, profiles) = recorder("uid-1")
        recorder.applyTo("uid-1")
        assertNull(profiles.getProfile("uid-1"))

        preferences.setPendingConsentAt(4_000L)
        recorder.applyTo("uid-2")
        assertNull(profiles.getProfile("uid-2"))
        assertEquals(4_000L, preferences.pendingConsentAt.first())

        val (guestRecorder, guestProfiles) = recorder(null)
        guestRecorder.applyTo(UserProfileRepository.GUEST_UID)
        assertNull(guestProfiles.getProfile(UserProfileRepository.GUEST_UID))
    }
}

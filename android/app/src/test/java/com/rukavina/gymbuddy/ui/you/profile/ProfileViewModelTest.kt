package com.rukavina.gymbuddy.ui.you.profile

import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.BodyWeightRepositoryImpl
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.BodyWeightEntry
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.Gender
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.UserProfile
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Profile (7.10b): avatar and About you edits write the stored row and keep the rest. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val clock = Clock.fixed(Instant.parse("2026-11-02T09:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var profiles: UserProfileRepository
    private lateinit var weights: BodyWeightRepositoryImpl
    private val prefs = AppPreferencesRepository(testPreferencesDataStore())

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).allowMainThreadQueries().build()
        val holder = DatabaseHolder.fixed(db, owner = UID)
        profiles = UserProfileRepository(holder, OutboxRecorder(holder, clock), clock)
        weights = BodyWeightRepositoryImpl(holder, OutboxRecorder(holder, clock), clock)
    }

    @After
    fun tearDown() = db.close()

    private fun vm() = ProfileViewModel(
        profiles, weights, prefs,
        object : SignInInfo() { override fun read() = Info("a@b.c", true) },
        clock,
    )

    private fun stored(): UserProfile = runBlocking { profiles.getProfile(UID)!! }

    private fun waitUntil(condition: () -> Boolean) {
        val end = System.currentTimeMillis() + 2_000
        while (!condition()) {
            check(System.currentTimeMillis() < end) { "timed out" }
            Thread.sleep(5)
        }
    }

    private fun seed() = runBlocking {
        profiles.saveProfile(
            UserProfile(uid = UID, name = "Karlo", email = "", joinedDate = 1, experienceLevel = ExperienceLevel.SOME, healthDataConsentVersion = "2026-11"),
        )
    }

    @Test
    fun `state shows the profile, the auth email and the newest body weight`() = runBlocking {
        seed()
        weights.save(BodyWeightEntry("w1", 1_000, 82.4f))
        val v = vm()
        waitUntil { v.uiState.value.bodyWeightKg == 82.4f }
        val s = v.uiState.value
        assertEquals("Karlo", s.name)
        assertEquals("a@b.c", s.email)
        assertTrue(s.viaGoogle)
        assertFalse(s.isGuest)
        assertEquals(ExperienceLevel.SOME, s.experience)
    }

    @Test
    fun `avatar saves an animal id and clears back to the letter, unknown ids are ignored`() {
        seed()
        val v = vm()
        v.setAvatar("fox")
        waitUntil { stored().avatarId == "fox" }
        v.setAvatar("red-panda")
        v.setAvatar(null)
        waitUntil { stored().avatarId == null }
        assertEquals("2026-11", stored().healthDataConsentVersion)
    }

    @Test
    fun `a new profile gets a random animal, an existing one keeps no picture`() = runBlocking {
        profiles.random = kotlin.random.Random(7)
        val expected = com.rukavina.gymbuddy.domain.model.AvatarIds.random(kotlin.random.Random(7))
        profiles.updateProfile(UID, { UserProfile(uid = UID, name = "", email = "", joinedDate = 1) }) { it }
        assertEquals(expected, stored().avatarId)

        profiles.updateProfile(UID, { error("row exists") }) { it.copy(avatarId = null) }
        profiles.updateProfile(UID, { error("row exists") }) { it.copy(name = "Karlo") }
        assertEquals(null, stored().avatarId)
    }

    @Test
    fun `avatar ids and pictures list the same animals`() {
        assertEquals(
            com.rukavina.gymbuddy.domain.model.AvatarIds.all,
            com.rukavina.gymbuddy.ui.you.Avatars.all.map { it.id },
        )
    }

    @Test
    fun `about you edits save, clear, and keep other fields`() {
        seed()
        val v = vm()
        v.setGender(Gender.FEMALE)
        v.setActivity(ActivityLevel.VERY_ACTIVE)
        v.setGoal(FitnessGoal.GET_STRONGER)
        v.setBirthDate(946_684_800_000)
        v.setExperience(ExperienceLevel.EXPERIENCED)
        assertTrue(v.setHeight("180"))
        assertTrue(v.setTargetWeight("75,5"))
        waitUntil { stored().targetWeight == 75.5f }
        val p = stored()
        assertEquals(Gender.FEMALE, p.gender)
        assertEquals(ActivityLevel.VERY_ACTIVE, p.activityLevel)
        assertEquals(FitnessGoal.GET_STRONGER, p.fitnessGoal)
        assertEquals(946_684_800_000, p.birthDate)
        assertEquals(ExperienceLevel.EXPERIENCED, p.experienceLevel)
        assertEquals(180f, p.height)
        assertEquals("Karlo", p.name)
        assertEquals("2026-11", p.healthDataConsentVersion)

        v.setGender(null)
        assertTrue(v.setHeight(""))
        waitUntil { stored().height == null && stored().gender == null }
    }

    @Test
    fun `out of range numbers are refused, imperial converts to metric`() = runBlocking {
        seed()
        prefs.setPreferredUnits(PreferredUnits.IMPERIAL)
        val v = vm()
        waitUntil { v.uiState.value.units == PreferredUnits.IMPERIAL }
        assertFalse(v.setHeight("5"))
        assertFalse(v.setTargetWeight("2000"))
        assertTrue(v.setHeight("70"))
        waitUntil { stored().height != null }
        assertEquals(177.8f, stored().height!!, 0.01f)
    }

    @Test
    fun `blank names are ignored`() {
        seed()
        val v = vm()
        v.setName("   ")
        v.setName("  Ivan ")
        waitUntil { stored().name == "Ivan" }
    }

    @Test
    fun `a guest edit creates the guest row`() {
        val holder = DatabaseHolder.fixed(db, owner = null)
        val guestProfiles = UserProfileRepository(holder, OutboxRecorder(holder, clock), clock)
        val v = ProfileViewModel(guestProfiles, weights, prefs, object : SignInInfo() { override fun read() = Info("", false) }, clock)
        v.setAvatar("owl")
        waitUntil { runBlocking { guestProfiles.getProfile(UserProfileRepository.GUEST_UID)?.avatarId } == "owl" }
        assertNull(runBlocking { guestProfiles.getProfile(UID) })
    }

    private companion object { const val UID = "user-1" }
}

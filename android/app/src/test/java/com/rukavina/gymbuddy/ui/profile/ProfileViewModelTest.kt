package com.rukavina.gymbuddy.ui.profile

import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.domain.model.ActivityLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.Gender
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
 * ProfileViewModel reads FirebaseAuth.getInstance().currentUser?.uid once in
 * its constructor (not injected), so it needs a FirebaseApp to exist at all
 * under Robolectric. No user is ever signed in during a unit test though -
 * there's no fake for a static Firebase singleton without a mocking
 * framework, and none is in this project - so uid is always null here. That
 * makes onSaveClick/loadProfile/saveProfileToDatabase (every path gated on
 * `uid?.let { ... }`) unreachable from a unit test as this class is
 * currently structured; injecting the uid (or a thin auth interface) would
 * fix that, but is a production-code change outside this task's scope.
 *
 * What IS reachable, and covered here: every pure state-transform method
 * that doesn't depend on uid.
 *
 * Still Robolectric-run, unlike the other ViewModel tests in this suite -
 * FirebaseApp.initializeApp() needs a real Context, so there's no avoiding
 * it here. That means these tests execute and pass for real, but (per
 * ActiveWorkoutViewModelTest's file comment) contribute nothing to the
 * JaCoCo coverage number - a known, accepted gap for this file specifically.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        // Robolectric reuses one classloader (and so one static FirebaseApp
        // registry) across every test method in this class - only the first
        // method's setUp() actually needs to register the default app.
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
    }

    private fun buildViewModel(): ProfileViewModel {
        val db = Room.inMemoryDatabaseBuilder(org.robolectric.RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        return ProfileViewModel(
            repository = UserProfileRepository(db.userProfileDao(), fixedClock),
            appPreferencesRepository = AppPreferencesRepository(testPreferencesDataStore()),
            clock = fixedClock
        )
    }

    @Test
    fun `calculateAge computes whole years elapsed from a birth date`() {
        // calculateAge measures against the real system clock (LocalDate.now()),
        // not the injected Clock - so the fixture must too, or this test
        // would drift out of sync with whatever "today" actually is.
        val viewModel = buildViewModel()
        val twentyFiveYearsAgo = java.time.LocalDate.now().minusYears(25)
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(25, viewModel.calculateAge(twentyFiveYearsAgo))
    }

    @Test
    fun `calculateAge returns null for a null birth date`() {
        val viewModel = buildViewModel()
        assertNull(viewModel.calculateAge(null))
    }

    @Test
    fun `onFieldChanged updates name, bio and parses numeric fields`() {
        val viewModel = buildViewModel()

        viewModel.onFieldChanged(ProfileField.Name, "Alex")
        viewModel.onFieldChanged(ProfileField.Bio, "Lifts things")
        viewModel.onFieldChanged(ProfileField.Weight, "82.5")
        viewModel.onFieldChanged(ProfileField.Height, "180")
        viewModel.onFieldChanged(ProfileField.TargetWeight, "75")

        val state = viewModel.uiState.value
        assertEquals("Alex", state.name)
        assertEquals("Lifts things", state.bio)
        assertEquals(82.5f, state.weight)
        assertEquals(180f, state.height)
        assertEquals(75f, state.targetWeight)
    }

    @Test
    fun `onFieldChanged sets weight to null on unparseable input rather than crashing`() {
        val viewModel = buildViewModel()

        viewModel.onFieldChanged(ProfileField.Weight, "not a number")

        assertNull(viewModel.uiState.value.weight)
    }

    @Test
    fun `onGenderChanged, onFitnessGoalChanged and onActivityLevelChanged update their fields independently`() {
        val viewModel = buildViewModel()

        viewModel.onGenderChanged(Gender.FEMALE)
        viewModel.onFitnessGoalChanged(FitnessGoal.BUILD_MUSCLE)
        viewModel.onActivityLevelChanged(ActivityLevel.VERY_ACTIVE)

        val state = viewModel.uiState.value
        assertEquals(Gender.FEMALE, state.gender)
        assertEquals(FitnessGoal.BUILD_MUSCLE, state.fitnessGoal)
        assertEquals(ActivityLevel.VERY_ACTIVE, state.activityLevel)
    }

    @Test
    fun `hasUnsavedChanges is false until a field diverges from the saved baseline`() {
        val viewModel = buildViewModel()
        assertFalse(viewModel.hasUnsavedChanges())

        viewModel.onFieldChanged(ProfileField.Name, "Alex")

        assertTrue(viewModel.hasUnsavedChanges())
    }

    @Test
    fun `onCancelClick reverts to the saved baseline and drops any pending message`() {
        val viewModel = buildViewModel()
        viewModel.onFieldChanged(ProfileField.Name, "Alex")
        assertTrue(viewModel.hasUnsavedChanges())

        viewModel.onCancelClick()

        assertFalse(viewModel.hasUnsavedChanges())
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `onImageSelected updates the profile image uri`() {
        val viewModel = buildViewModel()

        viewModel.onImageSelected("content://photo/1")

        assertEquals("content://photo/1", viewModel.uiState.value.profileImageUri)
    }
}

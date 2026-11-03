package com.rukavina.gymbuddy.ui.onboarding

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.StarterPlanEntity
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.StarterPlanRepository
import com.rukavina.gymbuddy.data.repository.UserProfileRepository
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.domain.model.ExperienceLevel
import com.rukavina.gymbuddy.domain.model.FitnessGoal
import com.rukavina.gymbuddy.domain.model.TrainingDays
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val main = MainDispatcherRule(dispatcher)

    private val clock = Clock.fixed(Instant.ofEpochMilli(5_000L), ZoneOffset.UTC)
    private lateinit var db: AppDatabase
    private lateinit var holder: DatabaseHolder
    private val preferences = AppPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())) {
            File.createTempFile("onboarding", ".preferences_pb").apply { delete(); deleteOnExit() }
        }
    )

    @Before
    fun setUp() {
        // Direct executors: Room's suspend calls finish before the next line.
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        holder = DatabaseHolder.fixed(db, owner = null) // fresh install: the guest's file
        runBlocking {
            db.starterPlanDao().insertPlans(
                listOf(
                    plan(FULL, TrainingDays.ONE_TO_THREE, 0),
                    plan(UPPER, TrainingDays.FOUR, 1),
                    plan(PPL, TrainingDays.FIVE_TO_SEVEN, 2),
                    plan(OLD, TrainingDays.FOUR, 3, deprecated = true),
                )
            )
        }
    }

    @After
    fun tearDown() = db.close()

    private fun plan(id: String, days: TrainingDays, order: Int, deprecated: Boolean = false) =
        StarterPlanEntity(id, id, null, days, order, deprecated)

    private val profiles get() = UserProfileRepository(holder, OutboxRecorder(holder, clock), clock)

    private fun vm(saved: SavedStateHandle = SavedStateHandle()) =
        OnboardingViewModel(saved, profiles, StarterPlanRepository(holder, clock), preferences, clock)

    @Test
    fun `a tap holds the answer a moment, then moves on`() = runTest(dispatcher) {
        val vm = vm()
        vm.chooseExperience(ExperienceLevel.SOME)
        assertEquals(SetupStep.EXPERIENCE, vm.state.value.step)
        assertEquals(ExperienceLevel.SOME, vm.state.value.experience.value)

        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS)
        runCurrent()
        assertEquals(SetupStep.GOAL, vm.state.value.step)
    }

    @Test
    fun `back keeps the answer, and leaves from the first question`() = runTest(dispatcher) {
        val vm = vm()
        vm.chooseExperience(ExperienceLevel.NEW)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseGoal(FitnessGoal.GET_STRONGER)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        assertEquals(SetupStep.DAYS, vm.state.value.step)

        assertTrue(vm.back())
        assertEquals(SetupStep.GOAL, vm.state.value.step)
        assertEquals(FitnessGoal.GET_STRONGER, vm.state.value.goal.value)
        assertTrue(vm.back())
        assertFalse(vm.back())
        assertEquals(SetupStep.EXPERIENCE, vm.state.value.step)
    }

    @Test
    fun `back during the hold cancels the move`() = runTest(dispatcher) {
        val vm = vm()
        vm.chooseExperience(ExperienceLevel.NEW)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseGoal(FitnessGoal.LOSE_WEIGHT)
        vm.back()
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        assertEquals(SetupStep.EXPERIENCE, vm.state.value.step)
    }

    @Test
    fun `days pick the suggested plan, deprecated plans are hidden`() = runTest(dispatcher) {
        val vm = vm()
        assertEquals(listOf(FULL, UPPER, PPL), vm.state.value.plans.map { it.id })
        vm.chooseExperience(ExperienceLevel.NEW)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseGoal(FitnessGoal.BUILD_MUSCLE)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseDays(TrainingDays.FIVE_TO_SEVEN)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)

        assertEquals(SetupStep.PLAN, vm.state.value.step)
        assertEquals(PPL, vm.state.value.suggested?.id)
        assertEquals(PPL, vm.state.value.finalPlanId)
    }

    @Test
    fun `not sure yet suggests by experience`() = runTest(dispatcher) {
        val vm = vm()
        vm.chooseExperience(ExperienceLevel.EXPERIENCED)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseGoal(FitnessGoal.BUILD_MUSCLE)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseDays(null)
        assertTrue(vm.state.value.days.answered)
        assertEquals(UPPER, vm.state.value.suggested?.id)
    }

    @Test
    fun `start with no plan is an answer, not a skip`() = runTest(dispatcher) {
        val vm = vm()
        vm.skip()
        assertEquals(SetupStep.READY, vm.state.value.step)
        assertEquals(FULL, vm.state.value.finalPlanId) // skipped: the default suggestion

        val vm2 = vm()
        repeat(3) {
            when (vm2.state.value.step) {
                SetupStep.EXPERIENCE -> vm2.chooseExperience(ExperienceLevel.NEW)
                SetupStep.GOAL -> vm2.chooseGoal(FitnessGoal.BUILD_MUSCLE)
                else -> vm2.chooseDays(TrainingDays.FOUR)
            }
            advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        }
        vm2.choosePlan(null)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        assertEquals(SetupStep.READY, vm2.state.value.step)
        assertNull(vm2.state.value.finalPlanId)
    }

    @Test
    fun `let's go saves the answers on the guest's profile and opens as a guest`() = runTest(dispatcher) {
        val vm = vm()
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launchCollect(vm, events)
        vm.chooseExperience(ExperienceLevel.SOME)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseGoal(FitnessGoal.GET_STRONGER)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseDays(TrainingDays.FOUR)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.skip()
        vm.finish()

        withTimeout(5_000) { preferences.isGuest.first { it } }
        val saved = profiles.getProfile(UserProfileRepository.GUEST_UID)!!
        assertEquals(ExperienceLevel.SOME, saved.experienceLevel)
        assertEquals(FitnessGoal.GET_STRONGER, saved.fitnessGoal)
        assertEquals(TrainingDays.FOUR, saved.trainingDays)
        assertEquals(UPPER, saved.activePlanId)
        assertEquals(5_000L, saved.joinedDate)
        assertEquals(listOf(OnboardingEvent.Done), events)
    }

    @Test
    fun `answers survive a process restart`() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val vm = vm(saved)
        vm.chooseExperience(ExperienceLevel.NEW)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseGoal(FitnessGoal.IMPROVE_ENDURANCE)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)
        vm.chooseDays(null)
        advanceTimeBy(OnboardingViewModel.PRESS_HOLD_MS + 1)

        val restored = vm(saved).state.value
        assertEquals(SetupStep.PLAN, restored.step)
        assertEquals(ExperienceLevel.NEW, restored.experience.value)
        assertEquals(FitnessGoal.IMPROVE_ENDURANCE, restored.goal.value)
        assertTrue(restored.days.answered)
        assertNull(restored.days.value)
        assertFalse(restored.plan.answered)
    }

    private fun CoroutineScope.launchCollect(vm: OnboardingViewModel, into: MutableList<OnboardingEvent>) =
        launch(dispatcher) { vm.events.collect { into += it } }

    private companion object {
        const val FULL = "full"
        const val UPPER = "upper"
        const val PPL = "ppl"
        const val OLD = "old"
    }
}

package com.rukavina.gymbuddy.ui.workout.active

import android.app.Application
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.sync.InMemoryDataOwnerStore
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.TemplateExercise
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.sync.RecordingSyncRequester
import com.rukavina.gymbuddy.domain.usecase.exercise.GetAllExercisesIncludingHiddenUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.CreateWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.ValidateWorkoutSessionSetsUseCase
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeActiveWorkoutDraftRepository
import com.rukavina.gymbuddy.testutil.FakeRestBuzzScheduler
import com.rukavina.gymbuddy.testutil.FakeWorkoutServiceStarter
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeWorkoutSessionRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.MutableClock
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.workout.ActiveWorkoutViewModel
import kotlinx.coroutines.cancelChildren
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Workout interaction: Done, swipe delete + Undo, keypad Next, stepper hold, panel layout (Plan 7 06a). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w412dp-h915dp")
class ActiveWorkoutUiTest {

    @get:Rule val rule = createComposeRule()

    private lateinit var vm: ActiveWorkoutViewModel

    private fun show(width: Int? = null, second: Boolean = false, drafts: FakeActiveWorkoutDraftRepository = FakeActiveWorkoutDraftRepository()) {
        val exercises = FakeExerciseRepository(listOf(exercise("bench", "Bench Press"), exercise("row", "Barbell Row")))
        val sessions = FakeWorkoutSessionRepository()
        val ids = FixedIdGenerator((0..20).map { "id-$it" })
        vm = ActiveWorkoutViewModel(
            CreateWorkoutSessionUseCase(sessions, ids, ValidateWorkoutSessionSetsUseCase(exercises), RecordingSyncRequester()),
            GetAllExercisesIncludingHiddenUseCase(exercises),
            AppPreferencesRepository(testPreferencesDataStore()),
            ids,
            drafts,
            sessions,
            InMemoryDataOwnerStore(),
            MutableClock(),
            FakeWorkoutServiceStarter(),
            FakeRestBuzzScheduler(),
            exercises,
        )
        vm.startWorkoutFromTemplate(
            WorkoutTemplate(
                id = "t", title = "Push A",
                templateExercises = listOf(
                    TemplateExercise(
                        id = "te", exerciseId = "bench", exerciseName = "", exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
                        plannedSets = 3, plannedReps = 8, orderIndex = 0, restSeconds = 120, plannedWeightKg = 80f,
                    ),
                ) + if (second) listOf(
                    TemplateExercise(
                        id = "te2", exerciseId = "row", exerciseName = "", exerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
                        plannedSets = 2, plannedReps = 10, orderIndex = 1, restSeconds = 90,
                    ),
                ) else emptyList(),
            ),
        )
        rule.waitUntil(3_000) { vm.uiState.value.exercises.isNotEmpty() }
        // A stopped clock keeps the test idle.
        vm.toggleTimer()
        rule.setContent {
            GymBuddyTheme {
                val modifier = if (width != null) Modifier.width(width.dp) else Modifier
                Box(modifier) { ActiveWorkoutScreen(vm, onCollapse = {}, onSaved = {}, onDiscarded = {}) }
            }
        }
    }

    @After
    fun tearDown() {
        if (::vm.isInitialized) vm.viewModelScope.coroutineContext.cancelChildren()
    }

    private fun sets() = vm.uiState.value.exercises[0].sets

    @Test
    fun unsavedSetShowsAnAlertBannerWithBothActions() {
        val drafts = FakeActiveWorkoutDraftRepository()
        show(drafts = drafts)
        drafts.failSaves = true
        rule.onNodeWithText("Done · set 1").performClick()
        rule.waitUntil(3_000) { vm.uiState.value.unsaved != null }
        rule.onNodeWithTag(UNSAVED_BANNER_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive))
        rule.onNodeWithText("Set 1 isn't saved yet").assertExists()
        rule.onNodeWithText("Storage settings").assertExists()

        drafts.failSaves = false
        rule.onNodeWithText("Try again · set 1").performClick()
        rule.waitUntil(3_000) { vm.uiState.value.unsaved == null }
        rule.onNodeWithText("Done · set 2").assertExists()
    }

    @Test
    fun doneLandsTheSetAndStartsRest() {
        show()
        rule.onNodeWithText("Done · set 1").performClick()
        rule.waitUntil(3_000) { sets()[0].isCompleted }
        rule.onNodeWithText("Done · set 2").assertExists()
        rule.onNodeWithText("2:00 rest").assertExists()
        // Done set 1 and upcoming set 3; set 2 is the card.
        rule.onAllNodesWithText("80 kg × 8").assertCountEquals(2)
    }

    @Test
    fun swipeRightOnTheCardIsDone() {
        show()
        rule.onNodeWithText("Set 1").performTouchInput { swipeRight(startX = left, endX = left + 300.dp.toPx()) }
        rule.waitUntil(3_000) { sets()[0].isCompleted }
    }

    @Test
    fun swipeLeftDeletesAndUndoBringsItBack() {
        show()
        val ids = sets().map { it.id }
        rule.onNodeWithText("Set 2").performTouchInput { swipeLeft() }
        rule.waitUntil(3_000) { sets().size == 2 }
        rule.onNodeWithText("Set deleted").assertExists()
        rule.onNodeWithText("Undo").performClick()
        rule.waitUntil(3_000) { sets().map { it.id } == ids }
    }

    @Test
    fun keypadNextGoesFromWeightToRepsThenSaves() {
        show()
        rule.onNodeWithText("80").performClick()
        rule.onNodeWithText("Set 1 · weight").assertExists()
        rule.onNodeWithText("Next: reps").performClick()
        rule.onNodeWithText("Set 1 · reps").assertExists()
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(3_000) { vm.uiState.value.keypad == null }
    }

    @Test
    fun holdingPlusRepeats() {
        show()
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Increase weight").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(1_200)
        rule.onNodeWithContentDescription("Increase weight").performTouchInput { up() }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        val weight = sets()[0].weight.toFloat()
        assertTrue("held + should step more than once, got $weight", weight >= 85f)
    }

    @Test
    fun panelsSitSideBySideOnA360dpPhone() {
        show(width = 360)
        val kg = rule.onNodeWithText("80").fetchSemanticsNode().boundsInRoot
        val reps = rule.onNodeWithText("8").fetchSemanticsNode().boundsInRoot
        assertTrue("reps panel right of weight panel: $kg / $reps", reps.left > kg.right)
        assertEquals(kg.top, reps.top, 1f)
    }

    @Test
    fun panelsStackBelow296dp() {
        show(width = 280)
        val kg = rule.onNodeWithText("80").fetchSemanticsNode().boundsInRoot
        val reps = rule.onNodeWithText("8").fetchSemanticsNode().boundsInRoot
        assertTrue("reps panel below weight panel: $kg / $reps", reps.top > kg.bottom)
    }

    @Test
    fun noUpNextWithOneExercise() {
        show()
        rule.onAllNodesWithText("Up next").assertCountEquals(0)
    }

    // --- 06b ---

    @Test
    fun progressPillOpensAllExercisesAndReorderMovesDown() {
        show(second = true)
        rule.onNodeWithContentDescription("Workout progress", substring = true).performClick()
        rule.onNodeWithText("2 exercises · 0 done").assertExists()
        rule.onNodeWithText("0/3").assertExists()
        rule.onNodeWithText("Reorder").performClick()
        // Bench is first: its Move up is off, Move down on.
        rule.onAllNodesWithContentDescription("Move down")[0].performClick()
        rule.waitUntil(3_000) { vm.uiState.value.exercises.map { it.exerciseId } == listOf("row", "bench") }
        // Now Row is first (its Move up is off); Bench goes back up.
        rule.onAllNodesWithContentDescription("Move up")[1].performClick()
        rule.waitUntil(3_000) { vm.uiState.value.exercises.map { it.exerciseId } == listOf("bench", "row") }
    }

    @Test
    fun tappingAnExerciseInTheSheetShowsIt() {
        show(second = true)
        rule.onNodeWithContentDescription("Workout progress", substring = true).performClick()
        rule.onAllNodesWithText("Barbell Row").onLast().performClick()
        rule.waitUntil(3_000) { vm.uiState.value.currentExerciseIndex == 1 }
    }

    @Test
    fun addSetAddsAFourthSet() {
        show()
        rule.onNodeWithText("Add set").performScrollTo().performClick()
        rule.waitUntil(3_000) { sets().size == 4 }
    }

    @Test
    fun menuRestTimerSetsTheExercisesRest() {
        show()
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Rest timer · 2:00").performClick()
        rule.onNodeWithText("Bench Press · rest").assertExists()
        repeat(3) { rule.onNodeWithContentDescription("Delete last digit").performClick() }
        listOf("1", "3", "0").forEach { rule.onNodeWithText(it).performClick() }
        rule.onNodeWithText("1:30").assertExists()
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(3_000) { vm.uiState.value.exercises[0].restSeconds == 90 }
    }
}

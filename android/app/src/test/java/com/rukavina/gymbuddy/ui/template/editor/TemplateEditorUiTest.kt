package com.rukavina.gymbuddy.ui.template.editor

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancelChildren
import com.rukavina.gymbuddy.domain.usecase.template.CreateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.domain.usecase.template.StampTemplateExerciseSnapshotsUseCase
import com.rukavina.gymbuddy.domain.usecase.template.UpdateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.FakeWorkoutTemplateRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Editor interaction: swipe remove + Undo, TalkBack Move up/down (Plan 7 05b). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TemplateEditorUiTest {

    @get:Rule val rule = createComposeRule()

    private val exercises = FakeExerciseRepository(listOf(exercise("e0", "Exercise 0"), exercise("e1", "Exercise 1")))
    private val templates = FakeWorkoutTemplateRepository(listOf(template("push", "Push A")))
    private lateinit var vm: TemplateEditorViewModel

    private fun show() {
        val stamp = StampTemplateExerciseSnapshotsUseCase(exercises)
        val ids = FixedIdGenerator("n1", "n2")
        vm = TemplateEditorViewModel(
            SavedStateHandle(mapOf(TemplateEditorViewModel.ARG_TEMPLATE_ID to "push")),
            FakeTemplateSources(), templates, exercises,
            CreateWorkoutTemplateUseCase(templates, ids, stamp), UpdateWorkoutTemplateUseCase(templates, stamp), ids,
        )
        rule.setContent {
            GymBuddyTheme {
                TemplateEditorScreen(onClose = {}, onAddExercises = {}, picked = null, onPickedHandled = {}, viewModel = vm)
            }
        }
    }

    @After
    fun tearDown() {
        if (::vm.isInitialized) vm.viewModelScope.coroutineContext.cancelChildren()
    }

    private fun names() = vm.uiState.value.exercises.map { it.name }

    @Test
    fun swipeRemovesAndUndoBringsItBack() {
        show()
        rule.onNodeWithText("Exercise 0").performTouchInput { swipeLeft() }
        rule.waitUntil(3_000) { names() == listOf("Exercise 1") }
        rule.onNodeWithText("Exercise 0 removed").assertExists()
        rule.onNodeWithText("Undo").performClick()
        rule.waitUntil(3_000) { names() == listOf("Exercise 0", "Exercise 1") }
        rule.waitForIdle()
        rule.onAllNodesWithText("Exercise 0").assertCountEquals(1)
    }

    @Test
    fun moveDownAndUpActions() {
        show()
        // Only the first card can move down and not up, so this node is unique.
        val firstCardActions = rule.onAllNodes(
            SemanticsMatcher("move down, no move up") { n ->
                val labels = n.config.getOrNull(SemanticsActions.CustomActions)?.map { it.label }.orEmpty()
                "Move down" in labels && "Move up" !in labels
            },
        )
        firstCardActions.assertCountEquals(1)
        rule.runOnIdle {
            firstCardActions[0].fetchSemanticsNode().config[SemanticsActions.CustomActions]
                .first { it.label == "Move down" }.action()
        }
        rule.runOnIdle { assertEquals(listOf("Exercise 1", "Exercise 0"), names()) }
        val lastCardActions = rule.onAllNodes(
            SemanticsMatcher("move up, no move down") { n ->
                val labels = n.config.getOrNull(SemanticsActions.CustomActions)?.map { it.label }.orEmpty()
                "Move up" in labels && "Move down" !in labels
            },
        )
        rule.runOnIdle {
            lastCardActions[0].fetchSemanticsNode().config[SemanticsActions.CustomActions]
                .first { it.label == "Move up" }.action()
        }
        rule.runOnIdle { assertEquals(listOf("Exercise 0", "Exercise 1"), names()) }
    }
}

package com.rukavina.gymbuddy.ui.template.picker

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import kotlinx.coroutines.cancelChildren
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Picker numbering and Add N (Plan 7 05b, Figma Picker A). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ExercisePickerUiTest {

    @get:Rule val rule = createComposeRule()

    private val vm = ExercisePickerViewModel(
        SavedStateHandle(mapOf(ExercisePickerViewModel.ARG_IN_TEMPLATE to "a")),
        FakeExerciseRepository(listOf(exercise("a", "Alpha Press"), exercise("b", "Bravo Fly"), exercise("c", "Charlie Dip"))),
    )

    @After
    fun tearDown() = vm.viewModelScope.coroutineContext.cancelChildren()

    @Test
    fun tapsNumberInPickOrderAndAddHandsThemBack() {
        val added = mutableListOf<List<String>>()
        rule.setContent { GymBuddyTheme { ExercisePickerScreen(onBack = {}, onAdd = { added += it }, viewModel = vm) } }
        rule.onNodeWithText("In template").assertExists()
        rule.onNodeWithText("Add").assertIsNotEnabled()

        rule.onNodeWithText("Charlie Dip").performClick()
        rule.onNodeWithText("Alpha Press").performClick()
        rule.onNodeWithText("1").assertExists()
        rule.onNodeWithText("2").assertExists()
        rule.onNodeWithText("Add 2").assertIsEnabled().performClick()
        assertEquals(listOf(listOf("c", "a")), added)

        rule.onNodeWithText("Charlie Dip").performClick() // unpick: Alpha becomes 1
        rule.onNodeWithText("2").assertDoesNotExist()
        rule.onNodeWithText("Add 1").assertExists()
    }
}

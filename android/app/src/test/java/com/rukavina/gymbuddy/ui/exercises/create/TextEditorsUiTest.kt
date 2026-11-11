package com.rukavina.gymbuddy.ui.exercises.create

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.workout.active.WorkoutNoteEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** F3b: notes and quick create are full-screen editors. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class TextEditorsUiTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun workoutNoteSavesTypedTextAndCloseSavesNothing() {
        val saved = mutableListOf<String>()
        var closed = 0
        rule.setContent { GymBuddyTheme { WorkoutNoteEditor("", { saved += it }, { closed++ }) } }
        rule.onNode(hasSetTextAction()).performTextInput("Felt strong")
        rule.onNodeWithContentDescription("Close").performClick()
        assertEquals(1, closed)
        assertEquals(emptyList<String>(), saved)
        rule.onNodeWithText("Save note").performClick()
        assertEquals(listOf("Felt strong"), saved)
    }

    @Test
    fun quickCreateNeedsAName() {
        val created = mutableListOf<String>()
        rule.setContent { GymBuddyTheme { QuickCreateScreen("zercher squat", busy = false, onCreate = { created += it.name }, onDismiss = {}) } }
        rule.onNodeWithText("Create").assertIsEnabled()
        rule.onNode(hasSetTextAction()).performTextClearance()
        rule.onNodeWithText("Create").assertIsNotEnabled()
        rule.onNode(hasSetTextAction()).performTextInput("Zercher Squat")
        rule.onNodeWithText("Create").performClick()
        assertEquals(listOf("Zercher Squat"), created)
    }
}

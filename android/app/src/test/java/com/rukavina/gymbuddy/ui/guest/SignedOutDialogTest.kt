package com.rukavina.gymbuddy.ui.guest

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Plan 9 step 04a, Figma R22-1: the blocking signed-out dialog. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SignedOutDialogTest {

    @get:Rule val rule = createComposeRule()

    private val taps = mutableListOf<String>()

    private fun show() = rule.setContent {
        GymBuddyTheme { SignedOutDialog(onSignIn = { taps += "signIn" }, onContinue = { taps += "continue" }) }
    }

    @Test
    fun `plain words and both choices`() {
        show()
        rule.onNodeWithText("You're signed out").assertIsDisplayed()
        rule.onNodeWithText("Your workouts stay on this phone.").assertIsDisplayed()
        rule.onNodeWithText("Sign in").assertIsDisplayed()
        rule.onNodeWithText("Continue without account").assertIsDisplayed()
    }

    @Test
    fun `taps in the first half second are ignored, then both buttons work`() {
        rule.mainClock.autoAdvance = false
        show()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Continue without account").performClick()
        assertEquals(emptyList<String>(), taps)

        rule.mainClock.advanceTimeBy(600)
        rule.onNodeWithText("Sign in").performClick()
        rule.onNodeWithText("Continue without account").performClick()
        assertEquals(listOf("signIn", "continue"), taps)
    }
}

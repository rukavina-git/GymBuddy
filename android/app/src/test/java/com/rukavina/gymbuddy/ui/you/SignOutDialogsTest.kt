package com.rukavina.gymbuddy.ui.you

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

/** Sign out when everything is backed up: asks first (tester feedback). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SignOutDialogsTest {
    @get:Rule val rule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun show() = rule.setContent {
        GymBuddyTheme {
            SignOutDialogs(
                state = LogoutUiState.ConfirmSynced,
                onBackUp = { calls += "signOut" },
                onAnyway = { calls += "anyway" },
                onRetry = { calls += "retry" },
                onCancel = { calls += "cancel" },
            )
        }
    }

    @Test
    fun `asks before signing out`() {
        show()
        rule.onNodeWithText("Sign out?").assertIsDisplayed()
        rule.onNodeWithText("Your workouts are backed up. Sign in again to get them back on this phone.").assertIsDisplayed()
        rule.onNodeWithText("Sign out").performClick()
        assertEquals(listOf("signOut"), calls)
    }

    @Test
    fun `cancel keeps you signed in`() {
        show()
        rule.onNodeWithText("Cancel").performClick()
        assertEquals(listOf("cancel"), calls)
    }
}

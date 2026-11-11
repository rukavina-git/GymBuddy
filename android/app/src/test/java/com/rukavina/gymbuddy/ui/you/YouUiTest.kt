package com.rukavina.gymbuddy.ui.you

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** You page (Plan 7 10a): guest vs account rows. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class YouUiTest {

    @get:Rule val rule = createComposeRule()

    private val clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC)
    private val calls = mutableListOf<String>()
    private val nav = YouNav(
        onProfile = { calls += "profile" },
        onCreateAccount = { calls += "create" },
        onSignIn = { calls += "signIn" },
        onHiddenExercises = {},
        onHiddenTemplates = {},
        onAbout = {},
        onDeleteAccount = {},
        onLeft = {},
    )
    private val actions = YouActions(
        nav = nav,
        onUnits = { calls += "units:$it" },
        onRestBuzz = {},
        onNotification = {},
        onExport = {},
        onErase = { calls += "erase" },
        onRetryBackup = { calls += "retry" },
        onPassword = {},
        onSignOut = { calls += "signOut" },
        onDelete = { calls += "delete" },
    )

    /** Lazy rows below the fold aren't composed: scroll the list to them first. */
    private fun tap(text: String) {
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        rule.onNodeWithText(text).performClick()
    }

    private fun show(state: YouUiState) = rule.setContent { GymBuddyTheme { YouContent(state, actions, clock) } }

    @Test
    fun guestSeesCreateAccountAndEraseButNoAccountRows() {
        show(YouUiState(isLoading = false, isGuest = true))
        rule.onNodeWithText("Create account").performClick()
        tap("Erase data on this phone")
        rule.onNodeWithText("Sign out").assertDoesNotExist()
        rule.onNodeWithText("Delete account").assertDoesNotExist()
        assertEquals(listOf("create", "erase"), calls)
    }

    @Test
    fun accountSeesBackupLineSignOutAndDelete() {
        show(YouUiState(isLoading = false, userName = "Karlo", userEmail = "k@example.com", backup = BackupLine.Failed(3)))
        rule.onNodeWithText("Couldn't back up · 3 changes waiting").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        tap("Sign out")
        tap("Delete account")
        rule.onNodeWithText("Erase data on this phone").assertDoesNotExist()
        assertEquals(listOf("retry", "signOut", "delete"), calls)
    }

    @Test
    fun backedUpShowsHowLongAgo() {
        show(YouUiState(isLoading = false, userEmail = "k@example.com", backup = BackupLine.BackedUp(clock.millis() - 2 * 60_000)))
        rule.onNodeWithText("Backed up · 2 min ago").assertIsDisplayed()
    }

    @Test
    fun unitsToggleSavesOnTap() {
        show(YouUiState(isLoading = false, isGuest = true))
        rule.onNodeWithText("lb · mi").performClick()
        assertEquals(listOf("units:IMPERIAL"), calls)
    }
}

package com.rukavina.gymbuddy.ui.you

import android.app.Application
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.rukavina.gymbuddy.data.sync.LocalDataCounts
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** F13: delete account screen, confirm word gates the button. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DeleteAccountUiTest {

    @get:Rule val rule = createComposeRule()
    private val calls = mutableListOf<String>()

    private fun show(counts: LocalDataCounts? = LocalDataCounts(12, 1, 0)) = rule.setContent {
        GymBuddyTheme {
            DeleteAccountContent(
                counts = counts,
                unsyncedLines = listOf("2 workout sessions"),
                isGoogleOnly = false,
                busy = false,
                error = null,
                onBack = { calls += "back" },
                onDelete = { calls += "delete" },
            )
        }
    }

    private val button get() = rule.onNode(hasText("Delete account") and hasClickAction() and !hasSetTextAction())
    private val field get() = rule.onNode(hasSetTextAction())

    @Test
    fun listsWhatIsLost() {
        show()
        rule.onNodeWithText("•  12 workouts").assertExists()
        rule.onNodeWithText("•  1 template").assertExists()
        rule.onNodeWithText("custom exercise", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Not backed up yet: 2 workout sessions.").assertExists()
    }

    @Test
    fun buttonOnlyWorksAfterTypingDelete() {
        show()
        button.assertIsNotEnabled()
        field.performTextInput("delet")
        button.assertIsNotEnabled()
        field.performTextReplacement(" Delete ")
        button.assertIsEnabled().performClick()
        assertEquals(listOf("delete"), calls)
    }

    @Test
    fun staysOffUntilCountsLoad() {
        show(counts = null)
        field.performTextInput("delete")
        button.assertIsNotEnabled()
    }

    @Test
    fun backLeavesWithoutDeleting() {
        show()
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf("back"), calls)
    }

    @Test
    fun wordMatch() {
        assertTrue(isDeleteWordMatch("  DELETE ", "delete"))
        assertTrue(isDeleteWordMatch("löschen", "Löschen"))
        assertFalse(isDeleteWordMatch("delete it", "delete"))
        assertFalse(isDeleteWordMatch("", ""))
        assertTrue(canConfirmDelete("delete", "delete", countsLoaded = true, busy = false))
        assertFalse(canConfirmDelete("delete", "delete", countsLoaded = true, busy = true))
        assertFalse(canConfirmDelete("delete", "delete", countsLoaded = false, busy = false))
    }
}

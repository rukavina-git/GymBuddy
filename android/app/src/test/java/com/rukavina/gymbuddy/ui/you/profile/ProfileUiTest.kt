package com.rukavina.gymbuddy.ui.you.profile

import android.app.Application
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Profile (R15-3) and Change picture (R15-4). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ProfileUiTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun `picker marks the chosen animal selected and picks another`() {
        var picked: String? = "unset"
        rule.setContent { GymBuddyTheme { PictureSheet("fox", { picked = it }, {}) } }
        rule.onNodeWithContentDescription("Fox").assertIsSelected()
        rule.onNodeWithContentDescription("Owl").assertIsNotSelected()
        rule.onNodeWithContentDescription("Owl").performSemanticsAction(SemanticsActions.OnClick)
        assertEquals("owl", picked)
        rule.onNodeWithText("Use your letter").assertDoesNotExist()
    }

    @Test
    fun `empty about you rows show a dash and rows open their editors`() {
        val opened = mutableListOf<Any>()
        rule.setContent {
            GymBuddyTheme {
                ProfileContent(
                    ProfileUiState(loading = false, isGuest = false, name = "Karlo", email = "k@x.y", viaGoogle = true),
                    onBack = {},
                    onBodyWeight = { opened += "weight" },
                    onOpen = { opened += it },
                    onName = {},
                )
            }
        }
        rule.onNodeWithText("k@x.y · signed in with Google").assertExists()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Body weight"))
        rule.onNodeWithText("Body weight").performClick()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Target weight"))
        assertEquals(true, rule.onAllNodesWithText("—").fetchSemanticsNodes().size >= 5)
        rule.onNodeWithText("Gender").performClick()
        assertEquals(listOf("weight", ProfileSheet.Gender), opened)
    }
}

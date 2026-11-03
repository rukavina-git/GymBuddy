package com.rukavina.gymbuddy.ui.design

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.robolectric.RobolectricTestRunner
import com.rukavina.gymbuddy.ui.theme.AppIcons
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Interaction logic in the ui/design parts (step 02a). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class DesignComponentsTest {

    @get:Rule val rule = createComposeRule()

    private fun set(content: @androidx.compose.runtime.Composable () -> Unit) =
        rule.setContent { GymBuddyTheme(content) }

    @Test
    fun navBarSelectsTappedTab() {
        var selected by mutableStateOf(NavTab.Home)
        set { AppNavBar(selected = selected, onSelect = { selected = it }) }

        rule.onNodeWithText("Home").assertIsSelected()
        rule.onNodeWithText("Progress").assertIsNotSelected().performClick()

        assertEquals(NavTab.Progress, selected)
        rule.onNodeWithText("Progress").assertIsSelected()
        rule.onNodeWithText("Home").assertIsNotSelected()
    }

    @Test
    fun navBarUsesFilledIconOnlyForSelectedTab() {
        NavTab.entries.forEach { tab ->
            assert(tab.icon.filled != tab.icon.outline) { "$tab has one icon for both states" }
        }
        assertEquals(AppIcons.Tab.entries.toSet(), NavTab.entries.map { it.icon }.toSet())
    }

    @Test
    fun searchFieldClearAppearsOnlyWhenFilledAndEmptiesIt() {
        var text by mutableStateOf("")
        set { SearchField(text, { text = it }, placeholder = "Search sessions") }

        rule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
        text = "Legs"
        rule.onNodeWithContentDescription("Clear search").assertIsDisplayed().performClick()

        assertEquals("", text)
        rule.onNodeWithContentDescription("Clear search").assertDoesNotExist()
    }

    @Test
    fun chipExposesSelectedState() {
        var on by mutableStateOf(false)
        set { AppChip("Weight + reps", selected = on, onClick = { on = !on }) }

        rule.onNodeWithText("Weight + reps").assertIsNotSelected().performClick()
        rule.onNodeWithText("Weight + reps").assertIsSelected()
    }

    @Test
    fun disabledButtonIsNotClickable() {
        var clicks = 0
        set { PrimaryButton("Start", { clicks++ }, enabled = false) }

        rule.onNodeWithText("Start").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun smallButtonsAndChipsKeepA48dpTapArea() {
        set {
            androidx.compose.foundation.layout.Column {
                AppTextButton("Keep editing", {})
                PrimaryButton("Save", {}, size = ButtonSize.Small)
                AppChip("Chip", selected = false, onClick = {})
                AppIconButton(AppIcons.Ui.More, "More", {})
            }
        }
        val min = with(rule.density) { 48.dp.toPx() } - 1f
        val nodes = listOf("Keep editing", "Save", "Chip").map { rule.onNodeWithText(it) } +
            rule.onNodeWithContentDescription("More")
        nodes.forEach {
            val touch = it.fetchSemanticsNode().touchBoundsInRoot
            assert(touch.height >= min && touch.width >= min) { "tap area $touch < 48dp" }
        }
    }

    @Test
    fun buttonsAnnounceAsButtons() {
        set { SecondaryButton("All sessions", {}) }
        rule.onNodeWithText("All sessions")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun segmentedButtonsSelectOne() {
        var index by mutableStateOf(0)
        set { SegmentedButtons(listOf("List", "Calendar"), index, { index = it }) }

        rule.onNodeWithText("Calendar").performClick()
        assertEquals(1, index)
        rule.onNodeWithText("Calendar").assertIsSelected()
        rule.onNodeWithText("List").assertIsNotSelected()
    }

    @Test
    fun textFieldErrorIsAnnounced() {
        set { AppTextField("you@", {}, label = "Email", helper = "Enter a full email address", isError = true) }
        rule.onNodeWithText("you@")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Enter a full email address"))
        rule.onNodeWithText("Email").assertTextEquals("Email")
    }
}

package com.rukavina.gymbuddy.ui.design

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.DecimalFormatSymbols

/** Interaction logic in the workout + data parts (step 02b). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class WorkoutComponentsTest {

    @get:Rule val rule = createComposeRule()

    private fun set(content: @Composable () -> Unit) = rule.setContent { GymBuddyTheme(content) }

    @Test
    fun keypadEmitsEveryInput() {
        val inputs = mutableListOf<KeypadInput>()
        set { Keypad("Set 3 · weight", "82.5", "kg", onInput = { inputs += it }) }

        (0..9).forEach { rule.onNodeWithText(it.toString()).performClick() }
        rule.onNodeWithText(DecimalFormatSymbols.getInstance().decimalSeparator.toString()).performClick()
        rule.onNodeWithContentDescription("Delete last digit").performClick()
        rule.onNodeWithText("Next").performClick()
        rule.onNodeWithText("Cancel").performClick()

        val expected = (0..9).map { KeypadInput.Digit(it) } +
            listOf(KeypadInput.Decimal, KeypadInput.Backspace, KeypadInput.Next, KeypadInput.Cancel)
        assertEquals(expected, inputs)
    }

    @Test
    fun keypadGridIsOneToNineThenDecimalZeroBackspace() {
        assertEquals(12, KeypadGrid.size)
        assertEquals(KeypadInput.Digit(1), KeypadGrid.first())
        assertEquals(listOf(KeypadInput.Decimal, KeypadInput.Digit(0), KeypadInput.Backspace), KeypadGrid.takeLast(3))
    }

    @Test
    fun keypadDisabledDecimalAndNextDoNothing() {
        val inputs = mutableListOf<KeypadInput>()
        set { Keypad("Set 1 · reps", "8", "reps", onInput = { inputs += it }, decimalEnabled = false, nextEnabled = false) }

        rule.onNodeWithText(DecimalFormatSymbols.getInstance().decimalSeparator.toString())
            .assertIsNotEnabled().performClick()
        rule.onNodeWithText("Next").assertIsNotEnabled().performClick()
        assertEquals(emptyList<KeypadInput>(), inputs)
    }

    @Test
    fun stepperKeyClicksWhenEnabledOnly() {
        var clicks = 0
        var enabled by mutableStateOf(true)
        set { StepperKey(StepperSign.Plus, { clicks++ }, enabled = enabled) }

        rule.onNodeWithContentDescription("Increase").performClick()
        assertEquals(1, clicks)
        enabled = false
        rule.onNodeWithContentDescription("Increase").assertIsNotEnabled().performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun stepperKeysAndKeypadKeysKeepA48dpTapArea() {
        set {
            Column {
                StepperKey(StepperSign.Minus, {})
                KeypadKey(KeypadInput.Digit(7), {})
            }
        }
        val min = with(rule.density) { 48.dp.toPx() } - 1f
        listOf(rule.onNodeWithContentDescription("Decrease"), rule.onNodeWithText("7")).forEach {
            val touch = it.fetchSemanticsNode().touchBoundsInRoot
            assert(touch.height >= min && touch.width >= min) { "tap area $touch < 48dp" }
        }
    }

    @Test
    fun valuePanelExposesEditingAsSelectedAndOpensOnTap() {
        var editing by mutableStateOf(false)
        var minus = 0
        set {
            ValuePanel("82.5", "kg", editing = editing, onClick = { editing = true }, onMinus = { minus++ }, onPlus = {})
        }

        rule.onNodeWithText("82.5").assertIsNotSelected().performClick()
        rule.onNodeWithText("82.5").assertIsSelected()
        rule.onNodeWithContentDescription("Decrease").performClick()
        assertEquals(1, minus)
    }

    @Test
    fun valuePanelShowsStepOnlyWhileEditing() {
        var editing by mutableStateOf(false)
        set { ValuePanel("82.5", "kg", editing, {}, {}, {}, step = "± 2.5 kg") }

        rule.onNodeWithText("± 2.5 kg").assertDoesNotExist()
        editing = true
        rule.onNodeWithText("± 2.5 kg").assertExists()
    }

    @Test
    fun weekDayStatesHaveDistinctDescriptions() {
        set {
            Column {
                WeekDayState.entries.forEach { WeekDay(WeekDayUi("M", "Monday", it)) }
            }
        }
        listOf(
            "Monday, no workout",
            "Monday, worked out",
            "Monday, today, no workout yet",
            "Monday, today, worked out",
        ).forEach { rule.onNodeWithContentDescription(it).assertExists() }
    }

    @Test
    fun liveBarExposesProgressAndOpens() {
        var opened = 0
        set { LiveBar("Push A · Bench press", "Set 3 of 4", progress = 0.25f, onClick = { opened++ }) }

        rule.onNodeWithText("Push A · Bench press")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(0.25f, 0f..1f)))
            .performClick()
        assertEquals(1, opened)
    }

    @Test
    fun liveBarClampsProgress() {
        set { LiveBar("Push A", "Set 5 of 4", progress = 1.4f, onClick = {}) }
        rule.onNodeWithText("Push A")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo(1f, 0f..1f)))
    }

    @Test
    fun setRowAnnouncesItsState() {
        set {
            Column {
                SetRow("Set 1", "80 kg × 8", SetState.Done)
                SetRow("Set 2", "80 kg × 6", SetState.Upcoming)
            }
        }
        rule.onNodeWithText("80 kg × 8").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Done"))
        rule.onNodeWithText("80 kg × 6")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Not done yet"))
    }

    // Robolectric fakes text metrics (every text the same size), so the
    // baseline maths is tested as a function, not on a laid-out panel.
    @Test
    fun baselineShiftLinesUpCentredTexts() {
        // Number 48 high, baseline 38; unit 22 high, baseline 17. Centres at 0:
        // number baseline +14, unit baseline +6, so the unit moves down 8.
        assertEquals(8f, baselineShift(38f, 48f, 17f, 22f), 0.001f)
        assertEquals(0f, baselineShift(17f, 22f, 17f, 22f), 0.001f)
    }
}

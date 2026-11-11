package com.rukavina.gymbuddy.ui.workout.active

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import com.rukavina.gymbuddy.ui.workout.SummaryExercise
import com.rukavina.gymbuddy.ui.workout.WorkoutSet
import com.rukavina.gymbuddy.ui.workout.WorkoutSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** F24b: Save as template on the summary, then "Saved" + Edit. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w412dp-h915dp")
class WorkoutSummaryUiTest {

    @get:Rule val rule = createComposeRule()

    private fun summary(template: WorkoutTemplate?) = WorkoutSummary(
        title = "Legs",
        startedAt = 0L,
        endedAt = 60_000L,
        durationSeconds = 60L,
        sets = 1,
        lifted = 80f,
        units = PreferredUnits.METRIC,
        exercises = listOf(
            SummaryExercise("Squat", 1, ExerciseTrackingType.WEIGHT_REPS, WorkoutSet("s", 1, reps = "8", weight = "80", isCompleted = true)),
        ),
        offerBackup = false,
        template = template,
    )

    @Test
    fun `save then edit the template`() {
        var state by mutableStateOf(summary(WorkoutTemplate("tpl-1", "Legs", emptyList())))
        var edited: String? = null
        rule.setContent {
            GymBuddyTheme {
                WorkoutSummaryContent(
                    summary = state,
                    onDone = {},
                    onCreateAccount = {},
                    onNotNow = {},
                    onSaveTemplate = { state = state.copy(savedTemplateId = "tpl-1") },
                    onEditTemplate = { edited = it },
                )
            }
        }
        rule.onNodeWithText("Save as template").performScrollTo().performClick()
        rule.onNodeWithText("Saved to Workouts").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Edit").performClick()
        assertEquals("tpl-1", edited)
    }

    @Test
    fun `no offer after a template workout`() {
        rule.setContent {
            GymBuddyTheme { WorkoutSummaryContent(summary(null), {}, {}, {}) }
        }
        rule.onNodeWithText("Save as template").assertDoesNotExist()
    }
}

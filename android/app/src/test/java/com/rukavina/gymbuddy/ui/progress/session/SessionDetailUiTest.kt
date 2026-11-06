package com.rukavina.gymbuddy.ui.progress.session

import android.app.Application
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rukavina.gymbuddy.domain.progress.SessionDetail
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.testutil.ProgressFixtures.day
import com.rukavina.gymbuddy.testutil.ProgressFixtures.lift
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import com.rukavina.gymbuddy.ui.theme.GymBuddyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset

/** Session detail (Plan 7 step 09a, R14-1/2). */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SessionDetailUiTest {

    @get:Rule val rule = createComposeRule()

    private val session = day("s1", 0, lift("bench", 80f to 8, 82.5f to 6, name = "Bench Press"), title = "Push A")

    private fun state(template: WorkoutTemplate? = null) = SessionDetailUiState(
        loaded = true,
        session = session,
        tiles = SessionDetail.tiles(session),
        exercises = SessionDetail.exercises(session, listOf(session)),
        template = template,
        zone = ZoneOffset.UTC,
    )

    private fun show(
        state: SessionDetailUiState,
        running: Boolean = false,
        onExercise: (String) -> Unit = {},
        onDelete: (String, String) -> Unit = { _, _ -> },
        onEdit: (String) -> Unit = {},
    ) = rule.setContent {
        GymBuddyTheme { SessionDetailContent(state, null, running, {}, onExercise, {}, onEdit, onDelete) }
    }

    @Test
    fun cardOpensItsSetsAndThePageLink() {
        val opened = mutableListOf<String>()
        show(state(), onExercise = { opened += it })
        rule.onNodeWithText("2 sets · top 82.5 kg × 6").assertExists()
        rule.onAllNodesWithText("80 kg × 8").assertCountEquals(0)
        rule.onNodeWithText("Bench Press").performClick()
        rule.onNodeWithText("80 kg × 8").assertExists()
        rule.onNodeWithText("Bench Press page").performClick()
        assertEquals(listOf("bench"), opened)
    }

    @Test
    fun menuHidesStartAgainWithoutATemplateAndDeletes() {
        val deleted = mutableListOf<String>()
        show(state(), onDelete = { id, _ -> deleted += id })
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onAllNodesWithText("Start Push A again").assertCountEquals(0)
        rule.onNodeWithText("Delete").performClick()
        assertEquals(listOf("s1"), deleted)
    }

    @Test
    fun menuOffersStartAgainWithATemplate() {
        show(state(template("t1", title = "Push A")))
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Start Push A again").assertExists()
    }

    @Test
    fun menuOpensEditWorkout() {
        val edited = mutableListOf<String>()
        show(state(), onEdit = { edited += it })
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Edit workout").performClick()
        assertEquals(listOf("s1"), edited)
    }
}

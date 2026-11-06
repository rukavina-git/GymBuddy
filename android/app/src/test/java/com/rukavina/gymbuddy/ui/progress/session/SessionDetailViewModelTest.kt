package com.rukavina.gymbuddy.ui.progress.session

import androidx.lifecycle.SavedStateHandle
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.FakeWorkoutSessionRepository
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.ProgressFixtures.day
import com.rukavina.gymbuddy.testutil.ProgressFixtures.lift
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** Session detail (Plan 7 step 09a). */
class SessionDetailViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val push = day("s1", 0, lift("bench", 80f to 8), title = "Push A").copy(templateId = "t1")
    private val sources = FakeTemplateSources(sessions = listOf(push))

    private fun vm(id: String = "s1", exerciseId: String? = null) = SessionDetailViewModel(
        SavedStateHandle(mapOf(SessionDetailViewModel.ARG_SESSION_ID to id, SessionDetailViewModel.ARG_EXERCISE_ID to exerciseId)),
        sources,
    )

    @Test
    fun startAgainOnlyWithALiveTemplate() = runTest {
        assertNull(vm().uiState.first { it.loaded }.template)
        sources.templates.value = listOf(template("t1", title = "Push A"))
        assertEquals("t1", vm().uiState.first { it.loaded }.template?.id)
    }

    @Test
    fun showsTheSessionTilesAndCards() = runTest {
        val s = vm().uiState.first { it.loaded }
        assertEquals("s1", s.session?.id)
        assertEquals(listOf("bench"), s.exercises.map { it.performed.exerciseId })
        assertEquals(60 * 60, s.tiles?.seconds)
    }

    @Test
    fun goneSessionLoadsAsNull() = runTest {
        val s = vm(id = "nope").uiState.first { it.loaded }
        assertNull(s.session)
    }

    @Test
    fun highlightComesFromTheRoute() {
        assertEquals("bench", vm(exerciseId = "bench").highlightExerciseId)
        assertNull(vm().highlightExerciseId)
    }

    @Test
    fun deleteThenRestore() = runTest {
        val repo = FakeWorkoutSessionRepository(listOf(push))
        val deletes = SessionDeleteViewModel(repo)
        deletes.delete("s1")
        assertNull(repo.getWorkoutSessionById("s1"))
        deletes.restore("s1")
        assertEquals("s1", repo.getWorkoutSessionById("s1")?.id)
        assertEquals(listOf("s1"), repo.deleted)
        assertEquals(listOf("s1"), repo.restored)
    }
}

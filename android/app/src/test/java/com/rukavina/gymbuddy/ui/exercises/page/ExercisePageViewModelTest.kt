package com.rukavina.gymbuddy.ui.exercises.page

import androidx.lifecycle.SavedStateHandle
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import com.rukavina.gymbuddy.domain.usecase.template.StampTemplateExerciseSnapshotsUseCase
import com.rukavina.gymbuddy.domain.usecase.template.UpdateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.testutil.CountingIds
import com.rukavina.gymbuddy.testutil.testPreferencesDataStore
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.FakeWorkoutTemplateRepository
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import com.rukavina.gymbuddy.ui.template.editor.TemplateEditorViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExercisePageViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val bench = exercise("bench", "Barbell Bench Press").copy(source = EntitySource.DEFAULT)
    private val crunch = exercise("crunch", "Cable Crunch").copy(source = EntitySource.CUSTOM)
    private val exercises = FakeExerciseRepository(listOf(bench, crunch, exercise("e0", "Exercise 0"), exercise("e1", "Exercise 1")))

    private val abs = template("abs", "Abs Day").let { t -> t.copy(templateExercises = t.templateExercises + t.templateExercises[0].copy(id = "abs-c", exerciseId = "crunch", orderIndex = 2)) }
    private val starter = template("full", "Full Body", source = EntitySource.DEFAULT)
    private val templateRepo = FakeWorkoutTemplateRepository(listOf(abs, starter))
    private val sources = FakeTemplateSources(templates = listOf(abs, starter))
    private val preferences = AppPreferencesRepository(testPreferencesDataStore())

    private fun session(id: String, at: Long, kg: Float, reps: Int) = WorkoutSession(
        id = id, startedAt = at, endedAt = at + 1, durationSeconds = 1, title = id,
        performedExercises = listOf(
            PerformedExercise(
                "$id-pe", "bench", 0, "", ExerciseCategory.STRENGTH, ExerciseTrackingType.WEIGHT_REPS, emptyList(),
                listOf(WorkoutSet(id = "$id-s", weightKg = kg, reps = reps, isCompleted = true, orderIndex = 0)),
            ),
        ),
    )

    private fun vm(id: String) = ExercisePageViewModel(
        SavedStateHandle(mapOf(ExercisePageViewModel.ARG_EXERCISE_ID to id)),
        sources, exercises, templateRepo,
        UpdateWorkoutTemplateUseCase(templateRepo, StampTemplateExerciseSnapshotsUseCase(exercises)),
        CountingIds("te"),
        preferences,
    )

    @Test
    fun builtInPageShowsNumbersHistoryAndYourTemplates() = runTest {
        sources.sessions.value = listOf(session("a", 1, 90f, 3), session("b", 3, 87.5f, 5), session("c", 2, 85f, 5))
        val s = vm("bench").uiState.first { it.loaded }
        assertFalse(s.own)
        assertEquals(87.5f, s.history.last?.weightKg)
        assertEquals(90f, s.history.best?.weightKg)
        assertEquals(listOf("b", "c", "a"), s.history.entries.map { it.sessionId })
        assertEquals(listOf(TemplateChoice("abs", "Abs Day")), s.templates) // no starters
        assertEquals(TemplateEditorViewModel.DEFAULT_REST_SECONDS, s.restSeconds)
    }

    @Test
    fun neverDoneHasNoNumbers() = runTest {
        val s = vm("bench").uiState.first { it.loaded }
        assertNull(s.history.last)
        assertTrue(s.history.entries.isEmpty())
    }

    @Test
    fun restAndNoteAreYours() = runTest {
        val vm = vm("bench")
        vm.uiState.first { it.loaded }
        vm.setRest(180)
        assertEquals(180, vm.uiState.first { it.user.restSeconds != null }.restSeconds)
        vm.setRest(null) // Use my default
        assertNull(vm.uiState.first { it.user.restSeconds == null }.user.restSeconds)
        vm.setNote("  Notch 3 ")
        assertEquals("Notch 3", vm.uiState.first { it.user.note != null }.user.note)
        vm.setNote("   ")
        assertNull(vm.uiState.first { it.user.note == null }.user.note)
    }

    @Test
    fun ownExerciseDeleteNamesTemplatesAndLeaves() = runTest {
        val vm = vm("crunch")
        val s = vm.uiState.first { it.loaded }
        assertTrue(s.own)
        assertEquals(listOf("Abs Day"), s.usedIn)
        vm.delete()
        assertEquals(ExercisePageEvent.Deleted("Cable Crunch"), vm.events.first())
        assertNotNull(exercises.all.getValue("crunch").deletedAt)
        assertEquals(3, templateRepo.getTemplateById("abs")!!.templateExercises.size) // templates keep it
    }

    @Test
    fun builtInCannotBeDeleted() = runTest {
        val vm = vm("bench")
        vm.uiState.first { it.loaded }
        vm.delete()
        assertNull(exercises.all.getValue("bench").deletedAt)
    }

    @Test
    fun hideHidesAndSaysSo() = runTest {
        val vm = vm("bench")
        vm.uiState.first { it.loaded }
        vm.hide()
        assertEquals(ExercisePageEvent.Hidden("Barbell Bench Press"), vm.events.first())
        assertEquals(true, exercises.states.value["bench"]?.hidden)
    }

    @Test
    fun deletedElsewhereLeavesThePage() = runTest {
        val vm = vm("crunch")
        vm.uiState.first { it.loaded }
        exercises.deleteExercise("crunch")
        assertEquals(ExercisePageEvent.Gone, vm.events.first())
    }

    @Test
    fun addToTemplateAppendsWithDefaultsAndYourRest() = runTest {
        exercises.setExerciseRest("bench", 150)
        val vm = vm("bench")
        vm.uiState.first { it.loaded && it.user.restSeconds == 150 }
        vm.addToTemplate("abs")
        assertEquals(ExercisePageEvent.AddedTo("Abs Day"), vm.events.first())
        val added = templateRepo.updated.last().templateExercises.last()
        assertEquals("bench", added.exerciseId)
        assertEquals(3, added.orderIndex)
        assertEquals(TemplateEditorViewModel.DEFAULT_SETS, added.plannedSets)
        assertEquals(TemplateEditorViewModel.DEFAULT_REPS, added.plannedReps)
        assertEquals(150, added.restSeconds)
    }

    @Test
    fun writeFailureSaysSo() = runTest {
        val vm = vm("bench")
        vm.uiState.first { it.loaded }
        exercises.failWrites = true
        vm.setRest(60)
        assertEquals(ExercisePageEvent.Failed, vm.events.first())
    }

    @Test
    fun chartableLiftGetsProgressBlockAndShowOnProgress() = runTest {
        // 3 sessions over 3 weeks: bench can be charted; it is the only lift, so it is on Progress.
        sources.sessions.value = listOf(session("a", 0, 80f, 5), session("b", 7 * DAY, 82.5f, 5), session("c", 21 * DAY, 85f, 5))
        val vm = vm("bench")
        val s = vm.uiState.first { it.loaded }
        assertEquals(listOf("a", "b", "c"), s.progress?.points?.map { it.sessionId })
        assertTrue(s.onProgress)
        assertFalse(s.canTurnOff) // nothing else to show instead
    }

    @Test
    fun notChartableHasNoProgressBlock() = runTest {
        sources.sessions.value = listOf(session("a", 0, 80f, 5), session("b", 1, 82.5f, 5), session("c", 2, 85f, 5))
        val s = vm("bench").uiState.first { it.loaded }
        assertNull(s.progress) // three sessions, but not over 2 weeks
        assertFalse(s.onProgress)
    }

    @Test
    fun showOnProgressStoresThisLift() = runTest {
        sources.sessions.value = listOf(session("a", 0, 80f, 5), session("b", 7 * DAY, 82.5f, 5), session("c", 21 * DAY, 85f, 5))
        val vm = vm("bench")
        vm.uiState.first { it.loaded }
        vm.setShowOnProgress(true)
        assertEquals("bench", preferences.progressLiftId.first { it != null })
    }

    private companion object {
        const val DAY = 86_400_000L
    }
}

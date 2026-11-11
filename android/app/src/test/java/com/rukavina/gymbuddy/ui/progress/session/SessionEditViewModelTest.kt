package com.rukavina.gymbuddy.ui.progress.session

import androidx.lifecycle.SavedStateHandle
import com.rukavina.gymbuddy.domain.usecase.workout.UpdateWorkoutSessionUseCase
import com.rukavina.gymbuddy.domain.usecase.workout.ValidateWorkoutSessionSetsUseCase
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.FakeWorkoutSessionRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.ProgressFixtures.day
import com.rukavina.gymbuddy.testutil.ProgressFixtures.lift
import com.rukavina.gymbuddy.ui.design.KeypadInput
import com.rukavina.gymbuddy.ui.workout.SetField
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

/** Edit workout (Plan 7 step 09b). */
class SessionEditViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val bench = lift("bench", 80f to 8, 82.5f to 6, name = "Bench Press")
    private val push = day("s1", 10, bench, title = "Push A", minutes = 52).copy(notes = "Felt strong")
    private val repo = FakeWorkoutSessionRepository(listOf(push))
    private val exercises = FakeExerciseRepository(listOf(exercise("bench", name = "Bench Press")))

    private fun vm(id: String = "s1") = SessionEditViewModel(
        SavedStateHandle(mapOf(SessionDetailViewModel.ARG_SESSION_ID to id)),
        FakeTemplateSources(),
        repo,
        UpdateWorkoutSessionUseCase(repo, ValidateWorkoutSessionSetsUseCase(exercises)),
        FixedIdGenerator("new-set"),
    )

    private suspend fun SessionEditViewModel.loaded() = uiState.first { it.loaded }

    private fun SessionEditViewModel.type(vararg digits: Int) = digits.forEach { keypad(KeypadInput.Digit(it)) }

    @Test
    fun loadsTheSession() = runTest {
        val s = vm().loaded()
        assertEquals("Push A", s.title)
        assertEquals("Felt strong", s.note)
        assertEquals(52 * 60, s.durationSeconds)
        assertEquals(listOf("80", "82.5"), s.exercises.single().sets.map { it.weight })
        assertFalse(s.dirty)
    }

    @Test
    fun goneSessionCloses() = runTest {
        val vm = vm(id = "nope")
        assertEquals(SessionEditEvent.Close, vm.events.first())
    }

    @Test
    fun keypadCommitWalksToTheNextValue() = runTest {
        val vm = vm()
        vm.loaded()
        vm.openKeypad(bench.id, "bench-s0", SetField.Weight)
        vm.type(8, 5)
        vm.keypad(KeypadInput.Next)
        val s = vm.uiState.value
        assertEquals("85", s.exercises.single().sets.first().weight)
        assertEquals(SetField.Reps, s.keypad?.field)
        assertEquals("bench-s0", s.keypad?.setId)
        assertTrue(s.dirty)
        // Reps, then on to set 2's weight.
        vm.keypad(KeypadInput.Next)
        assertEquals("bench-s1" to SetField.Weight, vm.uiState.value.keypad?.let { it.setId to it.field })
    }

    @Test
    fun keypadBlocksZeroReps() = runTest {
        val vm = vm()
        vm.loaded()
        vm.openKeypad(bench.id, "bench-s0", SetField.Reps)
        vm.type(0)
        vm.keypad(KeypadInput.Next)
        assertEquals("8", vm.uiState.value.exercises.single().sets.first().reps)
        assertEquals(SetField.Reps, vm.uiState.value.keypad?.field)
    }

    @Test
    fun addSetCopiesTheLast() = runTest {
        val vm = vm()
        vm.loaded()
        vm.addSet(bench.id)
        val sets = vm.uiState.value.exercises.single().sets
        assertEquals(3, sets.size)
        assertEquals("new-set", sets.last().id)
        assertEquals(3, sets.last().setNumber)
        assertEquals("82.5" to "6", sets.last().weight to sets.last().reps)
    }

    @Test
    fun deleteSetThenUndo() = runTest {
        val vm = vm()
        vm.loaded()
        vm.deleteSet(bench.id, "bench-s0")
        assertEquals(SessionEditEvent.SetDeleted(bench.id, "bench-s0"), vm.events.first())
        assertEquals(listOf("bench-s1" to 1), vm.uiState.value.exercises.single().sets.map { it.id to it.setNumber })
        assertTrue(vm.uiState.value.dirty)
        vm.undoDelete("bench-s0")
        assertEquals(listOf("bench-s0", "bench-s1"), vm.uiState.value.exercises.single().sets.map { it.id })
        assertFalse(vm.uiState.value.dirty)
    }

    @Test
    fun timeChangesSetTheDuration() = runTest {
        val vm = vm()
        val s = vm.loaded()
        val start = Instant.ofEpochMilli(s.startedAt).atZone(s.zone)
        vm.setEnd(start.toLocalTime().plusMinutes(75))
        assertEquals(75 * 60, vm.uiState.value.durationSeconds)
        vm.setStart(start.toLocalTime().plusMinutes(15))
        assertEquals(60 * 60, vm.uiState.value.durationSeconds)
        // An end before the start runs past midnight.
        vm.setEnd(start.toLocalTime().plusMinutes(5))
        assertEquals(24 * 60 * 60 - 10 * 60, vm.uiState.value.durationSeconds)
    }

    @Test
    fun dateChangeKeepsTheTimes() = runTest {
        val vm = vm()
        val s = vm.loaded()
        val start = Instant.ofEpochMilli(s.startedAt).atZone(s.zone)
        vm.setDate(start.toLocalDate().minusDays(2))
        val after = vm.uiState.value
        assertEquals(start.minusDays(2).toInstant().toEpochMilli(), after.startedAt)
        assertEquals(s.durationSeconds, after.durationSeconds)
    }

    @Test
    fun saveUpdatesTheSession() = runTest {
        val vm = vm()
        val s = vm.loaded()
        vm.setTitle("Push B ")
        vm.setNote("")
        vm.openKeypad(bench.id, "bench-s1", SetField.Weight)
        vm.type(9, 0)
        vm.keypad(KeypadInput.Cancel)
        vm.openKeypad(bench.id, "bench-s1", SetField.Weight)
        vm.type(9, 0)
        vm.keypad(KeypadInput.Next)
        vm.keypad(KeypadInput.Cancel)
        vm.setEnd(LocalTime.from(Instant.ofEpochMilli(s.startedAt).atZone(s.zone)).plusMinutes(30))
        vm.save()
        assertEquals(SessionEditEvent.Close, vm.events.first())
        val saved = repo.updated.single()
        assertEquals("Push B", saved.title)
        assertNull(saved.notes)
        assertEquals(30 * 60, saved.durationSeconds)
        val sets = saved.performedExercises.single().sets
        assertEquals(listOf(80f, 90f), sets.map { it.weightKg })
        // The untouched set is the stored one.
        assertEquals(bench.sets[0], sets[0])
    }

    @Test
    fun exerciseWithNoSetsIsDroppedOnSave() = runTest {
        val two = push.copy(performedExercises = listOf(bench, lift("row", 60f to 10, order = 1)))
        val repo = FakeWorkoutSessionRepository(listOf(two))
        val ex = FakeExerciseRepository(listOf(exercise("bench"), exercise("row")))
        val vm = SessionEditViewModel(
            SavedStateHandle(mapOf(SessionDetailViewModel.ARG_SESSION_ID to "s1")),
            FakeTemplateSources(), repo,
            UpdateWorkoutSessionUseCase(repo, ValidateWorkoutSessionSetsUseCase(ex)),
            FixedIdGenerator(),
        )
        vm.loaded()
        vm.deleteSet("row-1-1", "row-s0")
        vm.save()
        vm.events.first { it == SessionEditEvent.Close }
        assertEquals(listOf("bench"), repo.updated.single().performedExercises.map { it.exerciseId })
    }

    @Test
    fun blankNameBlocksSave() = runTest {
        val vm = vm()
        vm.loaded()
        vm.setTitle(" ")
        vm.save()
        assertTrue(vm.uiState.value.nameError)
        assertTrue(repo.updated.isEmpty())
    }

    @Test
    fun closeWithChangesAsksFirst() = runTest {
        val vm = vm()
        vm.loaded()
        vm.requestClose()
        assertEquals(SessionEditEvent.Close, vm.events.first())
        vm.setTitle("Other")
        vm.requestClose()
        assertTrue(vm.uiState.value.confirmDiscard)
        vm.keepEditing()
        assertFalse(vm.uiState.value.confirmDiscard)
        vm.setTitle("Push A")
        assertFalse(vm.uiState.value.dirty)
    }
}

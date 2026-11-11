package com.rukavina.gymbuddy.ui.exercises

import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.exerciseCreator
import com.rukavina.gymbuddy.ui.exercises.create.ExerciseForm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ExercisesViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = FakeExerciseRepository(
        listOf(
            exercise("bench", "Barbell Bench Press"),
            exercise("row", "Barbell Row", muscle = MuscleGroup.BACK),
            exercise("fly", "Cable Fly"),
            exercise("old", "Old Press").copy(deprecated = true),
        ),
    )

    private fun vm() = ExercisesViewModel(repo, exerciseCreator(repo))

    @Test
    fun listLeavesOutHiddenDeprecatedAndDeleted() = runTest {
        repo.hideExercise("fly")
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(listOf("bench", "row"), vm.uiState.first { it.loaded }.rows.map { it.id })
    }

    @Test
    fun searchForgivesTyposAndChipsFilterByMainMuscle() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.setQuery("bnch")
        assertEquals(listOf("bench"), vm.uiState.value.rows.map { it.id })
        vm.setQuery("")
        vm.setMuscle(MuscleGroup.BACK)
        assertEquals(listOf("row"), vm.uiState.value.rows.map { it.id })
        assertEquals("", vm.query.value)
    }

    @Test
    fun quickCreateWritesYourOwnAndTheSearchFindsIt() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.setQuery("zercher squat")
        assertEquals(emptyList<String>(), vm.uiState.value.rows.map { it.id })
        vm.quickCreate(ExerciseForm(name = "Zercher Squat", mainMuscle = MuscleGroup.LEGS))
        assertEquals(ExercisesEvent.Created("Zercher Squat"), vm.events.first())
        val made = repo.all.values.single { it.name == "Zercher Squat" }
        assertEquals(EntitySource.CUSTOM, made.source)
        assertEquals(listOf(MuscleGroup.LEGS), made.primaryMuscles)
        assertEquals(listOf(made.id), vm.uiState.value.rows.map { it.id })
        assertFalse(vm.creating.value)
    }

    @Test
    fun quickCreateFailureSaysSo() = runTest {
        repo.failWrites = true
        val vm = vm()
        vm.quickCreate(ExerciseForm(name = "X"))
        assertEquals(ExercisesEvent.CreateFailed, vm.events.first())
        assertFalse(vm.creating.value)
    }
}

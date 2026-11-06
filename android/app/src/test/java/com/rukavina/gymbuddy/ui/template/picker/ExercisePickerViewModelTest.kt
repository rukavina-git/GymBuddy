package com.rukavina.gymbuddy.ui.template.picker

import androidx.lifecycle.SavedStateHandle
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.exerciseCreator
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExercisePickerViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val repo = FakeExerciseRepository(
        listOf(
            exercise("bench", "Barbell Bench Press"),
            exercise("db", "Dumbbell Bench Press"),
            exercise("fly", "Cable Crossover"),
            exercise("row", "Barbell Row", muscle = MuscleGroup.BACK),
            exercise("old", "Old Press").copy(deprecated = true),
        ),
    )

    private fun vm() = ExercisePickerViewModel(
        SavedStateHandle(mapOf(ExercisePickerViewModel.ARG_IN_TEMPLATE to "bench,row")), repo, exerciseCreator(repo),
    )

    @Test
    fun picksAreNumberedInOrderAndSurviveSearch() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.toggle("fly")
        vm.toggle("bench")
        vm.toggle("row")
        vm.setQuery("bnch")
        val rows = vm.uiState.value.rows
        assertEquals(listOf("bench", "db"), rows.map { it.exercise.id })
        assertEquals(listOf(2, null), rows.map { it.pickNumber })
        assertEquals(3, vm.uiState.value.pickCount)
        vm.toggle("fly") // unpick: the rest move up
        assertEquals(listOf("bench", "row"), vm.picked)
        vm.setQuery("")
        assertEquals(1, vm.uiState.value.rows.first { it.exercise.id == "bench" }.pickNumber)
    }

    @Test
    fun inTemplateRowsStayPickableAndMuscleFilters() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(
            listOf("bench" to true, "row" to true, "fly" to false, "db" to false),
            vm.uiState.value.rows.map { it.exercise.id to it.inTemplate },
        )
        vm.toggle("bench")
        assertEquals(listOf("bench"), vm.picked)
        vm.setMuscle(MuscleGroup.BACK)
        assertEquals(listOf("row"), vm.uiState.value.rows.map { it.exercise.id })
    }

    @Test
    fun exactMatchesComeBeforeTypos() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.setQuery("barbel")
        assertEquals(listOf("bench", "row"), vm.uiState.value.rows.map { it.exercise.id })
        vm.setQuery("cros")
        assertEquals(listOf("fly"), vm.uiState.value.rows.map { it.exercise.id })
    }

    @Test
    fun quickCreateFromEmptySearchPicksTheNewExercise() = runTest {
        val vm = vm()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.toggle("fly")
        vm.setQuery("zercher squat")
        assertTrue(vm.uiState.value.rows.isEmpty())
        vm.quickCreate(com.rukavina.gymbuddy.ui.exercises.create.ExerciseForm(name = "Zercher Squat"))
        assertEquals(true, vm.created.first())
        val id = repo.all.values.single { it.name == "Zercher Squat" }.id
        assertEquals(listOf("fly", id), vm.picked)
        assertEquals(2, vm.uiState.value.rows.single().pickNumber)
    }
}

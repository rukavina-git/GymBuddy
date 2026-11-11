package com.rukavina.gymbuddy.ui.you

import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeWorkoutTemplateRepository
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** You › Hidden templates / Hidden exercises: list and unhide. */
class HiddenViewModelsTest {

    @get:Rule val main = MainDispatcherRule()

    @Test
    fun templates_unhide() = runTest {
        val repo = FakeWorkoutTemplateRepository(listOf(template("a"), template("b")))
        repo.hideTemplate("a")
        val vm = HiddenTemplatesViewModel(repo)
        assertEquals(listOf("a"), vm.hiddenTemplates.first().map { it.id })
        vm.unhideTemplate("a")
        assertTrue(repo.getHiddenTemplates().first().isEmpty())
    }

    @Test
    fun templates_unhideFailureIsSwallowed() = runTest {
        val repo = object : com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository by FakeWorkoutTemplateRepository() {
            override suspend fun unhideTemplate(id: String) = throw IllegalStateException("boom")
        }
        HiddenTemplatesViewModel(repo).unhideTemplate("x") // no crash
    }

    @Test
    fun exercises_unhideOneAndAll() = runTest {
        val repo = FakeExerciseRepository(listOf(exercise("a"), exercise("b"), exercise("c")))
        listOf("a", "b", "c").forEach { repo.hideExercise(it) }
        val vm = HiddenExercisesViewModel(repo)
        assertEquals(3, vm.hiddenExercises.first().size)
        vm.unhideExercise("a")
        assertEquals(setOf("b", "c"), vm.hiddenExercises.first().map { it.id }.toSet())
        vm.unhideAllExercises()
        assertTrue(vm.hiddenExercises.first().isEmpty())
    }

    @Test
    fun exercises_failureIsSwallowed() = runTest {
        val repo = FakeExerciseRepository(listOf(exercise("a"))).apply { failWrites = true }
        val vm = HiddenExercisesViewModel(repo)
        vm.unhideExercise("a")
        vm.unhideAllExercises()
    }
}

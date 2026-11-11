package com.rukavina.gymbuddy.ui.exercises.create

import androidx.lifecycle.SavedStateHandle
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.exerciseCreator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExerciseEditViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val mine = exercise("crunch", "Cable Crunch", muscle = MuscleGroup.CORE, equipment = Equipment.CABLE).copy(source = EntitySource.CUSTOM)
    private val builtIn = exercise("bench", "Bench").copy(source = EntitySource.DEFAULT)
    private val repo = FakeExerciseRepository(listOf(mine, builtIn))

    private fun vm(id: String? = null) = ExerciseEditViewModel(
        SavedStateHandle(if (id == null) emptyMap() else mapOf(ExerciseEditViewModel.ARG_EXERCISE_ID to id)),
        repo, exerciseCreator(repo), FakeTemplateSources(),
    )

    @Test
    fun saveNeedsANameThenCreatesWithRest() = runTest {
        val vm = vm()
        assertFalse(vm.uiState.value.canSave)
        vm.setName("x")
        vm.setName("")
        assertTrue(vm.uiState.value.nameError)
        vm.save()
        assertTrue(repo.all.size == 2)

        vm.setName("Sled Push")
        vm.setTracking(ExerciseTrackingType.DISTANCE_DURATION)
        vm.setMainMuscle(MuscleGroup.LEGS)
        vm.toggleEquipment(Equipment.MACHINE)
        vm.setRest(90)
        vm.save()
        val event = vm.events.first() as ExerciseEditEvent.Saved
        assertTrue(event.isNew)
        val made = repo.all.getValue(event.exerciseId)
        assertEquals("Sled Push", made.name)
        assertEquals(EntitySource.CUSTOM, made.source)
        assertEquals(listOf(Equipment.MACHINE), made.equipmentNeeded)
        assertEquals(90, repo.states.value[made.id]?.restSeconds)
    }

    @Test
    fun editLoadsYourExerciseAndKeepsItsId() = runTest {
        repo.setExerciseRest("crunch", 60)
        val vm = vm("crunch")
        val s = vm.uiState.first { it.loaded }
        assertFalse(s.isNew)
        assertEquals("Cable Crunch", s.form.name)
        assertEquals(60, s.form.restSeconds)
        vm.setName("Kneeling Cable Crunch")
        vm.save()
        assertEquals(ExerciseEditEvent.Saved("crunch", isNew = false), vm.events.first())
        assertEquals("Kneeling Cable Crunch", repo.all.getValue("crunch").name)
        assertEquals(60, repo.states.value["crunch"]?.restSeconds)
    }

    @Test
    fun builtInCannotBeEdited() = runTest {
        assertEquals(ExerciseEditEvent.Close, vm("bench").events.first())
    }

    @Test
    fun mainMuscleLeavesOtherMuscles() = runTest {
        val vm = vm()
        vm.toggleOtherMuscle(MuscleGroup.CORE)
        vm.setMainMuscle(MuscleGroup.CORE)
        assertEquals(emptySet<MuscleGroup>(), vm.uiState.value.form.otherMuscles)
    }

    @Test
    fun saveFailureSaysSo() = runTest {
        repo.failWrites = true
        val vm = vm()
        vm.setName("X")
        vm.save()
        assertEquals(ExerciseEditEvent.Failed, vm.events.first())
        assertFalse(vm.uiState.value.saving)
    }
}

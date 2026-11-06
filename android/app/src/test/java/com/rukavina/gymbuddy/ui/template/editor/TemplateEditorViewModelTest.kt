package com.rukavina.gymbuddy.ui.template.editor

import androidx.lifecycle.SavedStateHandle
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PreferredUnits
import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.domain.usecase.template.CreateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.domain.usecase.template.StampTemplateExerciseSnapshotsUseCase
import com.rukavina.gymbuddy.domain.usecase.template.UpdateWorkoutTemplateUseCase
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import com.rukavina.gymbuddy.testutil.FakeExerciseRepository
import com.rukavina.gymbuddy.testutil.FakeTemplateSources
import com.rukavina.gymbuddy.testutil.FakeWorkoutTemplateRepository
import com.rukavina.gymbuddy.testutil.FixedIdGenerator
import com.rukavina.gymbuddy.testutil.MainDispatcherRule
import com.rukavina.gymbuddy.testutil.TemplateFixtures.template
import com.rukavina.gymbuddy.ui.design.KeypadInput
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TemplateEditorViewModelTest {

    @get:Rule val main = MainDispatcherRule()

    private val exercises = FakeExerciseRepository(
        listOf(
            exercise("e0", "Exercise 0"), exercise("e1", "Exercise 1"),
            exercise("fly", "Cable Fly"), exercise("dip", "Dip", trackingType = ExerciseTrackingType.REPS_ONLY),
            exercise("plank", "Plank", trackingType = ExerciseTrackingType.DURATION),
        ),
    )
    private val existing = template("push", "Push A").let { t ->
        t.copy(templateExercises = t.templateExercises.mapIndexed { i, e -> e.copy(plannedWeightKg = 80f + i, notes = if (i == 0) "Pins 4" else null) })
    }
    private val templates = FakeWorkoutTemplateRepository(listOf(existing))
    private val sources = FakeTemplateSources()
    private val ids = FixedIdGenerator((1..20).map { "new-$it" })

    private fun vm(id: String? = null) = StampTemplateExerciseSnapshotsUseCase(exercises).let { stamp ->
        TemplateEditorViewModel(
            SavedStateHandle(if (id == null) emptyMap() else mapOf(TemplateEditorViewModel.ARG_TEMPLATE_ID to id)),
            sources, templates, exercises,
            CreateWorkoutTemplateUseCase(templates, ids, stamp),
            UpdateWorkoutTemplateUseCase(templates, stamp),
            ids,
        )
    }

    private val TemplateEditorViewModel.s get() = uiState.value
    private fun TemplateEditorViewModel.keys() = s.exercises.map { it.key }
    private fun TemplateEditorViewModel.names() = s.exercises.map { it.name }

    @Test
    fun loadsExistingInOrderAndIsClean() {
        val vm = vm("push")
        assertTrue(vm.s.loaded)
        assertFalse(vm.s.isNew)
        assertEquals("Push A", vm.s.name)
        assertEquals(listOf("Exercise 0", "Exercise 1"), vm.names())
        assertFalse(vm.s.dirty)
    }

    @Test
    fun starterOrMissingCloses() = runTest {
        templates.createTemplate(template("full", source = EntitySource.DEFAULT))
        assertEquals(TemplateEditorEvent.Close, vm("full").events.first())
        assertEquals(TemplateEditorEvent.Close, vm("nope").events.first())
    }

    @Test
    fun addAppendsInPickOrderWithDefaults() = runTest {
        val vm = vm("push")
        vm.add(listOf("plank", "fly", "missing", "dip"))
        assertEquals(TemplateEditorEvent.Added(3), vm.events.first())
        assertEquals(listOf("Exercise 0", "Exercise 1", "Plank", "Cable Fly", "Dip"), vm.names())
        val (plank, fly, dip) = vm.s.exercises.drop(2)
        assertEquals(3, fly.sets)
        assertEquals(10, fly.reps)
        assertNull(fly.weightKg)
        assertEquals(TemplateEditorViewModel.DEFAULT_REST_SECONDS, fly.restSeconds)
        assertEquals(10, dip.reps)
        assertNull(plank.reps)
        assertNull(fly.existingId)
        assertTrue(vm.s.dirty)
    }

    @Test
    fun removeThenUndoPutsItBackInPlace() = runTest {
        val vm = vm("push")
        val first = vm.keys()[0]
        vm.remove(first)
        assertEquals(TemplateEditorEvent.Removed(first, "Exercise 0"), vm.events.first())
        assertEquals(listOf("Exercise 1"), vm.names())
        assertTrue(vm.s.dirty)
        vm.undoRemove(first)
        assertEquals(listOf("Exercise 0", "Exercise 1"), vm.names())
        assertFalse(vm.s.dirty)
        vm.undoRemove(first) // only once
        assertEquals(2, vm.s.exercises.size)
    }

    @Test
    fun reorderByDragAndMoveUpDown() = runTest {
        val vm = vm("push")
        vm.add(listOf("fly"))
        val (a, b, c) = vm.keys()
        vm.move(c, 0)
        assertEquals(listOf(c, a, b), vm.keys())
        vm.moveBy(c, 1)
        assertEquals(listOf(a, c, b), vm.keys())
        vm.moveBy(a, -1) // already first
        assertEquals(listOf(a, c, b), vm.keys())
        vm.moveBy(b, 1) // already last
        assertEquals(listOf(a, c, b), vm.keys())
    }

    @Test
    fun emptyNameBlocksSaveWithMessage() {
        val vm = vm("push")
        vm.setName("  ")
        vm.save()
        assertTrue(vm.s.nameError)
        assertTrue(templates.updated.isEmpty())
        vm.setName("Push B")
        assertFalse(vm.s.nameError)
    }

    @Test
    fun saveWithoutExercisesIsOff() {
        val vm = vm()
        vm.setName("Legs")
        assertFalse(vm.s.canSave)
        vm.save()
        assertTrue(templates.created.isEmpty())
    }

    @Test
    fun discardAsksOnlyWithChanges() = runTest {
        val vm = vm("push")
        vm.requestClose()
        assertFalse(vm.s.confirmDiscard)
        assertEquals(TemplateEditorEvent.Close, vm.events.first())

        vm.setName("Push B")
        vm.requestClose()
        assertTrue(vm.s.confirmDiscard)
        vm.keepEditing()
        assertFalse(vm.s.confirmDiscard)
        vm.setName("Push A ") // back to the saved name: no change
        assertFalse(vm.s.dirty)
        vm.setNote(vm.keys()[1], "  ")
        assertFalse(vm.s.dirty)
        vm.setNote(vm.keys()[1], "slow")
        vm.requestClose()
        vm.discard()
        assertEquals(TemplateEditorEvent.Close, vm.events.first())
        assertTrue(templates.updated.isEmpty())
    }

    @Test
    fun keypadNextWalksChipsThenRestThenNextCard() {
        val vm = vm("push")
        val (a, b) = vm.keys()
        vm.openKeypad(KeypadTarget(a, EditField.Reps))
        assertEquals("10", vm.s.keypad?.text)
        vm.keypad(KeypadInput.Digit(6))
        vm.keypad(KeypadInput.Next)
        assertEquals(KeypadTarget(a, EditField.Weight), vm.s.keypad?.target)
        vm.keypad(KeypadInput.Next) // untouched: kept
        assertEquals(KeypadTarget(a, EditField.Rest), vm.s.keypad?.target)
        vm.keypad(KeypadInput.Next)
        assertEquals(KeypadTarget(b, EditField.Sets), vm.s.keypad?.target)
        vm.openKeypad(KeypadTarget(b, EditField.Rest))
        assertNull(vm.nextTarget(KeypadTarget(b, EditField.Rest)))
        vm.keypad(KeypadInput.Next)
        assertNull(vm.s.keypad)
        assertEquals(6, vm.s.exercises[0].reps)
        assertEquals(80f, vm.s.exercises[0].weightKg)
    }

    @Test
    fun keypadCommitsParsedValuesAndCancelKeepsOld() {
        val vm = vm("push")
        val a = vm.keys()[0]
        fun enter(field: EditField, vararg keys: KeypadInput) {
            vm.openKeypad(KeypadTarget(a, field))
            keys.forEach { vm.keypad(it, separator = ',') }
            vm.keypad(KeypadInput.Next)
        }
        enter(EditField.Weight, KeypadInput.Digit(8), KeypadInput.Digit(2), KeypadInput.Decimal, KeypadInput.Digit(5))
        assertEquals(82.5f, vm.s.exercises[0].weightKg)
        enter(EditField.Rest, KeypadInput.Digit(1), KeypadInput.Digit(3), KeypadInput.Digit(0))
        assertEquals(90, vm.s.exercises[0].restSeconds)
        enter(EditField.Sets, KeypadInput.Digit(0)) // 0 sets: keep 3
        assertEquals(3, vm.s.exercises[0].sets)
        enter(EditField.Reps, KeypadInput.Backspace) // cleared: no target
        assertNull(vm.s.exercises[0].reps)

        vm.openKeypad(KeypadTarget(a, EditField.Sets))
        vm.keypad(KeypadInput.Digit(5))
        vm.keypad(KeypadInput.Cancel)
        assertNull(vm.s.keypad)
        assertEquals(3, vm.s.exercises[0].sets)
    }

    @Test
    fun imperialWeightIsStoredInKg() {
        sources.units.value = PreferredUnits.IMPERIAL
        val vm = vm("push")
        val a = vm.keys()[0]
        vm.openKeypad(KeypadTarget(a, EditField.Weight))
        listOf(1, 0, 0).forEach { vm.keypad(KeypadInput.Digit(it)) }
        vm.keypad(KeypadInput.Next)
        assertEquals(45.36f, vm.s.exercises[0].weightKg!!, 0.01f)
    }

    @Test
    fun saveExistingKeepsIdsAndMintsNewOnes() = runTest {
        val vm = vm("push")
        vm.add(listOf("fly"))
        vm.events.first()
        vm.move(vm.keys()[2], 0)
        vm.setName(" Push B ")
        vm.save()
        assertEquals(TemplateEditorEvent.Close, vm.events.first())
        val saved: WorkoutTemplate = templates.updated.single()
        assertEquals("push", saved.id)
        assertEquals("Push B", saved.title)
        assertEquals(listOf("new-1", "push-ex0", "push-ex1"), saved.templateExercises.map { it.id })
        assertEquals(listOf(0, 1, 2), saved.templateExercises.map { it.orderIndex })
        assertEquals(80f, saved.templateExercises[1].plannedWeightKg)
        assertEquals("Pins 4", saved.templateExercises[1].notes)
        assertFalse(vm.s.dirty)
    }

    @Test
    fun saveNewCreatesCustom() = runTest {
        val vm = vm()
        vm.setName("Legs")
        vm.add(listOf("e0"))
        vm.events.first()
        vm.save()
        assertEquals(TemplateEditorEvent.Close, vm.events.first())
        val created = templates.created.single()
        assertEquals(EntitySource.CUSTOM, created.source)
        assertEquals("Legs", created.title)
        assertEquals("Exercise 0", created.templateExercises.single().exerciseName)
    }

    @Test
    fun saveFailureKeepsEditing() = runTest {
        val vm = vm("push")
        exercises.purge("e0") // stamping can't find it: the use case fails
        vm.setName("Push B")
        vm.save()
        assertEquals(TemplateEditorEvent.SaveFailed, vm.events.first())
        assertFalse(vm.s.saving)
        assertTrue(vm.s.dirty)
    }

    // 7.7: deleted exercise row (R13-12) and your rest on add

    @Test
    fun deletedExerciseIsMarkedAndSaveStillWorks() = runTest {
        exercises.deleteExercise("e0")
        val vm = vm("push")
        vm.uiState.first { it.loaded }
        assertEquals(listOf(true, false), vm.s.exercises.map { it.deleted })
        vm.setName("Push B")
        vm.save()
        assertEquals(TemplateEditorEvent.Close, vm.events.first())
        assertEquals(listOf("e0", "e1"), templates.updated.last().templateExercises.map { it.exerciseId })
    }

    @Test
    fun restoreBringsTheExerciseBackAndClearsTheRow() = runTest {
        exercises.deleteExercise("e0")
        val vm = vm("push")
        vm.uiState.first { it.loaded }
        vm.restore(vm.s.exercises.first().key)
        vm.uiState.first { s -> s.exercises.none { it.deleted } }
        assertNull(exercises.all.getValue("e0").deletedAt)
        assertFalse(vm.s.dirty)
    }

    @Test
    fun swapReplacesTheDeletedCardInPlace() = runTest {
        exercises.deleteExercise("e0")
        val vm = vm("push")
        vm.uiState.first { it.loaded }
        val key = vm.s.exercises.first().key
        vm.startSwap(key)
        vm.onPicked(listOf("fly", "dip"))
        vm.uiState.first { it.exercises.first().exerciseId == "fly" }
        val card = vm.s.exercises.first()
        assertEquals(key, card.key)
        assertEquals("Cable Fly", card.name)
        assertFalse(card.deleted)
        assertEquals(80f, card.weightKg) // same tracking type keeps the targets
        assertEquals(listOf("fly", "e1"), vm.s.exercises.map { it.exerciseId })
    }

    @Test
    fun plainAddAfterSwapIsNotASwap() = runTest {
        val vm = vm("push")
        vm.uiState.first { it.loaded }
        vm.startSwap(vm.s.exercises.first().key)
        vm.startSwap(null)
        vm.onPicked(listOf("fly"))
        vm.uiState.first { it.exercises.size == 3 }
        assertEquals(listOf("e0", "e1", "fly"), vm.s.exercises.map { it.exerciseId })
    }

    @Test
    fun addUsesYourRestForTheExercise() = runTest {
        exercises.setExerciseRest("fly", 75)
        val vm = vm()
        vm.uiState.first { it.loaded }
        vm.add(listOf("fly", "dip"))
        vm.uiState.first { it.exercises.size == 2 }
        assertEquals(listOf(75, TemplateEditorViewModel.DEFAULT_REST_SECONDS), vm.s.exercises.map { it.restSeconds })
    }
}

package com.rukavina.gymbuddy.ui.exercises.create

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import com.rukavina.gymbuddy.testutil.ExerciseFixtures.exercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseFormTest {

    @Test
    fun newExerciseGetsDefaultsForHiddenFields() {
        val e = ExerciseForm(name = "  Cable Crunch ").toExercise("id1")
        assertEquals("id1", e.id)
        assertEquals("Cable Crunch", e.name)
        assertEquals(ExerciseTrackingType.WEIGHT_REPS, e.trackingType)
        assertEquals(listOf(MuscleGroup.OTHER), e.primaryMuscles) // none picked
        assertEquals(DifficultyLevel.BEGINNER, e.difficulty)
        assertEquals(ExerciseType.COMPOUND, e.exerciseType)
        assertEquals(ExerciseCategory.STRENGTH, e.category)
        assertEquals(EntitySource.CUSTOM, e.source)
    }

    @Test
    fun stepsSplitByLineAndOtherMusclesSkipMain() {
        val e = ExerciseForm(
            name = "Crunch",
            mainMuscle = MuscleGroup.CORE,
            otherMuscles = setOf(MuscleGroup.CORE, MuscleGroup.SHOULDERS, MuscleGroup.ARMS),
            equipment = setOf(Equipment.CABLE, Equipment.BARBELL),
            steps = "Kneel\n\n  Crunch down \n",
        ).toExercise("x")
        assertEquals(listOf("Kneel", "Crunch down"), e.instructions)
        assertEquals(listOf(MuscleGroup.SHOULDERS, MuscleGroup.ARMS), e.secondaryMuscles)
        assertEquals(listOf(Equipment.BARBELL, Equipment.CABLE), e.equipmentNeeded)
    }

    @Test
    fun distanceAndTimeIsCardioButAnEditKeepsItsCategory() {
        assertEquals(ExerciseCategory.CARDIO, ExerciseForm(name = "Run", tracking = ExerciseTrackingType.DISTANCE_DURATION).toExercise("r").category)
        val base = exercise("b", "Sled").copy(category = ExerciseCategory.STRONGMAN, tips = listOf("Low"), source = EntitySource.CUSTOM)
        val edited = ExerciseForm.of(base, 90).copy(tracking = ExerciseTrackingType.DISTANCE_DURATION).toExercise("b", base)
        assertEquals(ExerciseCategory.STRONGMAN, edited.category)
        assertEquals(listOf("Low"), edited.tips) // fields the form doesn't show stay
    }

    @Test
    fun ofRoundTripsTheFormFields() {
        val base = exercise("b", "Press", muscle = MuscleGroup.SHOULDERS).copy(instructions = listOf("Up", "Down"))
        val form = ExerciseForm.of(base, 75)
        assertEquals("Up\nDown", form.steps)
        assertEquals(75, form.restSeconds)
        assertEquals(MuscleGroup.SHOULDERS, form.mainMuscle)
        assertEquals(base.copy(), form.toExercise("b", base))
    }

    @Test
    fun nameIsTheOnlyRule() {
        assertFalse(ExerciseForm(name = "   ").canSave)
        assertTrue(ExerciseForm(name = "A").canSave)
    }

    @Test
    fun titleCaseCapitalisesWords() {
        assertEquals("Zercher Squat", titleCase("  zercher   squat "))
        assertEquals("RDL Pause", titleCase("RDL pause"))
    }
}

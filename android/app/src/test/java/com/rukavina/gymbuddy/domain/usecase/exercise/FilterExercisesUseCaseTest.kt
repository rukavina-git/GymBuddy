package com.rukavina.gymbuddy.domain.usecase.exercise

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterExercisesUseCaseTest {

    private fun exercise(
        id: String,
        name: String,
        description: String? = null,
        primaryMuscles: List<MuscleGroup> = emptyList(),
        secondaryMuscles: List<MuscleGroup> = emptyList(),
        equipmentNeeded: List<Equipment> = emptyList()
    ) = Exercise(
        id = id,
        name = name,
        description = description,
        primaryMuscles = primaryMuscles,
        secondaryMuscles = secondaryMuscles,
        difficulty = DifficultyLevel.INTERMEDIATE,
        equipmentNeeded = equipmentNeeded,
        category = ExerciseCategory.STRENGTH,
        exerciseType = ExerciseType.COMPOUND,
        trackingType = ExerciseTrackingType.WEIGHT_REPS
    )

    private val benchPress = exercise(
        id = "1",
        name = "Bench Press",
        description = "A classic chest builder",
        primaryMuscles = listOf(MuscleGroup.CHEST),
        secondaryMuscles = listOf(MuscleGroup.ARMS),
        equipmentNeeded = listOf(Equipment.BARBELL, Equipment.BENCH)
    )
    private val squat = exercise(
        id = "2",
        name = "Barbell Squat",
        description = "Lower body compound movement",
        primaryMuscles = listOf(MuscleGroup.LEGS),
        equipmentNeeded = listOf(Equipment.BARBELL)
    )
    private val pushUp = exercise(
        id = "3",
        name = "Push Up",
        description = "Bodyweight chest exercise",
        primaryMuscles = listOf(MuscleGroup.CHEST),
        secondaryMuscles = listOf(MuscleGroup.CORE),
        equipmentNeeded = listOf(Equipment.BODYWEIGHT)
    )
    private val allExercises = listOf(benchPress, squat, pushUp)

    private val useCase = FilterExercisesUseCase()

    @Test
    fun `blank query and empty selections return everything unfiltered`() {
        val result = useCase(allExercises, searchQuery = "", selectedMuscles = emptySet(), selectedEquipment = emptySet())

        assertEquals(allExercises, result)
    }

    @Test
    fun `search query matches by name case-insensitively`() {
        val result = useCase(allExercises, searchQuery = "bench", selectedMuscles = emptySet(), selectedEquipment = emptySet())

        assertEquals(listOf(benchPress), result)
    }

    @Test
    fun `search query matches by description when name does not match`() {
        val result = useCase(allExercises, searchQuery = "compound", selectedMuscles = emptySet(), selectedEquipment = emptySet())

        assertEquals(listOf(squat), result)
    }

    @Test
    fun `search query matches by primary muscle name`() {
        val result = useCase(allExercises, searchQuery = "legs", selectedMuscles = emptySet(), selectedEquipment = emptySet())

        assertEquals(listOf(squat), result)
    }

    @Test
    fun `search query matching nothing returns an empty list`() {
        val result = useCase(allExercises, searchQuery = "nonexistent", selectedMuscles = emptySet(), selectedEquipment = emptySet())

        assertEquals(emptyList<Exercise>(), result)
    }

    @Test
    fun `muscle filter matches primary or secondary muscles`() {
        // CORE is only a secondary muscle on pushUp - must still match.
        val result = useCase(
            allExercises,
            searchQuery = "",
            selectedMuscles = setOf(MuscleGroup.CORE),
            selectedEquipment = emptySet()
        )

        assertEquals(listOf(pushUp), result)
    }

    @Test
    fun `muscle filter requires ALL selected muscles - AND logic`() {
        // No single exercise targets both CHEST and LEGS.
        val result = useCase(
            allExercises,
            searchQuery = "",
            selectedMuscles = setOf(MuscleGroup.CHEST, MuscleGroup.LEGS),
            selectedEquipment = emptySet()
        )

        assertEquals(emptyList<Exercise>(), result)
    }

    @Test
    fun `equipment filter requires ALL selected equipment - AND logic`() {
        // benchPress needs both BARBELL and BENCH; squat only has BARBELL.
        val result = useCase(
            allExercises,
            searchQuery = "",
            selectedMuscles = emptySet(),
            selectedEquipment = setOf(Equipment.BARBELL, Equipment.BENCH)
        )

        assertEquals(listOf(benchPress), result)
    }

    @Test
    fun `all three criteria combine with AND logic`() {
        val result = useCase(
            allExercises,
            searchQuery = "press",
            selectedMuscles = setOf(MuscleGroup.CHEST),
            selectedEquipment = setOf(Equipment.BARBELL)
        )

        assertEquals(listOf(benchPress), result)
    }
}

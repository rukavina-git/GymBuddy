package com.rukavina.gymbuddy.ui.exercise

import com.rukavina.gymbuddy.domain.model.DifficultyLevel
import com.rukavina.gymbuddy.domain.model.Equipment
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.ExerciseType
import com.rukavina.gymbuddy.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Test

/** Every enum value has its own label, so no two share a string by mistake. */
class ExerciseTextTest {

    private fun <E> distinct(values: List<E>, label: (E) -> Int) =
        assertEquals(values.size, values.map(label).toSet().size)

    @Test fun muscles() = distinct(MuscleGroup.entries, ::muscleLabel)
    @Test fun equipment() = distinct(Equipment.entries, ::equipmentLabel)
    @Test fun tracking() = distinct(ExerciseTrackingType.entries, ::trackingLabel)
    @Test fun difficulty() = distinct(DifficultyLevel.entries, ::difficultyLabel)
    @Test fun kind() = distinct(ExerciseType.entries, ::kindLabel)
}

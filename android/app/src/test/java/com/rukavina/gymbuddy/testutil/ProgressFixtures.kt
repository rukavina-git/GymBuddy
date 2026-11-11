package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.model.ExerciseCategory
import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.PerformedExercise
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.model.WorkoutSet

/** Sessions for Progress tests (Plan 7 step 08). */
object ProgressFixtures {
    const val DAY = 86_400_000L

    /** One performed exercise; [sets] as (kg, reps) pairs, kg may be null. */
    fun lift(
        exerciseId: String,
        vararg sets: Pair<Float?, Int?>,
        name: String = exerciseId,
        type: ExerciseTrackingType = ExerciseTrackingType.WEIGHT_REPS,
        order: Int = 0,
    ) = PerformedExercise(
        id = "$exerciseId-$order-${sets.size}",
        exerciseId = exerciseId,
        orderIndex = order,
        exerciseName = name,
        exerciseCategory = ExerciseCategory.STRENGTH,
        exerciseTrackingType = type,
        exercisePrimaryMuscles = emptyList(),
        sets = sets.mapIndexed { i, (kg, reps) -> WorkoutSet(id = "$exerciseId-s$i", weightKg = kg, reps = reps, isCompleted = true, orderIndex = i) },
    )

    fun day(id: String, day: Int, vararg lifts: PerformedExercise, title: String = id, minutes: Int = 60, deletedAt: Long? = null) =
        WorkoutSession(
            id = id,
            startedAt = day * DAY,
            endedAt = day * DAY + minutes * 60_000L,
            durationSeconds = minutes * 60,
            title = title,
            performedExercises = lifts.toList(),
            deletedAt = deletedAt,
        )
}

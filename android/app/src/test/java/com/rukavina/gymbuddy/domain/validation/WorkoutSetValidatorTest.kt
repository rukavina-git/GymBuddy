package com.rukavina.gymbuddy.domain.validation

import com.rukavina.gymbuddy.domain.model.ExerciseTrackingType
import com.rukavina.gymbuddy.domain.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSetValidatorTest {

    private fun set(
        weightKg: Float? = null,
        reps: Int? = null,
        durationSeconds: Int? = null,
        distanceMeters: Float? = null,
        isCompleted: Boolean = true,
        restTakenSeconds: Int? = null
    ) = WorkoutSet(
        id = "set-1",
        weightKg = weightKg,
        reps = reps,
        durationSeconds = durationSeconds,
        distanceMeters = distanceMeters,
        isCompleted = isCompleted,
        restTakenSeconds = restTakenSeconds,
        orderIndex = 0
    )

    private fun assertValid(result: WorkoutSetValidationResult) {
        assertEquals(WorkoutSetValidationResult.Valid, result)
    }

    private fun assertInvalid(result: WorkoutSetValidationResult) {
        assertTrue(result is WorkoutSetValidationResult.Invalid)
    }

    // WEIGHT_REPS

    @Test
    fun `weight reps valid when reps present`() {
        val result = WorkoutSetValidator.validate(
            set(reps = 10, weightKg = 50f),
            ExerciseTrackingType.WEIGHT_REPS
        )
        assertValid(result)
    }

    @Test
    fun `weight reps invalid when reps missing`() {
        val result = WorkoutSetValidator.validate(
            set(reps = null, weightKg = 50f),
            ExerciseTrackingType.WEIGHT_REPS
        )
        assertInvalid(result)
    }

    // REPS_ONLY

    @Test
    fun `reps only valid when reps present`() {
        val result = WorkoutSetValidator.validate(
            set(reps = 15),
            ExerciseTrackingType.REPS_ONLY
        )
        assertValid(result)
    }

    @Test
    fun `reps only invalid when reps missing`() {
        val result = WorkoutSetValidator.validate(
            set(reps = null),
            ExerciseTrackingType.REPS_ONLY
        )
        assertInvalid(result)
    }

    // WEIGHT_DURATION

    @Test
    fun `weight duration valid when duration present`() {
        val result = WorkoutSetValidator.validate(
            set(durationSeconds = 30, weightKg = 20f),
            ExerciseTrackingType.WEIGHT_DURATION
        )
        assertValid(result)
    }

    @Test
    fun `weight duration invalid when duration missing`() {
        val result = WorkoutSetValidator.validate(
            set(durationSeconds = null, weightKg = 20f),
            ExerciseTrackingType.WEIGHT_DURATION
        )
        assertInvalid(result)
    }

    // DURATION

    @Test
    fun `duration valid when duration present`() {
        val result = WorkoutSetValidator.validate(
            set(durationSeconds = 60),
            ExerciseTrackingType.DURATION
        )
        assertValid(result)
    }

    @Test
    fun `duration invalid when duration missing`() {
        val result = WorkoutSetValidator.validate(
            set(durationSeconds = null),
            ExerciseTrackingType.DURATION
        )
        assertInvalid(result)
    }

    // DISTANCE_DURATION (requires both fields)

    @Test
    fun `distance duration valid when both present`() {
        val result = WorkoutSetValidator.validate(
            set(distanceMeters = 1000f, durationSeconds = 300),
            ExerciseTrackingType.DISTANCE_DURATION
        )
        assertValid(result)
    }

    @Test
    fun `distance duration invalid when distance missing`() {
        val result = WorkoutSetValidator.validate(
            set(distanceMeters = null, durationSeconds = 300),
            ExerciseTrackingType.DISTANCE_DURATION
        )
        assertInvalid(result)
    }

    @Test
    fun `distance duration invalid when duration missing`() {
        val result = WorkoutSetValidator.validate(
            set(distanceMeters = 1000f, durationSeconds = null),
            ExerciseTrackingType.DISTANCE_DURATION
        )
        assertInvalid(result)
    }

    // WEIGHT_DISTANCE

    @Test
    fun `weight distance valid when distance present`() {
        val result = WorkoutSetValidator.validate(
            set(distanceMeters = 500f, weightKg = 10f),
            ExerciseTrackingType.WEIGHT_DISTANCE
        )
        assertValid(result)
    }

    @Test
    fun `weight distance invalid when distance missing`() {
        val result = WorkoutSetValidator.validate(
            set(distanceMeters = null, weightKg = 10f),
            ExerciseTrackingType.WEIGHT_DISTANCE
        )
        assertInvalid(result)
    }

    // Incomplete-set carve-out

    @Test
    fun `incomplete set is valid even with everything null`() {
        val result = WorkoutSetValidator.validate(
            set(isCompleted = false),
            ExerciseTrackingType.WEIGHT_REPS
        )
        assertValid(result)
    }

    @Test
    fun `incomplete set is valid even with negative values`() {
        val result = WorkoutSetValidator.validate(
            set(reps = -5, isCompleted = false),
            ExerciseTrackingType.WEIGHT_REPS
        )
        assertValid(result)
    }

    // Negative-value rejections

    @Test
    fun `negative weight is rejected`() {
        val result = WorkoutSetValidator.validate(
            set(weightKg = -1f, reps = 5),
            ExerciseTrackingType.WEIGHT_REPS
        )
        assertInvalid(result)
    }

    @Test
    fun `negative reps is rejected`() {
        val result = WorkoutSetValidator.validate(
            set(reps = -1),
            ExerciseTrackingType.WEIGHT_REPS
        )
        assertInvalid(result)
    }

    @Test
    fun `negative duration is rejected`() {
        val result = WorkoutSetValidator.validate(
            set(durationSeconds = -1),
            ExerciseTrackingType.DURATION
        )
        assertInvalid(result)
    }

    @Test
    fun `negative distance is rejected`() {
        val result = WorkoutSetValidator.validate(
            set(distanceMeters = -1f),
            ExerciseTrackingType.WEIGHT_DISTANCE
        )
        assertInvalid(result)
    }

    @Test
    fun `negative rest taken is rejected`() {
        val result = WorkoutSetValidator.validate(
            set(reps = 5, restTakenSeconds = -1),
            ExerciseTrackingType.WEIGHT_REPS
        )
        assertInvalid(result)
    }

    // Zero-value rules

    @Test
    fun `zero duration is rejected on completed set`() {
        val result = WorkoutSetValidator.validate(
            set(durationSeconds = 0),
            ExerciseTrackingType.DURATION
        )
        assertInvalid(result)
    }

    @Test
    fun `zero distance is rejected on completed set`() {
        val result = WorkoutSetValidator.validate(
            set(distanceMeters = 0f),
            ExerciseTrackingType.WEIGHT_DISTANCE
        )
        assertInvalid(result)
    }

    @Test
    fun `zero weight is valid for bodyweight movements`() {
        val result = WorkoutSetValidator.validate(
            set(weightKg = 0f, reps = 10),
            ExerciseTrackingType.WEIGHT_REPS
        )
        assertValid(result)
    }

    @Test
    fun `zero reps is valid`() {
        val result = WorkoutSetValidator.validate(
            set(reps = 0),
            ExerciseTrackingType.WEIGHT_REPS
        )
        assertValid(result)
    }
}

package com.rukavina.gymbuddy.ui.exercises.create

import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.usecase.exercise.CreateExerciseUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.UpdateExerciseUseCase
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Writes an [ExerciseForm]: Create A, Create C (Exercises search and the
 * picker) and Edit share it. The rest under More details is your
 * per-user rest, so it goes to the overlay, not the exercise.
 */
class ExerciseCreator @Inject constructor(
    private val create: CreateExerciseUseCase,
    private val update: UpdateExerciseUseCase,
    private val exercises: ExerciseRepository,
    private val ids: IdGenerator,
) {
    /** New exercise of your own; the result is what was written. */
    suspend fun create(form: ExerciseForm): Result<Exercise> = runCatchingIo {
        val exercise = form.toExercise(ids.newId())
        create(exercise).getOrThrow()
        if (form.restSeconds != null) exercises.setExerciseRest(exercise.id, form.restSeconds)
        exercise
    }

    /** Save [form] over [base]; the rest is written only when it changed. */
    suspend fun update(base: Exercise, form: ExerciseForm, oldRest: Int?): Result<Exercise> = runCatchingIo {
        val exercise = form.toExercise(base.id, base)
        update(exercise).getOrThrow()
        if (form.restSeconds != oldRest) exercises.setExerciseRest(exercise.id, form.restSeconds)
        exercise
    }

    private inline fun <T> runCatchingIo(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

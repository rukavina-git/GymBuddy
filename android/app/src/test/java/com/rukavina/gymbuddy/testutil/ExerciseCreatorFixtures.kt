package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.usecase.exercise.CreateExerciseUseCase
import com.rukavina.gymbuddy.domain.usecase.exercise.UpdateExerciseUseCase
import com.rukavina.gymbuddy.ui.exercises.create.ExerciseCreator

/** A real [ExerciseCreator] over [repo] (create/update use cases included). */
fun exerciseCreator(repo: ExerciseRepository, ids: IdGenerator = CountingIds("new")) =
    ExerciseCreator(CreateExerciseUseCase(repo, ids), UpdateExerciseUseCase(repo), repo, ids)

/** "new-1", "new-2", … */
class CountingIds(private val prefix: String) : IdGenerator {
    private var n = 0
    override fun newId(): String = "$prefix-${++n}"
}

package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory ExerciseRepository fake for domain-layer unit tests. Only
 * getExerciseById is exercised by current tests; the rest are simple,
 * consistent bookkeeping so the fake stays a true drop-in.
 */
class FakeExerciseRepository(
    initialExercises: List<Exercise> = emptyList()
) : ExerciseRepository {

    private val exercises = initialExercises.associateBy { it.id }.toMutableMap()
    private val hidden = mutableSetOf<String>()
    private val notes = mutableMapOf<String, String?>()
    private val allExercisesFlow = MutableStateFlow(exercises.values.toList())

    private fun publish() {
        allExercisesFlow.value = exercises.values.toList()
    }

    override fun getAllExercises(): Flow<List<Exercise>> = allExercisesFlow

    override suspend fun getExerciseById(id: String): Exercise? = exercises[id]

    override suspend fun createExercise(exercise: Exercise) {
        exercises[exercise.id] = exercise
        publish()
    }

    override suspend fun updateExercise(exercise: Exercise) {
        exercises[exercise.id] = exercise
        publish()
    }

    override suspend fun deleteExercise(id: String) {
        exercises.remove(id)
        publish()
    }

    override suspend fun hideExercise(id: String) {
        hidden.add(id)
    }

    override suspend fun unhideExercise(id: String) {
        hidden.remove(id)
    }

    override fun getHiddenExercises(): Flow<List<Exercise>> =
        MutableStateFlow(exercises.values.filter { it.id in hidden })

    override suspend fun unhideAllExercises() {
        hidden.clear()
    }

    override suspend fun getExerciseNote(exerciseId: String): String? = notes[exerciseId]

    override suspend fun updateExerciseNote(exerciseId: String, note: String?) {
        notes[exerciseId] = note
    }

    override fun searchExercises(query: String): Flow<List<Exercise>> =
        MutableStateFlow(exercises.values.filter { it.name.contains(query, ignoreCase = true) })

    override fun getAllExercisesIncludingHidden(): Flow<List<Exercise>> = allExercisesFlow
}

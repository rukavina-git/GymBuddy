package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.model.ExerciseUserState
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * In-memory ExerciseRepository fake. Deletes are tombstones (deletedAt
 * set) like Room, so the "any state" read and restore behave the same.
 * Your state (hidden, note, rest) lives in [states]. [failWrites] makes
 * every write throw (error paths).
 */
class FakeExerciseRepository(
    initialExercises: List<Exercise> = emptyList()
) : ExerciseRepository {

    private val exercises = MutableStateFlow(initialExercises.associateBy { it.id })
    val states = MutableStateFlow<Map<String, ExerciseUserState>>(emptyMap())
    var failWrites = false
    private var clock = 1L

    val all: Map<String, Exercise> get() = exercises.value

    private fun failIfBroken() {
        check(!failWrites) { "write failed" }
    }

    private fun put(e: Exercise) {
        exercises.value = exercises.value + (e.id to e)
    }

    private fun state(id: String, change: (ExerciseUserState) -> ExerciseUserState) {
        val current = states.value[id] ?: ExerciseUserState(id)
        states.value = states.value + (id to change(current))
    }

    /** Gone without a trace (no tombstone): stamping a template can't find it. */
    fun purge(id: String) {
        exercises.value = exercises.value - id
    }

    private fun live(e: Exercise) = e.deletedAt == null && !e.deprecated

    override fun getAllExercises(): Flow<List<Exercise>> = combine(exercises, states) { all, s ->
        all.values.filter { live(it) && s[it.id]?.hidden != true }.sortedBy { it.name }
    }

    override suspend fun getExerciseById(id: String): Exercise? = exercises.value[id]?.takeIf { it.deletedAt == null }

    override suspend fun createExercise(exercise: Exercise) {
        failIfBroken()
        put(exercise)
    }

    override suspend fun updateExercise(exercise: Exercise) = createExercise(exercise)

    override suspend fun deleteExercise(id: String) {
        failIfBroken()
        exercises.value[id]?.let { put(it.copy(deletedAt = clock++)) }
    }

    override suspend fun hideExercise(id: String) {
        failIfBroken()
        state(id) { it.copy(hidden = true) }
    }

    override suspend fun unhideExercise(id: String) {
        failIfBroken()
        state(id) { it.copy(hidden = false) }
    }

    override fun getHiddenExercises(): Flow<List<Exercise>> = combine(exercises, states) { all, s ->
        all.values.filter { live(it) && s[it.id]?.hidden == true }
    }

    override suspend fun unhideAllExercises() {
        states.value = states.value.mapValues { it.value.copy(hidden = false) }
    }

    override suspend fun getExerciseNote(exerciseId: String): String? = states.value[exerciseId]?.note

    override suspend fun updateExerciseNote(exerciseId: String, note: String?) {
        failIfBroken()
        state(exerciseId) { it.copy(note = note) }
    }

    override fun searchExercises(query: String): Flow<List<Exercise>> =
        exercises.map { all -> all.values.filter { live(it) && it.name.contains(query, ignoreCase = true) } }

    override fun getAllExercisesIncludingHidden(): Flow<List<Exercise>> =
        exercises.map { all -> all.values.filter { live(it) }.sortedBy { it.name } }

    override fun observeExercise(id: String): Flow<Exercise?> = exercises.map { it[id]?.takeIf { e -> e.deletedAt == null } }

    override suspend fun getExercisesAnyState(ids: List<String>): List<Exercise> = ids.distinct().mapNotNull { exercises.value[it] }

    override suspend fun restoreExercise(id: String) {
        failIfBroken()
        exercises.value[id]?.let { put(it.copy(deletedAt = null)) }
    }

    override fun observeUserState(exerciseId: String): Flow<ExerciseUserState> =
        states.map { it[exerciseId] ?: ExerciseUserState(exerciseId) }

    override fun observeUserStates(): Flow<Map<String, ExerciseUserState>> = states

    override suspend fun setExerciseRest(exerciseId: String, seconds: Int?) {
        failIfBroken()
        state(exerciseId) { it.copy(restSeconds = seconds) }
    }
}

package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.ExerciseDao
import com.rukavina.gymbuddy.data.local.dao.UserExerciseStateDao
import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.mapper.ExerciseMapper
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.domain.model.EntitySource
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Implementation of ExerciseRepository over the local Room database.
 * Writes to CUSTOM exercises and to the user_exercise_state overlay go
 * through [OutboxRecorder] so the sync engine pushes them. DEFAULT rows
 * are server-owned reference data and are never queued: the server
 * would reject them as FORBIDDEN anyway.
 *
 * Writes keep the stored revision - see WorkoutSessionRepositoryImpl.
 */
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val userExerciseStateDao: UserExerciseStateDao,
    private val outbox: OutboxRecorder,
    private val clock: Clock
) : ExerciseRepository {

    override fun getAllExercises(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercises().map { entities ->
            ExerciseMapper.toDomainList(entities)
        }
    }

    override suspend fun getExerciseById(id: String): Exercise? {
        val entity = exerciseDao.getExerciseById(id)
        return entity?.let { ExerciseMapper.toDomain(it) }
    }

    override suspend fun createExercise(exercise: Exercise) {
        writeExercise(exercise) { exerciseDao.insertExercise(it) }
    }

    override suspend fun updateExercise(exercise: Exercise) {
        writeExercise(exercise) { exerciseDao.updateExercise(it) }
    }

    private suspend fun writeExercise(exercise: Exercise, write: suspend (ExerciseEntity) -> Unit) {
        if (exercise.source == EntitySource.DEFAULT) {
            write(ExerciseMapper.toEntity(exercise).copy(updatedAt = clock.millis()))
            return
        }
        outbox.record(SyncEntityType.EXERCISE, exercise.id) {
            val revision = exerciseDao.getRevision(exercise.id) ?: 0
            write(
                ExerciseMapper.toEntity(exercise)
                    .copy(updatedAt = clock.millis(), revision = revision, syncState = SyncState.PENDING)
            )
        }
    }

    override suspend fun deleteExercise(id: String) {
        val now = clock.millis()
        outbox.record(SyncEntityType.EXERCISE, id, OutboxOperation.DELETE) {
            exerciseDao.deleteExercise(id, deletedAt = now, updatedAt = now)
        }
    }

    override suspend fun hideExercise(id: String) {
        outbox.record(SyncEntityType.USER_EXERCISE_STATE, id) {
            userExerciseStateDao.setHidden(id, true, clock.millis())
        }
    }

    override suspend fun unhideExercise(id: String) {
        outbox.record(SyncEntityType.USER_EXERCISE_STATE, id) {
            userExerciseStateDao.setHidden(id, false, clock.millis())
        }
    }

    override fun getHiddenExercises(): Flow<List<Exercise>> {
        return exerciseDao.getHiddenExercises().map { entities ->
            ExerciseMapper.toDomainList(entities)
        }
    }

    override suspend fun unhideAllExercises() {
        val ids = userExerciseStateDao.getHiddenIds()
        outbox.recordAll(SyncEntityType.USER_EXERCISE_STATE, ids) {
            userExerciseStateDao.unhideAll(clock.millis())
        }
    }

    override suspend fun getExerciseNote(exerciseId: String): String? {
        return userExerciseStateDao.getState(exerciseId)?.note
    }

    override suspend fun updateExerciseNote(exerciseId: String, note: String?) {
        outbox.record(SyncEntityType.USER_EXERCISE_STATE, exerciseId) {
            userExerciseStateDao.setNote(exerciseId, note, clock.millis())
        }
    }

    override fun searchExercises(query: String): Flow<List<Exercise>> {
        return exerciseDao.searchExercises(query).map { entities ->
            ExerciseMapper.toDomainList(entities)
        }
    }

    override fun getAllExercisesIncludingHidden(): Flow<List<Exercise>> {
        return exerciseDao.getAllExercisesIncludingHidden().map { entities ->
            ExerciseMapper.toDomainList(entities)
        }
    }
}

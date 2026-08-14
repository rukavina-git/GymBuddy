package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.ExerciseDao
import com.rukavina.gymbuddy.data.local.dao.UserExerciseStateDao
import com.rukavina.gymbuddy.data.local.mapper.ExerciseMapper
import com.rukavina.gymbuddy.domain.model.Exercise
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Implementation of ExerciseRepository.
 * Currently uses only local Room database.
 * Can be extended to sync with remote API in the future.
 */
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val userExerciseStateDao: UserExerciseStateDao,
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
        val entity = ExerciseMapper.toEntity(exercise).copy(updatedAt = clock.millis())
        exerciseDao.insertExercise(entity)
        // TODO: Sync with remote API when online
    }

    override suspend fun updateExercise(exercise: Exercise) {
        val entity = ExerciseMapper.toEntity(exercise).copy(updatedAt = clock.millis())
        exerciseDao.updateExercise(entity)
        // TODO: Sync with remote API when online
    }

    override suspend fun deleteExercise(id: String) {
        val now = clock.millis()
        exerciseDao.deleteExercise(id, deletedAt = now, updatedAt = now)
        // TODO: Sync deletion with remote API when online
    }

    override suspend fun hideExercise(id: String) {
        userExerciseStateDao.setHidden(id, true, clock.millis())
    }

    override suspend fun unhideExercise(id: String) {
        userExerciseStateDao.setHidden(id, false, clock.millis())
    }

    override fun getHiddenExercises(): Flow<List<Exercise>> {
        return exerciseDao.getHiddenExercises().map { entities ->
            ExerciseMapper.toDomainList(entities)
        }
    }

    override suspend fun unhideAllExercises() {
        userExerciseStateDao.unhideAll(clock.millis())
    }

    override suspend fun getExerciseNote(exerciseId: String): String? {
        return userExerciseStateDao.getState(exerciseId)?.note
    }

    override suspend fun updateExerciseNote(exerciseId: String, note: String?) {
        userExerciseStateDao.setNote(exerciseId, note, clock.millis())
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

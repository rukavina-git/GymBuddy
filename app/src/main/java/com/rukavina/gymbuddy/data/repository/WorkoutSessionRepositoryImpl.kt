package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.WorkoutSessionDao
import com.rukavina.gymbuddy.data.local.mapper.WorkoutSessionMapper
import com.rukavina.gymbuddy.data.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of WorkoutSessionRepository.
 * Currently uses only local Room database.
 * Can be extended to sync with remote API in the future.
 */
class WorkoutSessionRepositoryImpl @Inject constructor(
    private val workoutSessionDao: WorkoutSessionDao
) : WorkoutSessionRepository {

    override fun getAllWorkoutSessions(): Flow<List<WorkoutSession>> {
        return workoutSessionDao.getAllWorkoutSessions().map { workoutSessionsWithExercises ->
            WorkoutSessionMapper.toDomainList(workoutSessionsWithExercises)
        }
    }

    override suspend fun getWorkoutSessionById(id: String): WorkoutSession? {
        val workoutSessionWithExercises = workoutSessionDao.getWorkoutSessionById(id)
        return workoutSessionWithExercises?.let { WorkoutSessionMapper.toDomain(it) }
    }

    override fun getWorkoutSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSession>> {
        return workoutSessionDao.getWorkoutSessionsByDateRange(startDate, endDate).map { workoutSessionsWithExercises ->
            WorkoutSessionMapper.toDomainList(workoutSessionsWithExercises)
        }
    }

    override suspend fun createWorkoutSession(workoutSession: WorkoutSession) {
        val (workoutSessionEntity, performedExerciseEntities) = WorkoutSessionMapper.toEntities(workoutSession)
        workoutSessionDao.insertWorkoutSessionWithExercises(workoutSessionEntity, performedExerciseEntities)
        // TODO: Sync with remote API when online
    }

    override suspend fun updateWorkoutSession(workoutSession: WorkoutSession) {
        val (workoutSessionEntity, performedExerciseEntities) = WorkoutSessionMapper.toEntities(workoutSession)
        workoutSessionDao.updateWorkoutSessionWithExercises(workoutSessionEntity, performedExerciseEntities)
        // TODO: Sync with remote API when online
    }

    override suspend fun deleteWorkoutSession(id: String) {
        workoutSessionDao.deleteWorkoutSession(id)
        // TODO: Sync deletion with remote API when online
    }
}

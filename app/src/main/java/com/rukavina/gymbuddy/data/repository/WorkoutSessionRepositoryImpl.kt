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
            workoutSessionsWithExercises.map { sessionWithExercises ->
                val performedExercisesWithSets = workoutSessionDao.getPerformedExercisesWithSets(sessionWithExercises.workoutSession.id)
                WorkoutSessionMapper.toDomain(sessionWithExercises.workoutSession, performedExercisesWithSets)
            }
        }
    }

    override suspend fun getWorkoutSessionById(id: String): WorkoutSession? {
        val workoutSessionWithExercises = workoutSessionDao.getWorkoutSessionById(id)
        return workoutSessionWithExercises?.let {
            val performedExercisesWithSets = workoutSessionDao.getPerformedExercisesWithSets(it.workoutSession.id)
            WorkoutSessionMapper.toDomain(it.workoutSession, performedExercisesWithSets)
        }
    }

    override fun getWorkoutSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSession>> {
        return workoutSessionDao.getWorkoutSessionsByDateRange(startDate, endDate).map { workoutSessionsWithExercises ->
            workoutSessionsWithExercises.map { sessionWithExercises ->
                val performedExercisesWithSets = workoutSessionDao.getPerformedExercisesWithSets(sessionWithExercises.workoutSession.id)
                WorkoutSessionMapper.toDomain(sessionWithExercises.workoutSession, performedExercisesWithSets)
            }
        }
    }

    override suspend fun createWorkoutSession(workoutSession: WorkoutSession) {
        val (workoutSessionEntity, performedExerciseEntities, workoutSetEntities) = WorkoutSessionMapper.toEntities(workoutSession)
        workoutSessionDao.insertWorkoutSession(workoutSessionEntity)
        workoutSessionDao.insertPerformedExercises(performedExerciseEntities)
        workoutSessionDao.insertWorkoutSets(workoutSetEntities)
        // TODO: Sync with remote API when online
    }

    override suspend fun updateWorkoutSession(workoutSession: WorkoutSession) {
        val (workoutSessionEntity, performedExerciseEntities, workoutSetEntities) = WorkoutSessionMapper.toEntities(workoutSession)
        workoutSessionDao.updateWorkoutSession(workoutSessionEntity)
        workoutSessionDao.deletePerformedExercisesByWorkoutSessionId(workoutSession.id)
        workoutSessionDao.insertPerformedExercises(performedExerciseEntities)
        workoutSessionDao.insertWorkoutSets(workoutSetEntities)
        // TODO: Sync with remote API when online
    }

    override suspend fun deleteWorkoutSession(id: String) {
        workoutSessionDao.deleteWorkoutSession(id)
        // TODO: Sync deletion with remote API when online
    }
}

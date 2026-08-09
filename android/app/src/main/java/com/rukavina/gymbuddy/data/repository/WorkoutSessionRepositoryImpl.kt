package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.WorkoutSessionDao
import com.rukavina.gymbuddy.data.local.mapper.WorkoutSessionMapper
import com.rukavina.gymbuddy.domain.model.WorkoutSession
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
        return workoutSessionDao.getAllWorkoutSessions().map { sessions ->
            sessions.map { session ->
                val performedExercisesWithSets = workoutSessionDao.getPerformedExercisesWithSets(session.id)
                WorkoutSessionMapper.toDomain(session, performedExercisesWithSets)
            }
        }
    }

    override suspend fun getWorkoutSessionById(id: String): WorkoutSession? {
        val session = workoutSessionDao.getWorkoutSessionById(id)
        return session?.let {
            val performedExercisesWithSets = workoutSessionDao.getPerformedExercisesWithSets(it.id)
            WorkoutSessionMapper.toDomain(it, performedExercisesWithSets)
        }
    }

    override fun getWorkoutSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSession>> {
        return workoutSessionDao.getWorkoutSessionsByDateRange(startDate, endDate).map { sessions ->
            sessions.map { session ->
                val performedExercisesWithSets = workoutSessionDao.getPerformedExercisesWithSets(session.id)
                WorkoutSessionMapper.toDomain(session, performedExercisesWithSets)
            }
        }
    }

    override suspend fun createWorkoutSession(workoutSession: WorkoutSession) {
        requireStampedSnapshots(workoutSession)
        val (workoutSessionEntity, performedExerciseEntities, workoutSetEntities) = WorkoutSessionMapper.toEntities(workoutSession)
        workoutSessionDao.insertWorkoutSession(workoutSessionEntity)
        workoutSessionDao.insertPerformedExercises(performedExerciseEntities)
        workoutSessionDao.insertWorkoutSets(workoutSetEntities)
        // TODO: Sync with remote API when online
    }

    override suspend fun updateWorkoutSession(workoutSession: WorkoutSession) {
        requireStampedSnapshots(workoutSession)
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

    /**
     * Guards against ever persisting a PerformedExercise whose exercise
     * snapshot was never stamped. Construction sites are allowed to build a
     * PerformedExercise with placeholder snapshot values, relying on
     * ValidateWorkoutSessionSetsUseCase to overwrite them before the session
     * reaches a write method - this is the boundary every write crosses, so
     * it's where that reliance gets enforced rather than just documented.
     */
    private fun requireStampedSnapshots(workoutSession: WorkoutSession) {
        workoutSession.performedExercises.forEach { performedExercise ->
            check(performedExercise.exerciseName.isNotBlank()) {
                "PerformedExercise with exerciseId=${performedExercise.exerciseId} has an unstamped exercise snapshot (blank exerciseName). ValidateWorkoutSessionSetsUseCase must run before persistence."
            }
        }
    }
}

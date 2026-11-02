package com.rukavina.gymbuddy.data.repository

import com.rukavina.gymbuddy.data.local.dao.WorkoutSessionDao
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.entity.OutboxOperation
import com.rukavina.gymbuddy.data.local.entity.SyncEntityType
import com.rukavina.gymbuddy.data.local.mapper.WorkoutSessionMapper
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.domain.model.SyncState
import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/**
 * Implementation of WorkoutSessionRepository over the local Room
 * database. Every write goes through [OutboxRecorder], which queues the
 * session for the sync engine in the same transaction.
 *
 * A write keeps the stored revision rather than whatever the caller's
 * domain object carries: the revision is the server version this edit
 * is based on, and a stale in-memory copy must not roll it back.
 *
 * DAOs come from [DatabaseHolder] per call (the owner can switch);
 * writes use the database [OutboxRecorder] hands them.
 */
class WorkoutSessionRepositoryImpl @Inject constructor(
    private val holder: DatabaseHolder,
    private val outbox: OutboxRecorder,
    private val clock: Clock
) : WorkoutSessionRepository {

    private val workoutSessionDao: WorkoutSessionDao get() = holder.db.workoutSessionDao()

    override fun getAllWorkoutSessions(): Flow<List<WorkoutSession>> = holder.observe { db ->
        val dao = db.workoutSessionDao()
        dao.getAllWorkoutSessions().map { sessions ->
            sessions.map { session ->
                WorkoutSessionMapper.toDomain(session, dao.getPerformedExercisesWithSets(session.id))
            }
        }
    }

    override suspend fun getWorkoutSessionById(id: String): WorkoutSession? {
        val dao = workoutSessionDao
        val session = dao.getWorkoutSessionById(id)
        return session?.let {
            val performedExercisesWithSets = dao.getPerformedExercisesWithSets(it.id)
            WorkoutSessionMapper.toDomain(it, performedExercisesWithSets)
        }
    }

    override fun getWorkoutSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSession>> = holder.observe { db ->
        val dao = db.workoutSessionDao()
        dao.getWorkoutSessionsByDateRange(startDate, endDate).map { sessions ->
            sessions.map { session ->
                WorkoutSessionMapper.toDomain(session, dao.getPerformedExercisesWithSets(session.id))
            }
        }
    }

    override suspend fun createWorkoutSession(workoutSession: WorkoutSession) {
        requireStampedSnapshots(workoutSession)
        val (workoutSessionEntity, performedExerciseEntities, workoutSetEntities) = WorkoutSessionMapper.toEntities(workoutSession)
        outbox.record(SyncEntityType.WORKOUT_SESSION, workoutSession.id) { db ->
            val workoutSessionDao = db.workoutSessionDao()
            val revision = workoutSessionDao.getRevision(workoutSession.id) ?: 0
            workoutSessionDao.insertWorkoutSession(
                workoutSessionEntity.copy(updatedAt = clock.millis(), revision = revision, syncState = SyncState.PENDING)
            )
            workoutSessionDao.insertPerformedExercises(performedExerciseEntities)
            workoutSessionDao.insertWorkoutSets(workoutSetEntities)
        }
    }

    override suspend fun updateWorkoutSession(workoutSession: WorkoutSession) {
        requireStampedSnapshots(workoutSession)
        val (workoutSessionEntity, performedExerciseEntities, workoutSetEntities) = WorkoutSessionMapper.toEntities(workoutSession)
        outbox.record(SyncEntityType.WORKOUT_SESSION, workoutSession.id) { db ->
            val workoutSessionDao = db.workoutSessionDao()
            val revision = workoutSessionDao.getRevision(workoutSession.id) ?: 0
            workoutSessionDao.updateWorkoutSession(
                workoutSessionEntity.copy(updatedAt = clock.millis(), revision = revision, syncState = SyncState.PENDING)
            )
            workoutSessionDao.deletePerformedExercisesByWorkoutSessionId(workoutSession.id)
            workoutSessionDao.insertPerformedExercises(performedExerciseEntities)
            workoutSessionDao.insertWorkoutSets(workoutSetEntities)
        }
    }

    override suspend fun deleteWorkoutSession(id: String) {
        val now = clock.millis()
        outbox.record(SyncEntityType.WORKOUT_SESSION, id, OutboxOperation.DELETE) { db ->
            db.workoutSessionDao().deleteWorkoutSession(id, deletedAt = now, updatedAt = now)
        }
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

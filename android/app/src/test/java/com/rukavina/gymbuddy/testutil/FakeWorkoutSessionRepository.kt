package com.rukavina.gymbuddy.testutil

import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory WorkoutSessionRepository fake. Records every create/update
 * call verbatim (no re-derivation) so tests can assert exactly what a
 * use case handed to the repository.
 */
class FakeWorkoutSessionRepository(
    initialSessions: List<WorkoutSession> = emptyList()
) : WorkoutSessionRepository {

    private val sessions = initialSessions.associateBy { it.id }.toMutableMap()
    val created = mutableListOf<WorkoutSession>()
    val updated = mutableListOf<WorkoutSession>()

    override fun getAllWorkoutSessions(): Flow<List<WorkoutSession>> =
        MutableStateFlow(sessions.values.toList())

    override suspend fun getWorkoutSessionById(id: String): WorkoutSession? = sessions[id]

    override fun getWorkoutSessionsByDateRange(startDate: Long, endDate: Long): Flow<List<WorkoutSession>> =
        MutableStateFlow(sessions.values.filter { it.startedAt in startDate..endDate })

    override suspend fun createWorkoutSession(workoutSession: WorkoutSession) {
        sessions[workoutSession.id] = workoutSession
        created.add(workoutSession)
    }

    override suspend fun updateWorkoutSession(workoutSession: WorkoutSession) {
        sessions[workoutSession.id] = workoutSession
        updated.add(workoutSession)
    }

    override suspend fun deleteWorkoutSession(id: String) {
        sessions.remove(id)
    }
}

package com.rukavina.gymbuddy.domain.usecase.workout

import com.rukavina.gymbuddy.domain.model.WorkoutSession
import com.rukavina.gymbuddy.testutil.FakeWorkoutSessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteWorkoutSessionUseCaseTest {

    private val session = WorkoutSession(
        id = "session-1",
        startedAt = 1_000L,
        durationSeconds = 600,
        title = "Workout",
        performedExercises = emptyList()
    )

    @Test
    fun `fails without deleting when id is blank`() = runBlocking {
        val repo = FakeWorkoutSessionRepository(listOf(session))
        val useCase = DeleteWorkoutSessionUseCase(repo)

        val result = useCase("")

        assertTrue(result.isFailure)
        assertEquals("Workout session ID cannot be blank", result.exceptionOrNull()?.message)
        assertEquals(session, repo.getWorkoutSessionById("session-1"))
    }

    @Test
    fun `delegates to the repository when id is valid`() = runBlocking {
        val repo = FakeWorkoutSessionRepository(listOf(session))
        val useCase = DeleteWorkoutSessionUseCase(repo)

        val result = useCase("session-1")

        assertTrue(result.isSuccess)
        assertNull(repo.getWorkoutSessionById("session-1"))
    }
}

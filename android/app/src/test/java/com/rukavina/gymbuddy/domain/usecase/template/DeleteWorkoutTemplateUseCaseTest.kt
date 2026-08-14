package com.rukavina.gymbuddy.domain.usecase.template

import com.rukavina.gymbuddy.domain.model.WorkoutTemplate
import com.rukavina.gymbuddy.testutil.FakeWorkoutTemplateRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteWorkoutTemplateUseCaseTest {

    private val template = WorkoutTemplate(
        id = "template-1",
        title = "Push Day",
        templateExercises = emptyList()
    )

    @Test
    fun `fails without deleting when id is blank`() = runBlocking {
        val repo = FakeWorkoutTemplateRepository(listOf(template))
        val useCase = DeleteWorkoutTemplateUseCase(repo)

        val result = useCase(" ")

        assertTrue(result.isFailure)
        assertEquals("Template ID cannot be blank", result.exceptionOrNull()?.message)
        assertEquals(template, repo.getTemplateById("template-1"))
    }

    @Test
    fun `delegates to the repository when id is valid`() = runBlocking {
        val repo = FakeWorkoutTemplateRepository(listOf(template))
        val useCase = DeleteWorkoutTemplateUseCase(repo)

        val result = useCase("template-1")

        assertTrue(result.isSuccess)
        assertNull(repo.getTemplateById("template-1"))
    }
}

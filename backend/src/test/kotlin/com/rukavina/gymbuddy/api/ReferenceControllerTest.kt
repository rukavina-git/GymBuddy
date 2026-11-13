package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Verifies the read-only reference endpoints against the seeded default
 * library (V2__seed_reference_data.sql: exercise library version 9;
 * V7__starter_plans.sql: template library version 6, 3 starter plans) and against the ExerciseLibraryResponse /
 * TemplateLibraryResponse / ReferenceVersion schemas in api/openapi.yaml.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class ReferenceControllerTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private fun bearerToken(): String = TestJwtBuilder().subject("reference-test-${System.nanoTime()}").build()

    @Test
    fun `getReferenceVersion matches the seeded library versions`() {
        mockMvc.perform(get("/v1/reference/version").header("Authorization", "Bearer ${bearerToken()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.exerciseLibraryVersion").value(10))
            .andExpect(jsonPath("$.templateLibraryVersion").value(6))
    }

    @Test
    fun `getReferenceExercises returns the full default library including deprecated entries`() {
        mockMvc.perform(get("/v1/reference/exercises").header("Authorization", "Bearer ${bearerToken()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value(10))
            .andExpect(jsonPath("$.exercises").isArray)
            .andExpect(jsonPath("$.exercises.length()").value(60))
            .andExpect(jsonPath("$.exercises[0].id").exists())
            .andExpect(jsonPath("$.exercises[0].name").exists())
            .andExpect(jsonPath("$.exercises[0].primaryMuscles").isArray)
            .andExpect(jsonPath("$.exercises[0].source").value("DEFAULT"))
            .andExpect(jsonPath("$.exercises[0].ownerId").value(org.hamcrest.Matchers.nullValue()))
    }

    @Test
    fun `getReferenceTemplates returns templates with their nested exercises`() {
        mockMvc.perform(get("/v1/reference/templates").header("Authorization", "Bearer ${bearerToken()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value(6))
            .andExpect(jsonPath("$.templates").isArray)
            .andExpect(jsonPath("$.templates.length()").value(23))
            .andExpect(jsonPath("$.templates[0].id").exists())
            .andExpect(jsonPath("$.templates[0].title").exists())
            .andExpect(jsonPath("$.templates[0].source").value("DEFAULT"))
            .andExpect(jsonPath("$.templates[0].templateExercises").isArray)
            .andExpect(jsonPath("$.templates[0].templateExercises[0].exerciseName").exists())
    }

    @Test
    fun `getReferenceTemplates returns the 3 starter plans in order, and the old 13 templates are deprecated`() {
        val body = mockMvc.perform(get("/v1/reference/templates").header("Authorization", "Bearer ${bearerToken()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.plans.length()").value(3))
            .andExpect(jsonPath("$.plans[0].title").value("Full Body"))
            .andExpect(jsonPath("$.plans[0].suggestedFor").value("ONE_TO_THREE"))
            .andExpect(jsonPath("$.plans[0].templateIds.length()").value(3))
            .andExpect(jsonPath("$.plans[1].title").value("Upper / Lower"))
            .andExpect(jsonPath("$.plans[1].suggestedFor").value("FOUR"))
            .andExpect(jsonPath("$.plans[1].templateIds.length()").value(4))
            .andExpect(jsonPath("$.plans[2].title").value("Push / Pull / Legs"))
            .andExpect(jsonPath("$.plans[2].suggestedFor").value("FIVE_TO_SEVEN"))
            .andExpect(jsonPath("$.plans[2].deprecated").value(false))
            .andReturn().response.contentAsString

        val library = com.fasterxml.jackson.databind.ObjectMapper().readTree(body)
        val templatesById = library["templates"].associateBy { it["id"].asText() }
        val deprecatedTitles = library["templates"].filter { it["deprecated"].asBoolean() }.map { it["title"].asText() }
        assertEquals(13, deprecatedTitles.size)
        assertTrue("Chest & Triceps" in deprecatedTitles)
        val upperLower = library["plans"][1]["templateIds"].map { templatesById.getValue(it.asText())["title"].asText() }
        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), upperLower)
        library["plans"].flatMap { it["templateIds"] }.forEach {
            assertFalse(templatesById.getValue(it.asText())["deprecated"].asBoolean())
        }
    }

    @Test
    fun `reference endpoints require authentication`() {
        mockMvc.perform(get("/v1/reference/version")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/v1/reference/exercises")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/v1/reference/templates")).andExpect(status().isUnauthorized)
    }
}

package com.rukavina.gymbuddy.api

import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Verifies the read-only reference endpoints against the seeded default
 * library (V2 + V8 + V11 + V12: exercise library 12, template library 7,
 * 3 starter plans) and against the ExerciseLibraryResponse /
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
            .andExpect(jsonPath("$.exerciseLibraryVersion").value(12))
            .andExpect(jsonPath("$.templateLibraryVersion").value(7))
    }

    @Test
    fun `getReferenceExercises returns the full default library including deprecated entries`() {
        mockMvc.perform(get("/v1/reference/exercises").header("Authorization", "Bearer ${bearerToken()}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.version").value(12))
            .andExpect(jsonPath("$.exercises").isArray)
            .andExpect(jsonPath("$.exercises.length()").value(69))
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
            .andExpect(jsonPath("$.version").value(7))
            .andExpect(jsonPath("$.templates").isArray)
            .andExpect(jsonPath("$.templates.length()").value(31))
            .andExpect(jsonPath("$.templates[0].id").exists())
            .andExpect(jsonPath("$.templates[0].title").exists())
            .andExpect(jsonPath("$.templates[0].source").value("DEFAULT"))
            .andExpect(jsonPath("$.templates[0].templateExercises").isArray)
            .andExpect(jsonPath("$.templates[0].templateExercises[0].exerciseName").exists())
    }

    @Test
    fun `getReferenceTemplates returns the 3 starter plans in order, and the 21 replaced templates are deprecated`() {
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
        assertEquals(21, deprecatedTitles.size)
        assertTrue("Chest & Triceps" in deprecatedTitles)
        val upperLower = library["plans"][1]["templateIds"].map { templatesById.getValue(it.asText())["title"].asText() }
        assertEquals(listOf("Upper A", "Lower A", "Upper B", "Lower B"), upperLower)
        library["plans"].flatMap { it["templateIds"] }.forEach {
            assertFalse(templatesById.getValue(it.asText())["deprecated"].asBoolean())
        }
    }

    @Test
    fun `the seeded library matches the app's bundled JSON`() {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        val assets = java.io.File("../android/app/src/main/assets")
        val bundleEx = mapper.readTree(assets.resolve("default_exercises.json"))
        val bundleTp = mapper.readTree(assets.resolve("default_workout_templates.json"))
        fun fetch(path: String) = mapper.readTree(
            mockMvc.perform(get(path).header("Authorization", "Bearer ${bearerToken()}"))
                .andExpect(status().isOk).andReturn().response.contentAsString,
        )
        val serverEx = fetch("/v1/reference/exercises")
        val serverTp = fetch("/v1/reference/templates")

        assertEquals(bundleEx["version"].asInt(), serverEx["version"].asInt())
        fun exRow(e: com.fasterxml.jackson.databind.JsonNode) = listOf(
            e["id"], e["name"], e["deprecated"], e["difficulty"], e["trackingType"], e["category"],
            e["description"], e["instructions"], e["tips"], e["primaryMuscles"], e["secondaryMuscles"], e["equipmentNeeded"],
        ).joinToString("|") { it.toString() }
        assertEquals(bundleEx["exercises"].map(::exRow).sorted(), serverEx["exercises"].map(::exRow).sorted())

        assertEquals(bundleTp["version"].asInt(), serverTp["version"].asInt())
        fun tpRow(t: com.fasterxml.jackson.databind.JsonNode, exercises: String) =
            "${t["id"]}|${t["title"]}|${t["deprecated"]}|" +
                t[exercises].joinToString(",") { "${it["id"]}:${it["exerciseId"]}:${it["exerciseName"]}:${it["exerciseTrackingType"]}:${it["plannedSets"]}" }
        assertEquals(
            bundleTp["templates"].map { tpRow(it, "exercises") }.sorted(),
            serverTp["templates"].map { tpRow(it, "templateExercises") }.sorted(),
        )
        assertEquals(
            bundleTp["plans"].map { "${it["id"]}|${it["templateIds"]}" },
            serverTp["plans"].map { "${it["id"]}|${it["templateIds"]}" },
        )
    }

    @Test
    fun `reference endpoints require authentication`() {
        mockMvc.perform(get("/v1/reference/version")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/v1/reference/exercises")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/v1/reference/templates")).andExpect(status().isUnauthorized)
    }
}

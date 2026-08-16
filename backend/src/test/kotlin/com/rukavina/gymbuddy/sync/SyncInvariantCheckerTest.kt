package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * SyncInvariantChecker isn't wired to an endpoint yet (Group F point
 * 11) - exercised directly as a service method against a realistic
 * pushed batch, both the healthy case and a deliberately corrupted one.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class SyncInvariantCheckerTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var invariantChecker: SyncInvariantChecker

    private fun bearerToken(uid: String): String = TestJwtBuilder().subject(uid).build()

    private fun push(uid: String, request: PushRequestDto): PushResponseDto {
        val body = mockMvc.perform(
            post("/v1/sync/push")
                .header("Authorization", "Bearer ${bearerToken(uid)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        return objectMapper.readValue(body, PushResponseDto::class.java)
    }

    @Test
    fun `invariant check passes after a realistic batch and fails when a change_log row is deleted manually`() {
        val uid = "invariant-${System.nanoTime()}"
        val exercise = SyncTestFixtures.exercise()
        val template = SyncTestFixtures.workoutTemplate()
        val session = SyncTestFixtures.workoutSession()

        val response = push(
            uid,
            PushRequestDto(
                workoutSessions = listOf(session),
                exercises = listOf(exercise),
                workoutTemplates = listOf(template),
                userExerciseStates = listOf(SyncTestFixtures.userExerciseState(exerciseId = exercise.id)),
                userTemplateStates = listOf(SyncTestFixtures.userTemplateState(templateId = template.id)),
                userProfile = SyncTestFixtures.userProfile(),
            ),
        )
        assertTrue(response.results.all { it.status.name == "APPLIED" })

        assertEquals(emptyList<InvariantViolation>(), invariantChecker.check().filter { violationConcernsUid(it, uid, session.id, exercise.id) })

        jdbcTemplate.update(
            "DELETE FROM change_log WHERE user_id = :uid AND entity_id = :entityId",
            mapOf("uid" to uid, "entityId" to session.id),
        )

        val violations = invariantChecker.check().filter { violationConcernsUid(it, uid, session.id, exercise.id) }
        assertTrue(violations.isNotEmpty(), "deleting a change_log row should surface an invariant violation")
        assertTrue(violations.any { it.rule == "synced_entity_has_change_log_row" && it.detail.contains(session.id) })
    }

    /**
     * The Testcontainers instance is shared across the whole test class
     * (see AbstractPostgresIntegrationTest), so other test classes'
     * fixture data is present too - filter violations down to the ids
     * this test itself created before asserting on them.
     */
    private fun violationConcernsUid(violation: InvariantViolation, uid: String, vararg entityIds: String): Boolean =
        entityIds.any { violation.detail.contains(it) } || violation.detail.contains(uid)
}

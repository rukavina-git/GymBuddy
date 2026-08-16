package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.persistence.ExerciseSyncRepository
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Forces the one status the other push tests can't reach by construction:
 * ERROR only fires when something breaks that has nothing to do with the
 * pushed data itself (a DB outage, a bug), which SyncPushTest's real
 * Postgres round-trips never exercise on their own. @MockitoBean swaps
 * ExerciseSyncRepository for a mock that throws mid-transaction, so
 * ExerciseSyncService's catch block is exercised for real rather than
 * asserted by reading the source.
 *
 * This distinction is load-bearing: ERROR is the only status where the
 * client must keep the outbox entry and retry rather than clear it (see
 * api/openapi.yaml's push description). Confusing it with INVALID would
 * mean a transient failure permanently drops client data; confusing
 * INVALID with ERROR would mean an unfixable entity loops in the outbox
 * forever. A dedicated Spring context is required since MockitoBean
 * replaces a bean other push tests rely on behaving normally.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class SyncErrorPathTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @MockitoBean
    private lateinit var exerciseSyncRepository: ExerciseSyncRepository

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
    fun `an unexpected repository failure returns ERROR, not INVALID or FORBIDDEN, and writes no change_log row`() {
        val uid = "error-path-${System.nanoTime()}"
        val exercise = SyncTestFixtures.exercise()
        given(exerciseSyncRepository.find(exercise.id)).willThrow(RuntimeException("simulated database outage"))

        val response = push(uid, PushRequestDto(exercises = listOf(exercise)))
        val result = response.results.single()

        assertEquals(SyncStatus.ERROR, result.status)
        assertNotNull(result.reason)
        assertEquals(null, result.updatedAt)
        assertEquals(null, result.revision)

        val changeLogRows = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM change_log WHERE user_id = :uid AND entity_id = :entityId",
            mapOf("uid" to uid, "entityId" to exercise.id),
            Int::class.java,
        )
        assertEquals(0, changeLogRows, "a failed mutation must never produce a change_log row")
    }

    @Test
    fun `ERROR on one entity does not block the rest of the batch`() {
        val uid = "error-path-batch-${System.nanoTime()}"
        val exercise = SyncTestFixtures.exercise()
        val session = SyncTestFixtures.workoutSession()
        given(exerciseSyncRepository.find(exercise.id)).willThrow(RuntimeException("simulated database outage"))

        val response = push(uid, PushRequestDto(exercises = listOf(exercise), workoutSessions = listOf(session)))

        val exerciseResult = response.results.single { it.entityId == exercise.id }
        val sessionResult = response.results.single { it.entityId == session.id }
        assertEquals(SyncStatus.ERROR, exerciseResult.status)
        assertEquals(SyncStatus.APPLIED, sessionResult.status)
    }
}

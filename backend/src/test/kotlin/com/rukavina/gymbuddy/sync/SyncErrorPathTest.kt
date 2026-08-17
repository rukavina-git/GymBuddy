package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.persistence.ExerciseSyncRepository
import com.rukavina.gymbuddy.persistence.OverlaySyncRepository
import com.rukavina.gymbuddy.persistence.UserProfileRepository
import com.rukavina.gymbuddy.persistence.WorkoutTemplateSyncRepository
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
 * Postgres round-trips never exercise on their own.
 *
 * One @MockitoBean per per-type repository, all in this one context: a
 * given test only pushes the entity type whose repository it stubbed,
 * so the other three mocks sit inert and untouched - safe to share a
 * context rather than needing four separate ones. This is deliberately
 * one test per entity type (Exercise/WorkoutTemplate/both overlay
 * tables/Profile), not just one representative: per-type services exist
 * so a fault in one is contained (Group F/G/H's stated design), and a
 * service wiring its own repository or RevisionChecker call incorrectly
 * is exactly the bug that split protects against - testing it through
 * only one entity type would leave the other four unproven.
 *
 * This distinction is load-bearing: ERROR is the only status where the
 * client must keep the outbox entry and retry rather than clear it (see
 * api/openapi.yaml's push description). Confusing it with INVALID would
 * mean a transient failure permanently drops client data; confusing
 * INVALID with ERROR would mean an unfixable entity loops in the outbox
 * forever.
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

    @MockitoBean
    private lateinit var workoutTemplateSyncRepository: WorkoutTemplateSyncRepository

    @MockitoBean
    private lateinit var overlaySyncRepository: OverlaySyncRepository

    @MockitoBean
    private lateinit var userProfileRepository: UserProfileRepository

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

    private fun changeLogCount(uid: String, entityId: String): Int =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM change_log WHERE user_id = :uid AND entity_id = :entityId",
            mapOf("uid" to uid, "entityId" to entityId),
            Int::class.java,
        )!!

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
        assertEquals(0, changeLogCount(uid, exercise.id), "a failed mutation must never produce a change_log row")
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

    @Test
    fun `WorkoutTemplateSyncService returns ERROR on an unexpected repository failure`() {
        val uid = "error-path-template-${System.nanoTime()}"
        val template = SyncTestFixtures.workoutTemplate()
        given(workoutTemplateSyncRepository.find(template.id)).willThrow(RuntimeException("simulated database outage"))

        val result = push(uid, PushRequestDto(workoutTemplates = listOf(template))).results.single()

        assertEquals(SyncStatus.ERROR, result.status)
        assertEquals(0, changeLogCount(uid, template.id))
    }

    @Test
    fun `OverlaySyncService returns ERROR on an unexpected repository failure for a user exercise state`() {
        val uid = "error-path-exercise-state-${System.nanoTime()}"
        val state = SyncTestFixtures.userExerciseState()
        given(overlaySyncRepository.findExerciseState(uid, state.exerciseId)).willThrow(RuntimeException("simulated database outage"))

        val result = push(uid, PushRequestDto(userExerciseStates = listOf(state))).results.single()

        assertEquals(SyncStatus.ERROR, result.status)
        assertEquals(0, changeLogCount(uid, state.exerciseId))
    }

    @Test
    fun `OverlaySyncService returns ERROR on an unexpected repository failure for a user template state`() {
        val uid = "error-path-template-state-${System.nanoTime()}"
        val state = SyncTestFixtures.userTemplateState()
        given(overlaySyncRepository.findTemplateState(uid, state.templateId)).willThrow(RuntimeException("simulated database outage"))

        val result = push(uid, PushRequestDto(userTemplateStates = listOf(state))).results.single()

        assertEquals(SyncStatus.ERROR, result.status)
        assertEquals(0, changeLogCount(uid, state.templateId))
    }

    @Test
    fun `ProfileSyncService returns ERROR on an unexpected repository failure`() {
        val uid = "error-path-profile-${System.nanoTime()}"
        given(userProfileRepository.findByOwner(uid)).willThrow(RuntimeException("simulated database outage"))

        val result = push(uid, PushRequestDto(userProfile = SyncTestFixtures.userProfile())).results.single()

        assertEquals(SyncStatus.ERROR, result.status)
        assertEquals(0, changeLogCount(uid, uid))
    }
}

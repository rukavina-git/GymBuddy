package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PullResponseDto
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * gymbuddy.sync.read-only=true (SYNC_READ_ONLY) must reject every push
 * with 503 without touching the database, while pull keeps working.
 * "Keeps working" is verified here by actually pulling real data back,
 * not just hitting an unrelated endpoint - Group G's pull endpoint
 * didn't exist when this test was first written (Group F), which is
 * why it used to only prove "the rest of the API" rather than pull
 * specifically; that gap is closed now that pull exists.
 *
 * Since push is rejected outright under this flag, the data pulled here
 * is inserted directly via JDBC rather than through POST /v1/sync/push -
 * the one legitimate way to get data into this read-only context.
 *
 * A separate @SpringBootTest context (distinct @TestPropertySource) is
 * required since the flag is read once at controller construction.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
@TestPropertySource(properties = ["gymbuddy.sync.read-only=true"])
class SyncReadOnlyTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    private fun bearerToken(uid: String): String = TestJwtBuilder().subject(uid).build()

    @Test
    fun `push is rejected with 503 when sync is read-only`() {
        val uid = "read-only-${System.nanoTime()}"
        val request = PushRequestDto(workoutSessions = listOf(SyncTestFixtures.workoutSession()))

        mockMvc.perform(
            post("/v1/sync/push")
                .header("Authorization", "Bearer ${bearerToken(uid)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.traceId").exists())

        val stored = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM workout_sessions WHERE owner_id = :uid",
            mapOf("uid" to uid),
            Int::class.java,
        )
        assertEquals(0, stored, "a rejected push must never touch the database")
    }

    @Test
    fun `pull still returns real data when sync is read-only`() {
        val uid = "read-only-pull-${System.nanoTime()}"
        val exerciseId = SyncTestFixtures.uuid()
        val now = System.currentTimeMillis()

        // Push is unavailable under this flag, so seed data directly -
        // the same shape ExerciseSyncService would have written.
        jdbcTemplate.update(
            """
            INSERT INTO exercises
                (id, name, primary_muscles, secondary_muscles, description, instructions, tips, difficulty,
                 equipment_needed, category, exercise_type, tracking_type, video_url, thumbnail_url, source,
                 owner_id, derived_from_id, deprecated, updated_at, deleted_at, revision)
            VALUES
                (:id::uuid, 'Read-only pull test exercise', '[]'::jsonb, '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb,
                 'BEGINNER', '[]'::jsonb, 'STRENGTH', 'ISOLATION', 'WEIGHT_REPS', NULL, NULL, 'CUSTOM', :uid, NULL,
                 FALSE, :now, NULL, 1)
            """.trimIndent(),
            mapOf("id" to exerciseId, "uid" to uid, "now" to now),
        )
        jdbcTemplate.update(
            "INSERT INTO change_log (user_id, entity_type, entity_id, operation) VALUES (:uid, 'EXERCISE', :id, 'UPSERT')",
            mapOf("uid" to uid, "id" to exerciseId),
        )

        val body = mockMvc.perform(get("/v1/sync/pull").header("Authorization", "Bearer ${bearerToken(uid)}"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        val response = objectMapper.readValue(body, PullResponseDto::class.java)

        assertEquals(1, response.exercises.size)
        assertEquals(exerciseId, response.exercises.single().id)
    }
}

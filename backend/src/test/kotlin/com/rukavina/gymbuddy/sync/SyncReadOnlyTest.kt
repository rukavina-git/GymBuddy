package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * gymbuddy.sync.read-only=true (SYNC_READ_ONLY) must reject every push
 * with 503 without touching the database, while leaving the rest of the
 * API (verified here via /v1/reference/version, a proxy for "not sync
 * push") unaffected. Group F's pull endpoint doesn't exist yet (out of
 * scope - Group G), so "permits pull" can't be exercised directly here;
 * this test covers the push-rejection half of point 10 and the
 * "everything else still works" half via a non-sync endpoint.
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
    }

    @Test
    fun `other endpoints keep working when sync is read-only`() {
        val uid = "read-only-other-${System.nanoTime()}"
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v1/reference/version")
                .header("Authorization", "Bearer ${bearerToken(uid)}"),
        ).andExpect(status().isOk)
    }
}

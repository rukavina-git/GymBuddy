package com.rukavina.gymbuddy.feedback

import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.sync.AccountDataDeleter
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

/** POST /v1/feedback against real Postgres, with the real security chain. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class FeedbackControllerTest : AbstractPostgresIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
    @Autowired private lateinit var accountDataDeleter: AccountDataDeleter
    @Autowired private lateinit var rateLimiter: FeedbackRateLimiter

    @MockitoBean private lateinit var notifier: FeedbackNotifier

    private fun unique(label: String) = "$label-${System.nanoTime()}"

    // Each test gets its own guest IP so the shared limiter doesn't leak between tests.
    private fun send(body: String, uid: String? = null, ip: String = unique("ip")): ResultActions {
        val request = post("/v1/feedback")
            .contentType(MediaType.APPLICATION_JSON)
            .header("X-Real-IP", ip)
            .content(body)
        uid?.let { request.header("Authorization", "Bearer ${TestJwtBuilder().subject(it).build()}") }
        return mockMvc.perform(request)
    }

    private fun json(id: UUID = UUID.randomUUID(), message: String = "Love the rest timer", extra: String = "") =
        """{"id":"$id","message":${quote(message)},"appVersion":"0.2.4","osVersion":"Android 15","deviceModel":"samsung SM-S731B","createdAt":1764000000000$extra}"""

    private fun quote(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun row(id: UUID): Map<String, Any?>? = jdbcTemplate.queryForList(
        "SELECT user_id, message, app_version, device_model FROM feedback WHERE id = :id",
        MapSqlParameterSource("id", id),
    ).singleOrNull()

    @Test
    fun `guest feedback is stored without an account and pings the owner`() {
        val id = UUID.randomUUID()
        send(json(id, "  Love the rest timer  ")).andExpect(status().isNoContent)

        val row = row(id)!!
        assertNull(row["user_id"])
        assertEquals("Love the rest timer", row["message"])
        assertEquals("samsung SM-S731B", row["device_model"])
        verify(notifier, times(1)).newFeedback()
    }

    @Test
    fun `signed-in feedback is linked to the account`() {
        val uid = unique("user")
        val id = UUID.randomUUID()
        send(json(id), uid = uid).andExpect(status().isNoContent)
        assertEquals(uid, row(id)!!["user_id"])
    }

    @Test
    fun `an invalid token is treated as a guest, not rejected`() {
        val id = UUID.randomUUID()
        mockMvc.perform(
            post("/v1/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Real-IP", unique("ip"))
                .header("Authorization", "Bearer not-a-jwt")
                .content(json(id)),
        ).andExpect(status().isNoContent)
        assertNull(row(id)!!["user_id"])
    }

    @Test
    fun `a retried id stores once and pings once`() {
        val id = UUID.randomUUID()
        val ip = unique("ip")
        send(json(id, "first"), ip = ip).andExpect(status().isNoContent)
        send(json(id, "second"), ip = ip).andExpect(status().isNoContent)

        assertEquals("first", row(id)!!["message"])
        verify(notifier, times(1)).newFeedback()
    }

    @Test
    fun `blank, too long, bad id and missing createdAt are 400`() {
        val ip = unique("ip")
        send(json(message = "   "), ip = ip).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
        send(json(message = "x".repeat(FEEDBACK_MAX_CHARS + 1)), ip = unique("ip")).andExpect(status().isBadRequest)
        send("""{"id":"nope","message":"hi","createdAt":1}""", ip = unique("ip")).andExpect(status().isBadRequest)
        send("""{"id":"${UUID.randomUUID()}","message":"hi"}""", ip = unique("ip")).andExpect(status().isBadRequest)
        send(json(extra = ""","deviceModel":"${"m".repeat(65)}""""), ip = unique("ip")).andExpect(status().isBadRequest)
        verify(notifier, never()).newFeedback()
    }

    @Test
    fun `exactly the cap is accepted`() {
        send(json(message = "x".repeat(FEEDBACK_MAX_CHARS))).andExpect(status().isNoContent)
    }

    @Test
    fun `the sixth message in an hour from one guest IP is 429 with Retry-After`() {
        val ip = unique("ip")
        repeat(5) { send(json(), ip = ip).andExpect(status().isNoContent) }
        send(json(), ip = ip)
            .andExpect(status().isTooManyRequests)
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error").value("RATE_LIMITED"))
        // Another guest is unaffected.
        send(json()).andExpect(status().isNoContent)
    }

    @Test
    fun `signed-in users are limited per account, not per IP`() {
        val uid = unique("user")
        repeat(5) { send(json(), uid = uid).andExpect(status().isNoContent) }
        send(json(), uid = uid).andExpect(status().isTooManyRequests)
    }

    @Test
    fun `the limit frees up after an hour`() {
        val limiter = FeedbackRateLimiter(maxPerHour = 1)
        val start = Instant.parse("2026-11-25T10:00:00Z")
        limiter.clock = Clock.fixed(start, ZoneOffset.UTC)
        assertEquals(true, limiter.tryConsume("k").allowed)
        assertEquals(false, limiter.tryConsume("k").allowed)
        limiter.clock = Clock.fixed(start.plus(Duration.ofMinutes(60)), ZoneOffset.UTC)
        assertEquals(true, limiter.tryConsume("k").allowed)
    }

    @Test
    fun `feedback is deleted with the account, guest feedback stays`() {
        val uid = unique("user")
        val mine = UUID.randomUUID()
        val guest = UUID.randomUUID()
        send(json(mine), uid = uid).andExpect(status().isNoContent)
        send(json(guest)).andExpect(status().isNoContent)

        accountDataDeleter.deleteAllDataFor(uid)

        assertNull(row(mine))
        assertNotNull(row(guest))
    }
}

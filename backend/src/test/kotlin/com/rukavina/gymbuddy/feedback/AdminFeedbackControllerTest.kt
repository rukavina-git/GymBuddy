package com.rukavina.gymbuddy.feedback

import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** GET/PATCH /v1/admin/feedback with the real security chain; "admin-1" is the only admin (test profile). */
// admin-1 comes from src/test/resources/application-test.yml (no per-class
// properties: a new context would exhaust the shared Postgres's connections).
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class AdminFeedbackControllerTest : AbstractPostgresIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
    @Autowired private lateinit var retention: FeedbackRetentionService

    private val mapper = ObjectMapper()

    // Rows this class lists sit in 2100+, newer than anything other tests store.
    private val base = Instant.parse("2100-01-01T00:00:00Z")

    @BeforeEach
    fun clearFutureRows() {
        jdbcTemplate.update("DELETE FROM feedback WHERE received_at >= :t", MapSqlParameterSource("t", Timestamp.from(base)))
    }

    private fun insert(
        receivedAt: Instant,
        status: String = "open",
        userId: String? = null,
        message: String = "msg",
        id: UUID = UUID.randomUUID(),
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO feedback (id, user_id, message, app_version, os_version, device_model, created_at, received_at, status, updated_at)
            VALUES (:id, :uid, :msg, '0.2.4', 'Android 15', 'samsung SM-S731B', :t, :t, :status, :t)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("id", id)
                .addValue("uid", userId)
                .addValue("msg", message)
                .addValue("t", Timestamp.from(receivedAt))
                .addValue("status", status),
        )
        return id
    }

    private fun MockHttpServletRequestBuilder.asUser(uid: String?) =
        apply { uid?.let { header("Authorization", "Bearer ${TestJwtBuilder().subject(it).build()}") } }

    private fun list(query: String = "", uid: String? = "admin-1"): ResultActions =
        mockMvc.perform(get("/v1/admin/feedback$query").asUser(uid))

    private fun patchFeedback(id: Any, body: String, uid: String? = "admin-1"): ResultActions =
        mockMvc.perform(
            patch("/v1/admin/feedback/$id").asUser(uid).contentType(MediaType.APPLICATION_JSON).content(body),
        )

    private fun count(status: String): Long = jdbcTemplate.queryForObject(
        "SELECT count(*) FROM feedback WHERE status = :s", MapSqlParameterSource("s", status), Long::class.java,
    )!!

    @Test
    fun `no token is 401, a non-admin token is 403, on both routes`() {
        list(uid = null).andExpect(status().isUnauthorized).andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
        list(uid = "someone-else").andExpect(status().isForbidden).andExpect(jsonPath("$.error").value("FORBIDDEN"))
        val id = insert(base)
        patchFeedback(id, """{"status":"spam"}""", uid = null).andExpect(status().isUnauthorized)
        patchFeedback(id, """{"status":"spam"}""", uid = "someone-else").andExpect(status().isForbidden)
        assertEquals("open", jdbcTemplate.queryForObject("SELECT status FROM feedback WHERE id = :id", MapSqlParameterSource("id", id), String::class.java))
    }

    @Test
    fun `an invalid token is 401, not treated as a guest`() {
        mockMvc.perform(get("/v1/admin/feedback").header("Authorization", "Bearer not-a-jwt"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `lists newest first with account, filter, counts and a cursor`() {
        val oldest = insert(base.plusSeconds(1), userId = "user-a", message = "first\nline two")
        val middle = insert(base.plusSeconds(2), status = "solved")
        val newest = insert(base.plusSeconds(3))

        list("?limit=2").andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].id").value(newest.toString()))
            .andExpect(jsonPath("$.items[0].account").value("guest"))
            .andExpect(jsonPath("$.items[0].receivedAt").value(base.plusSeconds(3).toEpochMilli()))
            .andExpect(jsonPath("$.items[1].id").value(middle.toString()))
            .andExpect(jsonPath("$.counts.open").value(count("open")))
            .andExpect(jsonPath("$.counts.solved").value(count("solved")))
            .andExpect(jsonPath("$.counts.wont_fix").value(count("wont_fix")))
            .andExpect(jsonPath("$.counts.spam").value(count("spam")))

        val firstPage = mapper.readTree(list("?limit=2").andReturn().response.contentAsString)
        val cursor = firstPage.get("nextCursor").asText()
        list("?limit=2&cursor=$cursor").andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(oldest.toString()))
            .andExpect(jsonPath("$.items[0].account").value("user-a"))
            .andExpect(jsonPath("$.items[0].message").value("first\nline two"))
            .andExpect(jsonPath("$.items[0].appVersion").value("0.2.4"))

        list("?status=solved&limit=1").andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].id").value(middle.toString()))
    }

    @Test
    fun `the last page has no next cursor`() {
        insert(base.plusSeconds(1), status = "spam")
        // Other classes' rows are older than 2100, so the newest spam row is ours;
        // paging through every spam row must end with a null cursor.
        var cursor: String? = null
        var pages = 0
        do {
            val query = "?status=spam&limit=100" + (cursor?.let { "&cursor=$it" } ?: "")
            val page = mapper.readTree(list(query).andReturn().response.contentAsString)
            cursor = page.get("nextCursor").takeUnless { it.isNull }?.asText()
            pages++
        } while (cursor != null && pages < 100)
        assertNull(cursor)
    }

    @Test
    fun `bad status, bad cursor and bad limit are 400`() {
        list("?status=done").andExpect(status().isBadRequest).andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
        list("?cursor=not-a-cursor").andExpect(status().isBadRequest)
        list("?limit=abc").andExpect(status().isBadRequest)
    }

    @Test
    fun `patch sets status and note, then clears the note`() {
        val id = insert(base.plusSeconds(1))
        patchFeedback(id, """{"status":"solved","note":"  fixed in 0.2.5 "}""").andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value("solved"))
            .andExpect(jsonPath("$.note").value("fixed in 0.2.5"))
        // updated_at = now(), set by the patch (the row was stored with 2100).
        patchFeedback(id, """{"note":"later"}""").andExpect(status().isOk)
            .andExpect(jsonPath("$.note").value("later"))
        val updatedAt = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM feedback WHERE id = :id", MapSqlParameterSource("id", id), Timestamp::class.java,
        )!!.toInstant()
        assertTrue(updatedAt.isBefore(base))

        // status unchanged by a note-only patch
        patchFeedback(id, """{"note":""}""").andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("solved"))
            .andExpect(jsonPath("$.note").value(nullValue()))
    }

    @Test
    fun `patch on an unknown id is 404, bad bodies are 400`() {
        patchFeedback(UUID.randomUUID(), """{"status":"spam"}""").andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("NOT_FOUND"))
        val id = insert(base.plusSeconds(1))
        patchFeedback(id, """{"status":"done"}""").andExpect(status().isBadRequest)
        patchFeedback(id, """{"note":"${"n".repeat(FEEDBACK_NOTE_MAX_CHARS + 1)}"}""").andExpect(status().isBadRequest)
        patchFeedback(id, """{}""").andExpect(status().isBadRequest)
        patchFeedback("nope", """{"status":"spam"}""").andExpect(status().isBadRequest)
        patchFeedback(id, """{"note":"${"n".repeat(FEEDBACK_NOTE_MAX_CHARS)}"}""").andExpect(status().isOk)
    }

    @Test
    fun `retention deletes feedback older than two years, any status`() {
        val now = Instant.parse("2026-11-25T12:00:00Z")
        val old = insert(now.minus(Duration.ofDays(731)), status = "solved")
        val oldOpen = insert(now.minus(Duration.ofDays(800)))
        val recent = insert(now.minus(Duration.ofDays(729)))

        retention.pruneExpired(now)

        fun exists(id: UUID) = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM feedback WHERE id = :id", MapSqlParameterSource("id", id), Long::class.java,
        )!! == 1L
        assertEquals(false, exists(old))
        assertEquals(false, exists(oldOpen))
        assertEquals(true, exists(recent))
        jdbcTemplate.update("DELETE FROM feedback WHERE id = :id", MapSqlParameterSource("id", recent))
    }
}

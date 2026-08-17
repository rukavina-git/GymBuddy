package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PullResponseDto
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * End-to-end coverage of GET /v1/sync/pull against a real Postgres
 * (Testcontainers). Less failure-sensitive than push (Group F), but the
 * pagination path can silently skip data if it's wrong, so several of
 * these deliberately force a real multi-page walk (limit smaller than
 * the number of pending changes) rather than asserting against a single
 * page - see the pagination-boundary and concurrent-write tests below.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class SyncPullTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    private fun newUid(label: String) = "$label-${System.nanoTime()}"

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

    private fun pull(uid: String, cursor: String? = null, limit: Int? = null): PullResponseDto {
        var request = get("/v1/sync/pull").header("Authorization", "Bearer ${bearerToken(uid)}")
        cursor?.let { request = request.param("cursor", it) }
        limit?.let { request = request.param("limit", it.toString()) }
        val body = mockMvc.perform(request).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readValue(body, PullResponseDto::class.java)
    }

    private fun pushExercise(uid: String, name: String = "Exercise ${SyncTestFixtures.uuid()}") =
        push(uid, PushRequestDto(exercises = listOf(SyncTestFixtures.exercise(name = name)))).results.single()

    // 1-3. no-cursor pull sees everything pushed so far; resuming with the returned cursor sees only what's new.

    @Test
    fun `full sync with no cursor returns everything pushed, resuming sees nothing then exactly one new change`() {
        val uid = newUid("basic-flow")
        val session = SyncTestFixtures.workoutSession()
        val exercise = SyncTestFixtures.exercise()
        val template = SyncTestFixtures.workoutTemplate()
        push(uid, PushRequestDto(workoutSessions = listOf(session), exercises = listOf(exercise), workoutTemplates = listOf(template)))

        val first = pull(uid)
        assertEquals(1, first.workoutSessions.size)
        assertEquals(1, first.exercises.size)
        assertEquals(1, first.workoutTemplates.size)
        assertFalse(first.hasMore)

        val second = pull(uid, cursor = first.nextCursor)
        assertTrue(second.workoutSessions.isEmpty())
        assertTrue(second.exercises.isEmpty())
        assertTrue(second.workoutTemplates.isEmpty())
        assertFalse(second.hasMore)
        assertEquals(first.nextCursor, second.nextCursor, "an unchanged cursor should be echoed back unchanged")

        val newExercise = SyncTestFixtures.exercise()
        push(uid, PushRequestDto(exercises = listOf(newExercise)))
        val third = pull(uid, cursor = second.nextCursor)
        assertEquals(1, third.exercises.size)
        assertEquals(newExercise.id, third.exercises.single().id)
        assertTrue(third.workoutSessions.isEmpty())
        assertTrue(third.workoutTemplates.isEmpty())
    }

    // 4. an entity changed five times since the cursor returns once.

    @Test
    fun `an entity changed five times since the cursor is returned exactly once, with its latest state`() {
        val uid = newUid("dedup")
        val baseline = pull(uid) // establishes a cursor with nothing pending

        val exercise = SyncTestFixtures.exercise(name = "v1")
        var applied = push(uid, PushRequestDto(exercises = listOf(exercise))).results.single()
        repeat(4) { i ->
            val edit = exercise.copy(name = "v${i + 2}", revision = applied.revision!!)
            applied = push(uid, PushRequestDto(exercises = listOf(edit))).results.single()
        }

        val page = pull(uid, cursor = baseline.nextCursor)
        assertEquals(1, page.exercises.size, "five changes to one entity must collapse to one entry")
        val result = page.exercises.single()
        assertEquals(exercise.id, result.id)
        assertEquals("v5", result.name)
        assertEquals(5, result.revision)
    }

    // 5. a deleted entity is returned with a non-null deletedAt.

    @Test
    fun `a deleted entity is returned as a normal entity with a non-null deletedAt`() {
        val uid = newUid("deleted")
        val baseline = pull(uid)

        val session = SyncTestFixtures.workoutSession()
        val created = push(uid, PushRequestDto(workoutSessions = listOf(session))).results.single()
        push(uid, PushRequestDto(workoutSessions = listOf(session.copy(revision = created.revision!!, deletedAt = 1L))))

        val page = pull(uid, cursor = baseline.nextCursor)
        assertEquals(1, page.workoutSessions.size, "a delete is one entity, in the normal array, not a separate list")
        val result = page.workoutSessions.single()
        assertEquals(session.id, result.id)
        assertNotNull(result.deletedAt)
    }

    // 6. pagination across a page boundary delivers nothing twice and skips nothing.

    @Test
    fun `pagination across multiple page boundaries delivers every entity exactly once`() {
        val uid = newUid("pagination")
        val baseline = pull(uid)

        val pushed = (1..5).map { pushExercise(uid) }
        assertEquals(5, pushed.size)

        val seen = mutableListOf<String>()
        var cursor = baseline.nextCursor
        var pageCount = 0
        var hasMore = true
        while (hasMore) {
            val page = pull(uid, cursor = cursor, limit = 2)
            pageCount++
            assertTrue(page.exercises.size <= 2, "must never exceed the requested page size")
            seen += page.exercises.map { it.id }
            cursor = page.nextCursor
            hasMore = page.hasMore
        }

        assertTrue(pageCount >= 3, "5 entities at limit=2 must take at least 3 requests - a single-page result would not exercise a real boundary")
        assertEquals(5, seen.size, "no entity delivered twice")
        assertEquals(5, seen.toSet().size, "no entity skipped")

        // The walk must have actually stopped: cursor is now stable and empty.
        val afterEnd = pull(uid, cursor = cursor, limit = 2)
        assertTrue(afterEnd.exercises.isEmpty())
        assertFalse(afterEnd.hasMore)
    }

    // 7. a write concurrent with pagination is not lost.

    @Test
    fun `an entity pushed in between two page fetches is not lost`() {
        val uid = newUid("concurrent-write")
        val baseline = pull(uid)

        val firstBatch = (1..4).map { pushExercise(uid) }
        assertEquals(4, firstBatch.size)

        // First page of a limit=2 walk over the first batch.
        val page1 = pull(uid, cursor = baseline.nextCursor, limit = 2)
        assertEquals(2, page1.exercises.size)
        assertTrue(page1.hasMore, "4 pushed entities at limit=2 must not fit on one page")

        // A write lands strictly between fetching page 1 and page 2 - this is
        // the scenario the design notes call out: change_log.seq is
        // assigned at insert time, so this new row gets a seq higher than
        // anything already scanned, landing safely in a later page rather
        // than being silently skipped by the in-flight pagination.
        val concurrentExercise = pushExercise(uid)

        val remaining = mutableListOf<String>()
        var cursor = page1.nextCursor
        var hasMore = page1.hasMore
        var pagesAfterFirst = 0
        while (hasMore) {
            val page = pull(uid, cursor = cursor, limit = 2)
            pagesAfterFirst++
            remaining += page.exercises.map { it.id }
            cursor = page.nextCursor
            hasMore = page.hasMore
        }

        assertTrue(pagesAfterFirst >= 2, "must fetch at least 2 more pages to also exercise a real boundary after the concurrent write")
        val allSeen = page1.exercises.map { it.id } + remaining
        val expectedIds = (firstBatch.map { it.entityId } + concurrentExercise.entityId).toSet()
        assertEquals(expectedIds, allSeen.toSet(), "the concurrently-pushed entity must appear exactly once, nothing else missing or duplicated")
        assertEquals(allSeen.size, allSeen.toSet().size, "no duplicates across the walk")
    }

    // 8. an expired cursor returns 410 with CURSOR_EXPIRED.

    @Test
    fun `a cursor older than the oldest retained change_log row for this user returns 410 CURSOR_EXPIRED`() {
        val uid = newUid("expired")
        val first = pushExercise(uid)
        val afterFirst = pull(uid)
        pushExercise(uid) // ensures the user has a change_log row newer than the one we're about to delete

        // Nothing prunes change_log yet (Group H doesn't exist) - simulate
        // what retention will eventually do so the 410 path is exercised now.
        jdbcTemplate.update(
            "DELETE FROM change_log WHERE user_id = :uid AND entity_id = :entityId",
            mapOf("uid" to uid, "entityId" to first.entityId),
        )

        mockMvc.perform(
            get("/v1/sync/pull")
                .header("Authorization", "Bearer ${bearerToken(uid)}")
                .param("cursor", afterFirst.nextCursor),
        )
            .andExpect(status().isGone)
            .andExpect(jsonPath("$.error").value("CURSOR_EXPIRED"))
            .andExpect(jsonPath("$.traceId").exists())
    }

    // 9. a malformed cursor returns 400, not 500.

    @Test
    fun `a malformed cursor returns 400 VALIDATION_FAILED, not 500`() {
        val uid = newUid("malformed")
        mockMvc.perform(
            get("/v1/sync/pull")
                .header("Authorization", "Bearer ${bearerToken(uid)}")
                .param("cursor", "not-a-real-cursor!!!"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.traceId").exists())
    }

    // 10. user A's cursor never returns user B's entities.

    @Test
    fun `user A's cursor never returns user B's entities, even if reused while authenticated as B`() {
        val userA = newUid("user-a")
        val userB = newUid("user-b")

        // B has real history predating A's cursor, so reusing A's cursor
        // exercises ownership isolation specifically - not the unrelated
        // "cursor predates this user's own first-ever activity" case
        // (isCursorExpired's cursorSeq <= 0 exemption), which would
        // otherwise make this scenario indistinguishable from a brand-new
        // user's cold start.
        val exerciseB = pushExercise(userB)
        pushExercise(userA)
        val cursorA = pull(userA).nextCursor

        val bReusingAsCursor = pull(userB, cursor = cursorA)
        assertTrue(bReusingAsCursor.exercises.none { it.id != exerciseB.entityId }, "must never see user A's exercise")

        val bFullSync = pull(userB)
        assertTrue(bFullSync.exercises.none { it.id != exerciseB.entityId }, "must never see user A's exercise")
    }

    // 11. a full initial sync returns entities that predate the change log.

    @Test
    fun `a full initial sync returns an entity whose change_log row no longer exists`() {
        val uid = newUid("predates-log")
        val exercise = pushExercise(uid)

        jdbcTemplate.update(
            "DELETE FROM change_log WHERE user_id = :uid AND entity_id = :entityId",
            mapOf("uid" to uid, "entityId" to exercise.entityId),
        )
        val remaining = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM change_log WHERE user_id = :uid",
            mapOf("uid" to uid),
            Int::class.java,
        )
        assertEquals(0, remaining, "precondition: this user has no change_log rows left at all")

        val fullSync = pull(uid)
        assertEquals(1, fullSync.exercises.size, "full sync must read current table state, not replay change_log")
        assertEquals(exercise.entityId, fullSync.exercises.single().id)
    }
}

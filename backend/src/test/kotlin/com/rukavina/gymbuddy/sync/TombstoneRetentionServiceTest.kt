package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PullResponseDto
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * gymbuddy.sync.tombstone-retention-days=0 (point 2: "the window is
 * configuration, not a constant") means anything tombstoned before this
 * test's own retention call is immediately eligible - no backdating of
 * deleted_at needed, and the window override is exercised for real
 * rather than assumed. A dedicated Spring context, since the override
 * would otherwise apply to every other test sharing the container.
 *
 * The last two tests are Group H's actual reason for existing: closing
 * the cursor-expiry gap from Group G. Both cursors used are real,
 * server-issued cursors from actual pull calls - never constructed -
 * consistent with the rest of this codebase's "opaque cursor" testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
@TestPropertySource(properties = ["gymbuddy.sync.tombstone-retention-days=0"])
class TombstoneRetentionServiceTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var tombstoneRetentionService: TombstoneRetentionService

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

    private fun pull(uid: String, cursor: String? = null): PullResponseDto {
        var request = get("/v1/sync/pull").header("Authorization", "Bearer ${bearerToken(uid)}")
        cursor?.let { request = request.param("cursor", it) }
        val body = mockMvc.perform(request).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readValue(body, PullResponseDto::class.java)
    }

    private fun changeLogCount(uid: String, entityId: String): Int =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM change_log WHERE user_id = :uid AND entity_id = :entityId",
            mapOf("uid" to uid, "entityId" to entityId),
            Int::class.java,
        )!!

    private fun watermark(uid: String): Long? =
        jdbcTemplate.query(
            "SELECT retention_floor_seq FROM sync_retention_watermark WHERE user_id = :uid",
            mapOf("uid" to uid),
        ) { rs, _ -> rs.getLong("retention_floor_seq") }.firstOrNull()

    /**
     * A: created, then deleted (tombstoned) - immediately prunable under
     * the 0-day window. B: created and left live, never deleted. cursorAfterCreate
     * sits right after A's create, before its delete; cursorFinal sits
     * after B's create, past everything that will get pruned.
     */
    private data class Scenario(val uid: String, val tombstonedId: String, val liveId: String, val cursorAfterCreate: String, val cursorFinal: String)

    private fun buildScenario(label: String): Scenario {
        val uid = newUid(label)
        val tombstoned = SyncTestFixtures.exercise(name = "tombstoned")
        val created = push(uid, PushRequestDto(exercises = listOf(tombstoned))).results.single()
        val cursorAfterCreate = pull(uid).nextCursor

        push(uid, PushRequestDto(exercises = listOf(tombstoned.copy(revision = created.revision!!, deletedAt = 1L))))
        val live = SyncTestFixtures.exercise(name = "live")
        push(uid, PushRequestDto(exercises = listOf(live)))
        val cursorFinal = pull(uid, cursor = cursorAfterCreate).nextCursor

        return Scenario(uid, tombstoned.id, live.id, cursorAfterCreate, cursorFinal)
    }

    @Test
    fun `retention removes tombstones past the window and their change_log rows, and leaves live data alone`() {
        val scenario = buildScenario("retention-basic")

        tombstoneRetentionService.pruneExpiredTombstones()

        val tombstonedRemaining = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM exercises WHERE id = :id::uuid",
            mapOf("id" to scenario.tombstonedId),
            Int::class.java,
        )
        assertEquals(0, tombstonedRemaining, "the tombstoned entity itself must be hard-deleted")
        assertEquals(0, changeLogCount(scenario.uid, scenario.tombstonedId), "its change_log rows must go with it")

        val liveRemaining = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM exercises WHERE id = :id::uuid",
            mapOf("id" to scenario.liveId),
            Int::class.java,
        )
        assertEquals(1, liveRemaining, "live (never-deleted) data must be untouched")
        assertTrue(changeLogCount(scenario.uid, scenario.liveId) > 0, "the live entity's own change_log history must be untouched")
    }

    @Test
    fun `the watermark advances when pruning occurs`() {
        val scenario = buildScenario("watermark-advance")
        assertEquals(null, watermark(scenario.uid), "precondition: nothing pruned yet, no watermark row")

        tombstoneRetentionService.pruneExpiredTombstones()

        val floor = watermark(scenario.uid)
        assertTrue(floor != null && floor > 0, "expected a watermark row after pruning, got $floor")
    }

    @Test
    fun `a cursor older than the watermark returns 410 CURSOR_EXPIRED`() {
        val scenario = buildScenario("expired-cursor")
        tombstoneRetentionService.pruneExpiredTombstones()

        mockMvc.perform(
            get("/v1/sync/pull")
                .header("Authorization", "Bearer ${bearerToken(scenario.uid)}")
                .param("cursor", scenario.cursorAfterCreate),
        )
            .andExpect(status().isGone)
            .andExpect(jsonPath("$.error").value("CURSOR_EXPIRED"))
    }

    @Test
    fun `a valid cursor within the retained range does not return 410 even after pruning`() {
        val scenario = buildScenario("not-expired-cursor")
        tombstoneRetentionService.pruneExpiredTombstones()

        val response = pull(scenario.uid, cursor = scenario.cursorFinal)
        assertEquals(false, response.hasMore)
    }
}

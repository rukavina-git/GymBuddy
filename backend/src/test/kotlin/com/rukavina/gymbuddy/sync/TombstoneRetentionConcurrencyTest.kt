package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PullResponseDto
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.domain.SyncStatus
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

/**
 * Group H's "safe to run concurrently with sync", tested rather than
 * argued. TombstoneRetentionService and every push service take the
 * same pg_advisory_xact_lock(hashtext(uid)), so for one user the two
 * must serialise - never interleave - and whichever runs first, the end
 * state is the same:
 *
 * - the expired tombstone and its change_log rows are gone,
 * - the concurrently pushed entity is APPLIED with exactly one
 *   change_log row,
 * - the watermark sits at the tombstone's last seq, below the pushed
 *   entity's seq, so a fresh pull still delivers the pushed entity.
 *
 * Same 0-day window and property set as TombstoneRetentionServiceTest,
 * so Spring reuses that test's cached context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
@TestPropertySource(properties = ["gymbuddy.sync.tombstone-retention-days=0"])
class TombstoneRetentionConcurrencyTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var dataSource: DataSource

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

    private fun pull(uid: String): PullResponseDto {
        val body = mockMvc.perform(get("/v1/sync/pull").header("Authorization", "Bearer ${bearerToken(uid)}"))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        return objectMapper.readValue(body, PullResponseDto::class.java)
    }

    private fun changeLogSeqs(uid: String, entityId: String): List<Long> =
        jdbcTemplate.queryForList(
            "SELECT seq FROM change_log WHERE user_id = :uid AND entity_id = :entityId ORDER BY seq",
            mapOf("uid" to uid, "entityId" to entityId),
            Long::class.java,
        )

    private fun exerciseExists(id: String): Boolean =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM exercises WHERE id = :id::uuid",
            mapOf("id" to id),
            Int::class.java,
        ) == 1

    private fun watermark(uid: String): Long? =
        jdbcTemplate.query(
            "SELECT retention_floor_seq FROM sync_retention_watermark WHERE user_id = :uid",
            mapOf("uid" to uid),
        ) { rs, _ -> rs.getLong("retention_floor_seq") }.firstOrNull()

    /** Sessions blocked waiting on this user's advisory lock. */
    private fun advisoryWaiters(uid: String): Int =
        jdbcTemplate.queryForObject(
            """
            SELECT count(*) FROM pg_locks
            WHERE locktype = 'advisory' AND NOT granted
              AND objid::bigint = (hashtext(:uid)::bigint & 4294967295)
            """.trimIndent(),
            mapOf("uid" to uid),
            Int::class.java,
        )!!

    private data class Setup(val uid: String, val tombstoneId: String, val tombstoneLastSeq: Long)

    /** A user with one tombstoned custom exercise, prunable under the 0-day window. */
    private fun userWithExpiredTombstone(label: String): Setup {
        val uid = newUid(label)
        val tombstone = SyncTestFixtures.exercise(name = "to be pruned")
        val created = push(uid, PushRequestDto(exercises = listOf(tombstone))).results.single()
        push(uid, PushRequestDto(exercises = listOf(tombstone.copy(revision = created.revision!!, deletedAt = 1L))))
        // cutoff is `deleted_at < now`; make sure "now" has moved on.
        Thread.sleep(5)
        return Setup(uid, tombstone.id, changeLogSeqs(uid, tombstone.id).last())
    }

    private fun assertConsistentEndState(setup: Setup, pushedId: String) {
        assertTrue(!exerciseExists(setup.tombstoneId), "tombstone must be hard-deleted")
        assertEquals(emptyList<Long>(), changeLogSeqs(setup.uid, setup.tombstoneId), "tombstone's change_log rows must be gone")

        assertTrue(exerciseExists(pushedId), "pushed entity must be stored")
        val pushedSeqs = changeLogSeqs(setup.uid, pushedId)
        assertEquals(1, pushedSeqs.size, "pushed entity must have exactly one change_log row")

        assertEquals(setup.tombstoneLastSeq, watermark(setup.uid), "watermark must be the pruned tombstone's last seq")
        assertTrue(pushedSeqs.single() > setup.tombstoneLastSeq, "pushed row must sit above the watermark")

        val pulledIds = pull(setup.uid).exercises.map { it.id }
        assertEquals(listOf(pushedId), pulledIds, "a fresh pull delivers the pushed entity and nothing of the pruned one")
    }

    @Test
    fun `retention and push for the same user serialise on the advisory lock and both complete cleanly`() {
        val setup = userWithExpiredTombstone("retention-vs-push-gated")
        val pushed = SyncTestFixtures.exercise(name = "pushed during retention")
        val pool = Executors.newFixedThreadPool(2)

        // Hold the user's lock on a separate connection so both
        // contenders are guaranteed to be in flight at the same time,
        // queued on the same lock - not merely launched close together.
        dataSource.connection.use { gate ->
            gate.autoCommit = false
            gate.prepareStatement("SELECT pg_advisory_xact_lock(hashtext(?))").use {
                it.setString(1, setup.uid)
                it.execute()
            }

            try {
                val retention = CompletableFuture.supplyAsync({ tombstoneRetentionService.pruneExpiredTombstones() }, pool)
                val push = CompletableFuture.supplyAsync({ push(setup.uid, PushRequestDto(exercises = listOf(pushed))) }, pool)

                val deadline = System.currentTimeMillis() + 10_000
                while (advisoryWaiters(setup.uid) < 2 && System.currentTimeMillis() < deadline) Thread.sleep(20)
                assertEquals(2, advisoryWaiters(setup.uid), "retention and push must both be blocked on the same per-user lock")
                assertTrue(!retention.isDone && !push.isDone, "neither may make progress while the lock is held")

                gate.commit()

                val summary = retention.get(20, TimeUnit.SECONDS)
                val result = push.get(20, TimeUnit.SECONDS).results.single()

                assertTrue(summary.entitiesDeleted >= 1)
                assertEquals(SyncStatus.APPLIED, result.status)
                assertEquals(1, result.revision)
            } finally {
                pool.shutdownNow()
            }
        }

        assertConsistentEndState(setup, pushed.id)
    }

    @Test
    fun `unsynchronised retention-vs-push races always converge to the same end state`() {
        repeat(10) { round ->
            val setup = userWithExpiredTombstone("retention-vs-push-race-$round")
            val pushed = SyncTestFixtures.exercise(name = "race $round")
            val pool = Executors.newFixedThreadPool(2)
            val start = CountDownLatch(1)

            try {
                val retention = CompletableFuture.supplyAsync({
                    start.await()
                    tombstoneRetentionService.pruneExpiredTombstones()
                }, pool)
                val push = CompletableFuture.supplyAsync({
                    start.await()
                    push(setup.uid, PushRequestDto(exercises = listOf(pushed)))
                }, pool)
                start.countDown()

                retention.get(20, TimeUnit.SECONDS)
                assertEquals(SyncStatus.APPLIED, push.get(20, TimeUnit.SECONDS).results.single().status, "round $round")
            } finally {
                pool.shutdownNow()
            }

            assertConsistentEndState(setup, pushed.id)
        }
    }
}

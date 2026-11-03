package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PullResponseDto
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.domain.EntityType
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** Plan 6 step 04: BodyWeightEntry push and pull against a real Postgres. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class BodyWeightSyncTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var invariantChecker: SyncInvariantChecker

    private fun newUid(label: String) = "$label-${System.nanoTime()}"

    private fun bearerToken(uid: String): String = TestJwtBuilder().subject(uid).build()

    private fun push(uid: String, request: PushRequestDto): PushResponseDto {
        val body = mockMvc.perform(
            post("/v1/sync/push")
                .header("Authorization", "Bearer ${bearerToken(uid)}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        ).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readValue(body, PushResponseDto::class.java)
    }

    private fun pull(uid: String, cursor: String? = null): PullResponseDto {
        var request = get("/v1/sync/pull").header("Authorization", "Bearer ${bearerToken(uid)}")
        cursor?.let { request = request.param("cursor", it) }
        val body = mockMvc.perform(request).andExpect(status().isOk).andReturn().response.contentAsString
        return objectMapper.readValue(body, PullResponseDto::class.java)
    }

    private fun storedWeight(id: String): Float? = jdbcTemplate.query(
        "SELECT weight_kg FROM body_weight_entries WHERE id = :id::uuid",
        mapOf("id" to id),
    ) { rs, _ -> rs.getFloat(1) }.firstOrNull()

    @Test
    fun `create, update and delete apply with server revisions and change_log rows`() {
        val uid = newUid("bw")
        val entry = SyncTestFixtures.bodyWeightEntry(weightKg = 82f)

        val created = push(uid, PushRequestDto(bodyWeightEntries = listOf(entry))).results.single()
        assertEquals(EntityType.BODY_WEIGHT_ENTRY, created.entityType)
        assertEquals(SyncStatus.APPLIED, created.status)
        assertEquals(1, created.revision)

        val updated = push(uid, PushRequestDto(bodyWeightEntries = listOf(entry.copy(weightKg = 81.5f, revision = 1)))).results.single()
        assertEquals(SyncStatus.APPLIED, updated.status)
        assertEquals(2, updated.revision)
        assertEquals(81.5f, storedWeight(entry.id))

        val deleted = push(uid, PushRequestDto(bodyWeightEntries = listOf(entry.copy(weightKg = 81.5f, revision = 2, deletedAt = 1L)))).results.single()
        assertEquals(SyncStatus.APPLIED, deleted.status)
        assertEquals(3, deleted.revision)
        // The checker scans the shared test database; only this entry is ours.
        assertTrue(invariantChecker.check().none { entry.id in it.detail })
    }

    @Test
    fun `a stale revision with different content is a CONFLICT and the server keeps its value`() {
        val uid = newUid("bw-conflict")
        val entry = SyncTestFixtures.bodyWeightEntry(weightKg = 80f)
        push(uid, PushRequestDto(bodyWeightEntries = listOf(entry)))
        push(uid, PushRequestDto(bodyWeightEntries = listOf(entry.copy(weightKg = 79f, revision = 1))))

        val stale = push(uid, PushRequestDto(bodyWeightEntries = listOf(entry.copy(weightKg = 90f, revision = 1)))).results.single()

        assertEquals(SyncStatus.CONFLICT, stale.status)
        assertEquals(79f, storedWeight(entry.id))
    }

    @Test
    fun `another user's entry is FORBIDDEN and stays untouched`() {
        val owner = newUid("bw-owner")
        val entry = SyncTestFixtures.bodyWeightEntry(weightKg = 70f)
        push(owner, PushRequestDto(bodyWeightEntries = listOf(entry)))

        val attempt = push(newUid("bw-intruder"), PushRequestDto(bodyWeightEntries = listOf(entry.copy(weightKg = 1f, revision = 1)))).results.single()

        assertEquals(SyncStatus.FORBIDDEN, attempt.status)
        assertEquals(70f, storedWeight(entry.id))
    }

    @Test
    fun `an out-of-range weight is INVALID and not stored`() {
        val uid = newUid("bw-invalid")
        val zero = SyncTestFixtures.bodyWeightEntry(weightKg = 0f)
        val huge = SyncTestFixtures.bodyWeightEntry(weightKg = 701f)

        val results = push(uid, PushRequestDto(bodyWeightEntries = listOf(zero, huge))).results

        assertTrue(results.all { it.status == SyncStatus.INVALID })
        assertEquals(null, storedWeight(zero.id))
        assertEquals(null, storedWeight(huge.id))
    }

    @Test
    fun `pull returns entries, then the tombstone after a delete`() {
        val uid = newUid("bw-pull")
        val entry = SyncTestFixtures.bodyWeightEntry(weightKg = 75.25f)
        push(uid, PushRequestDto(bodyWeightEntries = listOf(entry)))

        val first = pull(uid)
        val pulled = first.bodyWeightEntries.single()
        assertEquals(entry.id, pulled.id)
        assertEquals(75.25f, pulled.weightKg)
        assertEquals(entry.measuredAt, pulled.measuredAt)
        assertEquals(null, pulled.deletedAt)

        push(uid, PushRequestDto(bodyWeightEntries = listOf(entry.copy(revision = 1, deletedAt = 1L))))
        val second = pull(uid, cursor = first.nextCursor)
        assertNotNull(second.bodyWeightEntries.single().deletedAt)
        assertTrue(pull(newUid("bw-other")).bodyWeightEntries.isEmpty(), "never another user's entries")
    }
}

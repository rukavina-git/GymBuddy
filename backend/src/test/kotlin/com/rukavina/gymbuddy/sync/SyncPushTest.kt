package com.rukavina.gymbuddy.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.MutationResultDto
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.domain.EntitySource
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * End-to-end coverage of POST /v1/sync/push against a real Postgres
 * (Testcontainers) - this is Group F's most failure-sensitive surface,
 * so these exercise the semantics from the design notes directly rather
 * than trusting unit tests of the pieces in isolation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class SyncPushTest : AbstractPostgresIntegrationTest() {

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

    private fun changeLogCount(userId: String, entityId: String): Int =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM change_log WHERE user_id = :userId AND entity_id = :entityId",
            mapOf("userId" to userId, "entityId" to entityId),
            Int::class.java,
        )!!

    private fun changeLogOperations(userId: String, entityId: String): List<String> =
        jdbcTemplate.queryForList(
            "SELECT operation FROM change_log WHERE user_id = :userId AND entity_id = :entityId ORDER BY seq",
            mapOf("userId" to userId, "entityId" to entityId),
            String::class.java,
        )

    // 1. two mutations to the same entity in one batch

    @Test
    fun `second mutation to the same entity in a batch sees the first's revision`() {
        val uid = newUid("batch-seq")
        val sessionId = SyncTestFixtures.uuid()
        val first = SyncTestFixtures.workoutSession(id = sessionId, title = "Push Day A", revision = 0)
        val second = SyncTestFixtures.workoutSession(id = sessionId, title = "Push Day B", revision = 1)

        val response = push(uid, PushRequestDto(workoutSessions = listOf(first, second)))

        assertEquals(2, response.results.size)
        assertEquals(SyncStatus.APPLIED, response.results[0].status)
        assertEquals(1, response.results[0].revision)
        assertEquals(SyncStatus.APPLIED, response.results[1].status)
        assertEquals(2, response.results[1].revision)
    }

    // 2. stale revision -> CONFLICT, does not apply

    @Test
    fun `stale revision with different content returns CONFLICT and does not apply`() {
        val uid = newUid("stale")
        val sessionId = SyncTestFixtures.uuid()
        push(uid, PushRequestDto(workoutSessions = listOf(SyncTestFixtures.workoutSession(id = sessionId, title = "Original", revision = 0))))

        val stale = SyncTestFixtures.workoutSession(id = sessionId, title = "A different edit", revision = 0)
        val response = push(uid, PushRequestDto(workoutSessions = listOf(stale)))

        val result = response.results.single()
        assertEquals(SyncStatus.CONFLICT, result.status)
        assertNotNull(result.reason)

        // Server state must be unchanged by the rejected mutation.
        val storedTitle = jdbcTemplate.queryForObject(
            "SELECT title FROM workout_sessions WHERE id = :id::uuid",
            mapOf("id" to sessionId),
            String::class.java,
        )
        assertEquals("Original", storedTitle)
    }

    /**
     * CONFLICT is proven for WorkoutSession above via the shared
     * RevisionChecker, but each of these four services independently
     * wires that check up to its own repository lookup and its own
     * DTO's revision field - a mistake in that wiring (e.g. comparing
     * against the wrong stored revision, or never reaching the check at
     * all) is exactly what a per-type-service split can hide unless
     * every type is exercised, not just one representative.
     */
    @Test
    fun `workout template stale revision with different content returns CONFLICT and does not apply`() {
        val uid = newUid("template-stale")
        val template = SyncTestFixtures.workoutTemplate(title = "Original")
        push(uid, PushRequestDto(workoutTemplates = listOf(template)))

        val stale = template.copy(title = "A different edit", revision = 0)
        val result = push(uid, PushRequestDto(workoutTemplates = listOf(stale))).results.single()

        assertEquals(SyncStatus.CONFLICT, result.status)
        assertNotNull(result.reason)
        val storedTitle = jdbcTemplate.queryForObject(
            "SELECT title FROM workout_templates WHERE id = :id::uuid",
            mapOf("id" to template.id),
            String::class.java,
        )
        assertEquals("Original", storedTitle)
    }

    @Test
    fun `user exercise state stale revision with different content returns CONFLICT and does not apply`() {
        val uid = newUid("exercise-state-stale")
        val state = SyncTestFixtures.userExerciseState().copy(defaultRestSeconds = 60)
        push(uid, PushRequestDto(userExerciseStates = listOf(state)))

        val stale = state.copy(defaultRestSeconds = 999, revision = 0)
        val result = push(uid, PushRequestDto(userExerciseStates = listOf(stale))).results.single()

        assertEquals(SyncStatus.CONFLICT, result.status)
        assertNotNull(result.reason)
        val storedRest = jdbcTemplate.queryForObject(
            "SELECT default_rest_seconds FROM user_exercise_state WHERE owner_id = :uid AND exercise_id = :id::uuid",
            mapOf("uid" to uid, "id" to state.exerciseId),
            Int::class.java,
        )
        assertEquals(60, storedRest)
    }

    @Test
    fun `user template state stale revision with different content returns CONFLICT and does not apply`() {
        val uid = newUid("template-state-stale")
        val state = SyncTestFixtures.userTemplateState().copy(isFavorite = true)
        push(uid, PushRequestDto(userTemplateStates = listOf(state)))

        val stale = state.copy(isFavorite = false, revision = 0)
        val result = push(uid, PushRequestDto(userTemplateStates = listOf(stale))).results.single()

        assertEquals(SyncStatus.CONFLICT, result.status)
        assertNotNull(result.reason)
        val storedFavorite = jdbcTemplate.queryForObject(
            "SELECT is_favorite FROM user_template_state WHERE owner_id = :uid AND template_id = :id::uuid",
            mapOf("uid" to uid, "id" to state.templateId),
            Boolean::class.java,
        )
        assertEquals(true, storedFavorite)
    }

    @Test
    fun `user profile stale revision with different content returns CONFLICT and does not apply`() {
        val uid = newUid("profile-stale")
        val profile = SyncTestFixtures.userProfile(name = "Original")
        push(uid, PushRequestDto(userProfile = profile))

        val stale = profile.copy(name = "A different edit", revision = 0)
        val result = push(uid, PushRequestDto(userProfile = stale)).results.single()

        assertEquals(SyncStatus.CONFLICT, result.status)
        assertNotNull(result.reason)
        val storedName = jdbcTemplate.queryForObject(
            "SELECT name FROM user_profile WHERE uid = :uid",
            mapOf("uid" to uid),
            String::class.java,
        )
        assertEquals("Original", storedName)
    }

    // 3. idempotent replay

    @Test
    fun `repeated push of an already-applied entity returns APPLIED with no duplicate change_log row`() {
        val uid = newUid("idempotent")
        val session = SyncTestFixtures.workoutSession(revision = 0)

        val firstResponse = push(uid, PushRequestDto(workoutSessions = listOf(session)))
        val secondResponse = push(uid, PushRequestDto(workoutSessions = listOf(session)))

        assertEquals(SyncStatus.APPLIED, firstResponse.results.single().status)
        assertEquals(1, firstResponse.results.single().revision)
        assertEquals(SyncStatus.APPLIED, secondResponse.results.single().status)
        assertEquals(1, secondResponse.results.single().revision)

        assertEquals(1, changeLogCount(uid, session.id))
    }

    // 4. an INVALID entity mid-batch does not block the others

    @Test
    fun `an INVALID entity mid-batch does not prevent the others applying`() {
        val uid = newUid("mixed-batch")
        val valid1 = SyncTestFixtures.workoutSession(title = "Valid One")
        val invalid = SyncTestFixtures.workoutSession(title = "  ") // blank title -> INVALID
        val valid2 = SyncTestFixtures.workoutSession(title = "Valid Two")

        val response = push(uid, PushRequestDto(workoutSessions = listOf(valid1, invalid, valid2)))

        assertEquals(3, response.results.size)
        assertEquals(SyncStatus.APPLIED, response.results[0].status)
        assertEquals(SyncStatus.INVALID, response.results[1].status)
        assertEquals(SyncStatus.APPLIED, response.results[2].status)
    }

    // 5. ownership

    @Test
    fun `pushing another user's entity returns FORBIDDEN`() {
        val owner = newUid("owner")
        val intruder = newUid("intruder")
        val session = SyncTestFixtures.workoutSession(revision = 0)
        push(owner, PushRequestDto(workoutSessions = listOf(session)))

        val attempt = session.copy(title = "Hijacked", revision = 1)
        val response = push(intruder, PushRequestDto(workoutSessions = listOf(attempt)))

        val result = response.results.single()
        assertEquals(SyncStatus.FORBIDDEN, result.status)

        val storedOwner = jdbcTemplate.queryForObject(
            "SELECT owner_id FROM workout_sessions WHERE id = :id::uuid",
            mapOf("id" to session.id),
            String::class.java,
        )
        assertEquals(owner, storedOwner)
    }

    @Test
    fun `pushing a DEFAULT exercise is rejected`() {
        val uid = newUid("default-exercise")
        val defaultExercise = SyncTestFixtures.exercise(source = EntitySource.DEFAULT)

        val response = push(uid, PushRequestDto(exercises = listOf(defaultExercise)))

        val result = response.results.single()
        assertEquals(SyncStatus.FORBIDDEN, result.status)

        val exists = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM exercises WHERE id = :id::uuid",
            mapOf("id" to defaultExercise.id),
            Int::class.java,
        )
        assertEquals(0, exists)
    }

    // 7. aggregate replacement

    @Test
    fun `aggregate replacement removes sets that are no longer pushed`() {
        val uid = newUid("aggregate-replace")
        val exerciseId = SyncTestFixtures.uuid()
        val performedExerciseId = SyncTestFixtures.uuid()
        val twoSets = SyncTestFixtures.performedExercise(
            id = performedExerciseId,
            exerciseId = exerciseId,
            sets = listOf(
                SyncTestFixtures.workoutSet(orderIndex = 0),
                SyncTestFixtures.workoutSet(orderIndex = 1),
            ),
        )
        val session = SyncTestFixtures.workoutSession(performedExercises = listOf(twoSets), revision = 0)
        push(uid, PushRequestDto(workoutSessions = listOf(session)))

        val setCountAfterCreate = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM workout_sets ws JOIN performed_exercises pe ON pe.id = ws.performed_exercise_id WHERE pe.workout_session_id = :id::uuid",
            mapOf("id" to session.id),
            Int::class.java,
        )
        assertEquals(2, setCountAfterCreate)

        val oneSet = SyncTestFixtures.performedExercise(
            id = performedExerciseId,
            exerciseId = exerciseId,
            sets = listOf(SyncTestFixtures.workoutSet(orderIndex = 0)),
        )
        val update = session.copy(performedExercises = listOf(oneSet), revision = 1)
        val response = push(uid, PushRequestDto(workoutSessions = listOf(update)))
        assertEquals(SyncStatus.APPLIED, response.results.single().status)

        val setCountAfterUpdate = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM workout_sets ws JOIN performed_exercises pe ON pe.id = ws.performed_exercise_id WHERE pe.workout_session_id = :id::uuid",
            mapOf("id" to session.id),
            Int::class.java,
        )
        assertEquals(1, setCountAfterUpdate)
    }

    @Test
    fun `aggregate replacement removes a whole performed exercise absent from the payload, not just its sets`() {
        val uid = newUid("aggregate-replace-whole-child")
        val keep = SyncTestFixtures.performedExercise(orderIndex = 0)
        val drop = SyncTestFixtures.performedExercise(orderIndex = 1)
        val session = SyncTestFixtures.workoutSession(performedExercises = listOf(keep, drop), revision = 0)
        push(uid, PushRequestDto(workoutSessions = listOf(session)))

        val countAfterCreate = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM performed_exercises WHERE workout_session_id = :id::uuid",
            mapOf("id" to session.id),
            Int::class.java,
        )
        assertEquals(2, countAfterCreate)

        val update = session.copy(performedExercises = listOf(keep), revision = 1)
        val response = push(uid, PushRequestDto(workoutSessions = listOf(update)))
        assertEquals(SyncStatus.APPLIED, response.results.single().status)

        val remainingIds = jdbcTemplate.queryForList(
            "SELECT id FROM performed_exercises WHERE workout_session_id = :id::uuid",
            mapOf("id" to session.id),
            String::class.java,
        )
        assertEquals(listOf(keep.id), remainingIds, "the dropped performed exercise - and its sets, via cascade - must be gone entirely")

        val setsForDropped = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM workout_sets WHERE performed_exercise_id = :id::uuid",
            mapOf("id" to drop.id),
            Int::class.java,
        )
        assertEquals(0, setsForDropped)
    }

    @Test
    fun `aggregate replacement removes a template exercise absent from the payload`() {
        val uid = newUid("template-aggregate-replace")
        val keep = SyncTestFixtures.templateExercise(orderIndex = 0)
        val drop = SyncTestFixtures.templateExercise(orderIndex = 1)
        val template = SyncTestFixtures.workoutTemplate(templateExercises = listOf(keep, drop), revision = 0)
        push(uid, PushRequestDto(workoutTemplates = listOf(template)))

        val countAfterCreate = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM template_exercises WHERE template_id = :id::uuid",
            mapOf("id" to template.id),
            Int::class.java,
        )
        assertEquals(2, countAfterCreate)

        val update = template.copy(templateExercises = listOf(keep), revision = 1)
        val response = push(uid, PushRequestDto(workoutTemplates = listOf(update)))
        assertEquals(SyncStatus.APPLIED, response.results.single().status)

        val remainingIds = jdbcTemplate.queryForList(
            "SELECT id FROM template_exercises WHERE template_id = :id::uuid",
            mapOf("id" to template.id),
            String::class.java,
        )
        assertEquals(listOf(keep.id), remainingIds, "the dropped template exercise must be gone entirely")
    }

    // 8. exactly one change_log row per applied entity, none for rejected

    @Test
    fun `exactly one change_log row per applied entity, none for rejected`() {
        val owner = newUid("changelog-owner")
        val intruder = newUid("changelog-intruder")

        val applied = SyncTestFixtures.workoutSession(title = "Kept")
        val invalid = SyncTestFixtures.workoutSession(title = "")
        push(owner, PushRequestDto(workoutSessions = listOf(applied, invalid)))

        assertEquals(1, changeLogCount(owner, applied.id))
        assertEquals(0, changeLogCount(owner, invalid.id))

        val forbiddenTarget = SyncTestFixtures.workoutSession()
        push(owner, PushRequestDto(workoutSessions = listOf(forbiddenTarget)))
        push(intruder, PushRequestDto(workoutSessions = listOf(forbiddenTarget.copy(title = "stolen", revision = 1))))
        assertEquals(0, changeLogCount(intruder, forbiddenTarget.id))
    }

    // 9. concurrency

    @Test
    fun `concurrent pushes for the same user all apply and change_log stays monotonic`() {
        val uid = newUid("concurrent")
        val threadCount = 8
        val sessions = (1..threadCount).map { SyncTestFixtures.workoutSession(title = "Concurrent $it") }
        val pool = Executors.newFixedThreadPool(threadCount)
        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)
        val done = CountDownLatch(threadCount)
        val results = java.util.Collections.synchronizedList(mutableListOf<MutationResultDto>())

        try {
            sessions.forEach { session ->
                pool.submit {
                    ready.countDown()
                    start.await()
                    try {
                        results += push(uid, PushRequestDto(workoutSessions = listOf(session))).results.single()
                    } finally {
                        done.countDown()
                    }
                }
            }
            ready.await(10, TimeUnit.SECONDS)
            start.countDown()
            assertTrue(done.await(20, TimeUnit.SECONDS), "all pushes should finish within 20s")
        } finally {
            pool.shutdown()
        }

        assertTrue(results.all { it.status == SyncStatus.APPLIED && it.revision == 1 })

        val seqValues = jdbcTemplate.queryForList(
            "SELECT seq FROM change_log WHERE user_id = :uid ORDER BY seq",
            mapOf("uid" to uid),
            Long::class.java,
        )
        assertEquals(threadCount, seqValues.size)
        assertEquals(seqValues.distinct().size, seqValues.size, "no duplicate/lost sequence numbers")
    }

    // Non-aggregate entity types round-trip through the same shared machinery

    @Test
    fun `custom exercise, template, overlay rows and profile all apply`() {
        val uid = newUid("all-types")
        val exercise = SyncTestFixtures.exercise()
        val template = SyncTestFixtures.workoutTemplate()
        val exerciseState = SyncTestFixtures.userExerciseState(exerciseId = exercise.id)
        val templateState = SyncTestFixtures.userTemplateState(templateId = template.id)
        val profile = SyncTestFixtures.userProfile()

        val response = push(
            uid,
            PushRequestDto(
                exercises = listOf(exercise),
                workoutTemplates = listOf(template),
                userExerciseStates = listOf(exerciseState),
                userTemplateStates = listOf(templateState),
                userProfile = profile,
            ),
        )

        assertEquals(5, response.results.size)
        assertTrue(response.results.all { it.status == SyncStatus.APPLIED })
        assertEquals(setOf(EntityType.EXERCISE, EntityType.WORKOUT_TEMPLATE, EntityType.USER_EXERCISE_STATE, EntityType.USER_TEMPLATE_STATE, EntityType.USER_PROFILE), response.results.map { it.entityType }.toSet())
    }

    // Delete path: an upsert with deletedAt set

    @Test
    fun `deleting a workout session stamps the server's own deletedAt and logs operation DELETE`() {
        val uid = newUid("delete-session")
        val session = SyncTestFixtures.workoutSession(revision = 0)
        val created = push(uid, PushRequestDto(workoutSessions = listOf(session))).results.single()
        assertEquals(SyncStatus.APPLIED, created.status)

        // Client's own local delete timestamp - deliberately far from "now"
        // so the assertion below can tell whether the server actually
        // ignored it and stamped its own value, rather than persisting
        // whatever the client sent verbatim.
        val clientLocalDeleteTimestamp = 1L
        val delete = session.copy(revision = created.revision!!, deletedAt = clientLocalDeleteTimestamp)
        val beforeDelete = System.currentTimeMillis()
        val deleted = push(uid, PushRequestDto(workoutSessions = listOf(delete))).results.single()

        assertEquals(SyncStatus.APPLIED, deleted.status)
        assertEquals((created.revision ?: 0) + 1, deleted.revision)

        val storedDeletedAt = jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM workout_sessions WHERE id = :id::uuid",
            mapOf("id" to session.id),
            Long::class.java,
        )
        assertNotNull(storedDeletedAt)
        assertTrue(storedDeletedAt!! >= beforeDelete, "server must stamp its own current time, not persist the client's value ($clientLocalDeleteTimestamp)")

        assertEquals(listOf("UPSERT", "DELETE"), changeLogOperations(uid, session.id))
    }

    @Test
    fun `repeated delete push of the same entity is idempotent`() {
        val uid = newUid("delete-idempotent")
        val exercise = SyncTestFixtures.exercise(revision = 0)
        val created = push(uid, PushRequestDto(exercises = listOf(exercise))).results.single()

        val delete = exercise.copy(revision = created.revision!!, deletedAt = 12345L)
        val firstDelete = push(uid, PushRequestDto(exercises = listOf(delete))).results.single()
        val secondDelete = push(uid, PushRequestDto(exercises = listOf(delete))).results.single()

        assertEquals(SyncStatus.APPLIED, firstDelete.status)
        assertEquals(SyncStatus.APPLIED, secondDelete.status)
        assertEquals(firstDelete.revision, secondDelete.revision)
        assertEquals(listOf("UPSERT", "DELETE"), changeLogOperations(uid, exercise.id))
    }

    @Test
    fun `deleting a DEFAULT exercise is still rejected as FORBIDDEN`() {
        val uid = newUid("delete-default-exercise")
        val defaultExercise = SyncTestFixtures.exercise(source = EntitySource.DEFAULT).copy(deletedAt = 1L)

        val result = push(uid, PushRequestDto(exercises = listOf(defaultExercise))).results.single()
        assertEquals(SyncStatus.FORBIDDEN, result.status)
    }
}

package com.rukavina.gymbuddy.api

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import com.rukavina.gymbuddy.api.dto.PullResponseDto
import com.rukavina.gymbuddy.api.dto.PushRequestDto
import com.rukavina.gymbuddy.api.dto.PushResponseDto
import com.rukavina.gymbuddy.auth.RestAuthenticationEntryPoint
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.domain.SyncStatus
import com.rukavina.gymbuddy.sync.SyncMetrics
import com.rukavina.gymbuddy.sync.SyncTestFixtures
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
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

/**
 * Step 04 of phase5-data-safety: every log line of a request carries
 * traceId/userId/client headers in MDC, an error body's traceId is the
 * one that was logged, and the sync counters move.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class RequestObservabilityTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    private val attached = mutableListOf<Pair<Logger, ListAppender<ILoggingEvent>>>()

    @AfterEach
    fun detachAppenders() {
        attached.forEach { (logger, appender) -> logger.detachAppender(appender) }
    }

    /** Captures events with their MDC frozen at log time (logback reads it lazily). */
    private fun capture(type: Class<*>): ListAppender<ILoggingEvent> {
        val logger = LoggerFactory.getLogger(type) as Logger
        val appender = object : ListAppender<ILoggingEvent>() {
            override fun append(event: ILoggingEvent) {
                event.prepareForDeferredProcessing()
                super.append(event)
            }
        }
        appender.start()
        logger.addAppender(appender)
        attached += logger to appender
        return appender
    }

    private fun newUid(label: String) = "$label-${System.nanoTime()}"

    private fun bearerToken(uid: String): String = TestJwtBuilder().subject(uid).build()

    private fun push(uid: String, request: PushRequestDto): PushResponseDto {
        val body = mockMvc.perform(
            post("/v1/sync/push")
                .header("Authorization", "Bearer ${bearerToken(uid)}")
                .header("X-Client-Version", "0.0.2-test")
                .header("X-Client-Platform", "android")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString
        return objectMapper.readValue(body, PushResponseDto::class.java)
    }

    private fun traceIdOf(body: String): String = objectMapper.readTree(body).get("traceId").asText()

    private fun counter(name: String, vararg tags: String): Double =
        meterRegistry.find(name).tags(*tags).counter()?.count() ?: 0.0

    @Test
    fun `a push logs traceId, userId and both client headers, and MDC is cleared afterwards`() {
        val logs = capture(SyncController::class.java)
        val uid = newUid("mdc")

        push(uid, PushRequestDto(exercises = listOf(SyncTestFixtures.exercise())))

        val mdc = logs.list.single { it.formattedMessage.startsWith("push:") }.mdcPropertyMap
        assertNotNull(mdc[RequestContext.TRACE_ID])
        assertEquals(uid, mdc[RequestContext.USER_ID])
        assertEquals("0.0.2-test", mdc[RequestContext.CLIENT_VERSION])
        assertEquals("android", mdc[RequestContext.CLIENT_PLATFORM])
        assertNull(MDC.get(RequestContext.TRACE_ID))
        assertNull(MDC.get(RequestContext.USER_ID))
    }

    @Test
    fun `a 401 body carries the traceId that was logged`() {
        val logs = capture(RestAuthenticationEntryPoint::class.java)

        val body = mockMvc.perform(get("/v1/sync/pull"))
            .andExpect(status().isUnauthorized)
            .andReturn().response.contentAsString

        val event = logs.list.single()
        assertEquals(traceIdOf(body), event.mdcPropertyMap[RequestContext.TRACE_ID])
        assertTrue(event.formattedMessage.contains(traceIdOf(body)))
    }

    @Test
    fun `a 410 bumps the cursor-expired counter and its body carries the logged traceId`() {
        val logs = capture(GlobalExceptionHandler::class.java)
        val uid = newUid("expired")
        push(uid, PushRequestDto(exercises = listOf(SyncTestFixtures.exercise())))
        val afterFirst = objectMapper.readValue(
            mockMvc.perform(get("/v1/sync/pull").header("Authorization", "Bearer ${bearerToken(uid)}"))
                .andExpect(status().isOk).andReturn().response.contentAsString,
            PullResponseDto::class.java,
        )
        assertTrue(counter(SyncMetrics.PULL_ENTITIES, "entityType", "EXERCISE") >= 1.0)

        // Same setup as SyncPullTest's 410 case: prune a change the cursor
        // hasn't seen yet and move the retention watermark past it.
        val second = push(uid, PushRequestDto(exercises = listOf(SyncTestFixtures.exercise()))).results.single()
        val prunedSeq = jdbcTemplate.queryForObject(
            "SELECT seq FROM change_log WHERE user_id = :uid AND entity_id = :entityId",
            mapOf("uid" to uid, "entityId" to second.entityId),
            Long::class.java,
        )!!
        jdbcTemplate.update(
            "DELETE FROM change_log WHERE user_id = :uid AND entity_id = :entityId",
            mapOf("uid" to uid, "entityId" to second.entityId),
        )
        jdbcTemplate.update(
            "INSERT INTO sync_retention_watermark (user_id, retention_floor_seq, updated_at) VALUES (:uid, :seq, :now)",
            mapOf("uid" to uid, "seq" to prunedSeq, "now" to System.currentTimeMillis()),
        )
        val before = counter(SyncMetrics.CURSOR_EXPIRED)

        val body = mockMvc.perform(
            get("/v1/sync/pull")
                .header("Authorization", "Bearer ${bearerToken(uid)}")
                .param("cursor", afterFirst.nextCursor),
        )
            .andExpect(status().isGone)
            .andReturn().response.contentAsString

        assertEquals(before + 1, counter(SyncMetrics.CURSOR_EXPIRED))
        val event = logs.list.single { it.formattedMessage.startsWith("410") }
        assertEquals(traceIdOf(body), event.mdcPropertyMap[RequestContext.TRACE_ID])
        assertEquals(uid, event.mdcPropertyMap[RequestContext.USER_ID])
    }

    @Test
    fun `a push with a conflict bumps the CONFLICT counter for that entity type`() {
        val uid = newUid("conflict")
        val sessionId = SyncTestFixtures.uuid()
        push(uid, PushRequestDto(workoutSessions = listOf(SyncTestFixtures.workoutSession(id = sessionId, title = "Original"))))
        val conflicts = counter(SyncMetrics.PUSH_RESULTS, "entityType", "WORKOUT_SESSION", "status", "CONFLICT")
        val applied = counter(SyncMetrics.PUSH_RESULTS, "entityType", "WORKOUT_SESSION", "status", "APPLIED")

        val result = push(
            uid,
            PushRequestDto(workoutSessions = listOf(SyncTestFixtures.workoutSession(id = sessionId, title = "Stale edit", revision = 0))),
        ).results.single()

        assertEquals(SyncStatus.CONFLICT, result.status)
        assertEquals(conflicts + 1, counter(SyncMetrics.PUSH_RESULTS, "entityType", "WORKOUT_SESSION", "status", "CONFLICT"))
        assertEquals(applied, counter(SyncMetrics.PUSH_RESULTS, "entityType", "WORKOUT_SESSION", "status", "APPLIED"))
    }
}

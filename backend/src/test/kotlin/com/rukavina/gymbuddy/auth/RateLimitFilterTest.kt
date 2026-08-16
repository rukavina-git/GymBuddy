package com.rukavina.gymbuddy.auth

import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * A tiny capacity (2/min, refilling at 2/min) keeps this deterministic
 * and fast instead of firing 61 real requests against the default
 * ~60/min bucket.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["gymbuddy.rate-limit.capacity=2", "gymbuddy.rate-limit.refill-per-minute=2"],
)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class RateLimitFilterTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `exceeding the per-uid bucket returns 429 with Retry-After and the Error shape`() {
        val token = TestJwtBuilder().subject("rate-limit-test-${System.nanoTime()}").build()

        // Capacity 2: the first two requests consume the bucket, the third is rejected.
        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $token")).andExpect(status().isOk)
        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $token")).andExpect(status().isOk)
        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isTooManyRequests)
            .andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.error").value("RATE_LIMITED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty)
    }

    @Test
    fun `buckets are keyed per uid - a different uid is unaffected`() {
        val exhaustedUid = TestJwtBuilder().subject("rate-limit-test-a-${System.nanoTime()}").build()
        val freshUid = TestJwtBuilder().subject("rate-limit-test-b-${System.nanoTime()}").build()

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $exhaustedUid")).andExpect(status().isOk)
        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $exhaustedUid")).andExpect(status().isOk)
        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $exhaustedUid")).andExpect(status().isTooManyRequests)

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $freshUid")).andExpect(status().isOk)
    }
}

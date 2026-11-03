package com.rukavina.gymbuddy.sync

import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/** A push body Jackson can't read is 400 VALIDATION_FAILED (spec: BadRequest), never 500. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class SyncUnreadableBodyTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private fun pushRaw(body: String) = mockMvc.perform(
        post("/v1/sync/push")
            .header("Authorization", "Bearer ${TestJwtBuilder().subject("unreadable-${System.nanoTime()}").build()}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
    )

    @Test
    fun `malformed JSON returns 400 VALIDATION_FAILED`() {
        pushRaw("{\"exercises\": [")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.traceId").exists())
    }

    @Test
    fun `unknown enum value returns 400 VALIDATION_FAILED`() {
        pushRaw("{\"userProfile\": {\"uid\": \"x\", \"gender\": \"NOT_A_GENDER\"}}")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
    }

    @Test
    fun `wrong type returns 400 VALIDATION_FAILED`() {
        pushRaw("{\"exercises\": \"not-an-array\"}")
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
    }
}

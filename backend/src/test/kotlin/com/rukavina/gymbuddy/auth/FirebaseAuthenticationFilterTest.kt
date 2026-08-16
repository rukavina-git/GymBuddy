package com.rukavina.gymbuddy.auth

import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.KeyPairGenerator
import java.time.Instant

/**
 * Exercises the real Spring Security filter chain end to end (MockMvc
 * against the actual securityFilterChain bean, not a unit test of
 * FirebaseAuthenticationFilter in isolation) - the only way to catch a
 * misconfigured chain, e.g. a missing authenticationEntryPoint falling
 * back to Spring's default empty 403. Tokens are hand-crafted with a
 * test RSA key pair via TestJwtBuilder/LocalJwtTokenVerifier; Firebase
 * itself is never called.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(TestSecurityConfig::class)
class FirebaseAuthenticationFilterTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `valid token returns 200 and bootstraps the caller's profile`() {
        val uid = "filter-test-valid-${System.nanoTime()}"
        val token = TestJwtBuilder()
            .subject(uid)
            .claim("email", "someone@example.com")
            .claim("name", "Someone")
            .build()

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.profile.uid").value(uid))
            .andExpect(jsonPath("$.profile.email").value("someone@example.com"))
            .andExpect(jsonPath("$.exerciseLibraryVersion").value(9))
            .andExpect(jsonPath("$.templateLibraryVersion").value(5))
            .andExpect(jsonPath("$.serverTime").exists())
    }

    @Test
    fun `missing Authorization header returns 401 with Error shape and does not continue the chain`() {
        mockMvc.perform(get("/v1/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(header().string("Content-Type", containsString("UTF-8")))
            .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.traceId").isNotEmpty)
    }

    @Test
    fun `malformed Authorization header returns 401`() {
        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer x"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
            .andExpect(jsonPath("$.traceId").isNotEmpty)
    }

    @Test
    fun `Bearer prefix with no token returns 401`() {
        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer "))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
    }

    @Test
    fun `expired token returns 401`() {
        val token = TestJwtBuilder()
            .issuedAt(Instant.now().minusSeconds(7200))
            .expiresAt(Instant.now().minusSeconds(3600))
            .build()

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
    }

    @Test
    fun `wrong issuer returns 401`() {
        val token = TestJwtBuilder().issuer("https://evil.example.com/not-firebase").build()

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
    }

    @Test
    fun `wrong audience returns 401`() {
        val token = TestJwtBuilder().audience("some-other-firebase-project").build()

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
    }

    @Test
    fun `token signed with the wrong key returns 401`() {
        val otherKeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val token = TestJwtBuilder().signedWith(otherKeyPair.private).build()

        mockMvc.perform(get("/v1/me").header("Authorization", "Bearer $token"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.error").value("UNAUTHENTICATED"))
    }

    @Test
    fun `unauthenticated request to health is not affected`() {
        mockMvc.perform(get("/v1/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
    }
}

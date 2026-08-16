package com.rukavina.gymbuddy.auth.testsupport

import com.rukavina.gymbuddy.auth.TokenVerifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * Replaces the production TokenVerifier (FirebaseTokenVerifier, excluded
 * under the "test" profile - see FirebaseAdminConfig) with one that
 * verifies hand-crafted JWTs against TestKeys, with no Firebase call.
 * Import this into any @SpringBootTest that needs the real security
 * filter chain.
 */
@TestConfiguration
class TestSecurityConfig {

    @Bean
    fun tokenVerifier(): TokenVerifier = LocalJwtTokenVerifier()
}

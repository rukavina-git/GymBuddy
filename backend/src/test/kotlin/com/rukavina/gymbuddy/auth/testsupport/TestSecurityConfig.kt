package com.rukavina.gymbuddy.auth.testsupport

import com.rukavina.gymbuddy.auth.FirebaseUserDeleter
import com.rukavina.gymbuddy.auth.TokenVerifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

/**
 * Replaces the production TokenVerifier (FirebaseTokenVerifier, excluded
 * under the "test" profile - see FirebaseAdminConfig) with one that
 * verifies hand-crafted JWTs against TestKeys, with no Firebase call.
 * Import this into any @SpringBootTest that needs the real security
 * filter chain.
 *
 * Also supplies a no-op FirebaseUserDeleter: FirebaseAdminUserDeleter is
 * likewise excluded under "test", but unlike TokenVerifier every test
 * context needs SOME bean of this type just to start up (AccountController
 * -> AccountDeletionService is an eagerly-instantiated singleton, wired
 * into every context regardless of whether that test ever calls DELETE
 * /v1/account). Tests that actually exercise account deletion override
 * this with @MockitoBean to assert on it or make it throw - see
 * AccountDeletionTest/AccountDeletionFailureTest.
 */
@TestConfiguration
class TestSecurityConfig {

    @Bean
    fun tokenVerifier(): TokenVerifier = LocalJwtTokenVerifier()

    @Bean
    fun firebaseUserDeleter(): FirebaseUserDeleter = object : FirebaseUserDeleter {
        override fun deleteUser(uid: String) {
            // No-op by default. Tests that care replace this bean via @MockitoBean.
        }
    }
}

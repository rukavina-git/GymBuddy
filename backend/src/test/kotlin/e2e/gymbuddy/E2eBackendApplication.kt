package e2e.gymbuddy

import com.rukavina.gymbuddy.auth.FirebaseUserDeleter
import com.rukavina.gymbuddy.auth.TokenVerifier
import com.rukavina.gymbuddy.auth.testsupport.LocalJwtTokenVerifier
import com.rukavina.gymbuddy.auth.testsupport.TestJwtBuilder
import com.rukavina.gymbuddy.main as gymBuddyMain
import com.rukavina.gymbuddy.sync.RetentionSummary
import com.rukavina.gymbuddy.sync.TombstoneRetentionService
import org.springframework.boot.SpringApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The real backend, for the Android sync engine's end-to-end tests.
 *
 * Everything is production code except two things. Firebase token
 * verification is swapped for the test key pair (LocalJwtTokenVerifier,
 * via the "test" profile, same as the backend's own integration tests),
 * because a test can't mint real Firebase ID tokens. And there are two
 * test-only endpoints under /e2e: one to mint a token for a uid, one to
 * run tombstone retention on demand. It serves real HTTP against a real
 * Postgres (docker/compose.yaml's `db` service by default; DATABASE_URL
 * as in application.yml).
 *
 * Run: `docker compose -f docker/compose.yaml up -d db`, then
 * `./gradlew bootTestRun` in backend/. Lives in test sources, in a
 * package outside com.rukavina.gymbuddy so the backend's own
 * @SpringBootTest component scans never pick these endpoints up. It is
 * never part of a production build.
 */
fun main(args: Array<String>) {
    SpringApplication.from { gymBuddyMain(it) }
        .with(E2eConfig::class.java)
        .withAdditionalProfiles("test")
        .run(
            "--gymbuddy.rate-limit.capacity=100000",
            "--gymbuddy.rate-limit.refill-per-minute=100000",
            // Anything tombstoned is immediately prunable by POST /e2e/retention.
            "--gymbuddy.sync.tombstone-retention-days=0",
            *args,
        )
}

@TestConfiguration(proxyBeanMethods = false)
class E2eConfig {

    @Bean
    fun tokenVerifier(): TokenVerifier = LocalJwtTokenVerifier()

    @Bean
    fun firebaseUserDeleter(): FirebaseUserDeleter = object : FirebaseUserDeleter {
        override fun deleteUser(uid: String) = Unit
    }

    @Bean
    fun e2eController(retention: TombstoneRetentionService) = E2eController(retention)

    // Everything under /e2e is unauthenticated; the main chain still guards the rest.
    @Bean
    @Order(0)
    fun e2eSecurityChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("/e2e/**")
            authorizeHttpRequests { authorize(anyRequest, permitAll) }
            csrf { disable() }
        }
        return http.build()
    }
}

@RestController
class E2eController(private val retention: TombstoneRetentionService) {

    @PostMapping("/e2e/token")
    fun token(@RequestParam uid: String): Map<String, String> = mapOf("token" to TestJwtBuilder().subject(uid).build())

    @PostMapping("/e2e/retention")
    fun runRetention(): RetentionSummary = retention.pruneExpiredTombstones()
}

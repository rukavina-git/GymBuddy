package com.rukavina.gymbuddy.persistence

import com.rukavina.gymbuddy.auth.testsupport.TestSecurityConfig
import com.rukavina.gymbuddy.testsupport.AbstractPostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Race safety for just-in-time provisioning: many concurrent first
 * requests from the same brand-new uid must produce exactly one
 * user_profile row, not one per request and not a constraint-violation
 * crash. Relies on UserProfileRepository.insertIfAbsent's real
 * INSERT ... ON CONFLICT (uid) DO NOTHING against a real Postgres -
 * an in-memory or mocked datastore wouldn't exercise the actual
 * conflict-resolution path this depends on.
 */
@SpringBootTest
@Import(TestSecurityConfig::class)
class UserProfileRepositoryConcurrencyTest : AbstractPostgresIntegrationTest() {

    @Autowired
    private lateinit var userProfileRepository: UserProfileRepository

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun `concurrent first-sight provisioning for the same uid creates exactly one row`() {
        val uid = "concurrency-test-${System.nanoTime()}"
        val threadCount = 20
        val pool = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        try {
            repeat(threadCount) { i ->
                pool.submit {
                    readyLatch.countDown()
                    startLatch.await()
                    try {
                        userProfileRepository.insertIfAbsent(
                            uid = uid,
                            name = "Racer $i",
                            email = "racer$i@example.com",
                            profileImageUrl = null,
                        )
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            readyLatch.await(10, TimeUnit.SECONDS)
            startLatch.countDown()
            assertEquals(true, doneLatch.await(10, TimeUnit.SECONDS), "all racers should finish within 10s")
        } finally {
            pool.shutdown()
        }

        val rowCount = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM user_profile WHERE uid = :uid",
            mapOf("uid" to uid),
            Int::class.java,
        )
        assertEquals(1, rowCount)
    }
}

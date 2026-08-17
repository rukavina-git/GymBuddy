package com.rukavina.gymbuddy.infra

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

/**
 * The one Testcontainers test that doesn't extend
 * AbstractPostgresIntegrationTest (it's testing the container mechanism
 * itself, not a Spring context against it), so it needs @Tag("testcontainers")
 * applied directly rather than inherited - see that class's doc for why
 * every other Testcontainers test doesn't repeat this.
 */
@Tag("testcontainers")
@Testcontainers
class PostgresTestcontainerSmokeTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @Test
    fun `container starts and accepts a connection`() {
        assertTrue(postgres.isRunning)

        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT 1").use { resultSet ->
                    assertTrue(resultSet.next())
                    assertTrue(resultSet.getInt(1) == 1)
                }
            }
        }
    }
}

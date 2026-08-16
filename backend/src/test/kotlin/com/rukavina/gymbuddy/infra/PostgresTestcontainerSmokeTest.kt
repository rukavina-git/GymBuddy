package com.rukavina.gymbuddy.infra

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager

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

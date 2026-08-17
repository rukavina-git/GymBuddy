package com.rukavina.gymbuddy.testsupport

import org.junit.jupiter.api.Tag
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Shared Postgres Testcontainers wiring for @SpringBootTest classes.
 *
 * This is the Testcontainers "singleton container" pattern, not
 * @Testcontainers + @Container: the container lives in a Kotlin object,
 * started once via its init block and never stopped explicitly - Ryuk
 * reaps it at JVM exit. Several @SpringBootTest classes extend this
 * base, and a JUnit-managed @Container field is a *static field on the
 * declaring class*, so subclasses would all share the exact same
 * instance but each subclass's own before/after-all callbacks would
 * still stop it independently - the first test class to finish tears
 * the container down while a sibling class is still mid-run against it,
 * surfacing as a flaky "connection refused". The singleton-object
 * pattern sidesteps that: nothing but JVM exit stops this container.
 */
private object SharedPostgres : PostgreSQLContainer<Nothing>("postgres:16-alpine") {
    init {
        start()
    }
}

/**
 * @Tag("testcontainers") here, not repeated on every subclass: JUnit5
 * aggregates tags from the whole class hierarchy (see the Gradle user
 * guide's tagging section), so every test extending this base is tagged
 * consistently by construction - nothing to remember to add per class,
 * and no risk of a new test quietly going untagged.
 */
@Tag("testcontainers")
@ActiveProfiles("test")
abstract class AbstractPostgresIntegrationTest {

    companion object {
        @DynamicPropertySource
        @JvmStatic
        fun datasourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { SharedPostgres.jdbcUrl }
            registry.add("spring.datasource.username") { SharedPostgres.username }
            registry.add("spring.datasource.password") { SharedPostgres.password }
        }
    }
}

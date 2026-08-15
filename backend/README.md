# Backend

Spring Boot (Kotlin) + PostgreSQL. Implements the contract in
`api/openapi.yaml`.

- Kotlin, Gradle Kotlin DSL, JDK 21 (`jvmToolchain`, independent of the
  JDK that launches Gradle - the wrapper will provision one if needed).
- Package root `com.rukavina.gymbuddy`.
- Reaches Postgres only via the `DATABASE_URL` env var - no
  provider-specific config elsewhere. See `docker/compose.yaml` for the
  full local setup.

## Package structure

    api          controllers, DTOs, error mapping
    domain       entities, value objects, validation
    persistence  repositories, Flyway migrations
    sync         change log, cursor codec, push/pull orchestration
    auth         token verification, security filter chain
    config       Spring configuration, OpenAPI wiring

## Commands

    ./gradlew build      compile and package
    ./gradlew bootRun     run locally (needs DATABASE_URL pointing at a reachable Postgres)
    ./gradlew test        run tests

Local run without Docker:

    DATABASE_URL="jdbc:postgresql://localhost:5432/gymbuddy?user=gymbuddy&password=gymbuddy" ./gradlew bootRun

## Config placeholders

Firebase project id/credentials path and sync retention/page-size limits
are externalised in `application.yml` with local defaults, ready to be
wired up in later groups. The Firebase service account key file is
gitignored - it is a real credential and must never be committed.

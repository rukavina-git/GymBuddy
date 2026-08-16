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

## Firebase credentials

The backend verifies Firebase ID tokens using the Admin SDK, which needs
a service account key. That key is supplied as `FIREBASE_CREDENTIALS_B64`
- the service account JSON, base64-encoded, in an environment variable.
There is no key file on disk anywhere, locally or in production: the
value lives in the macOS Keychain on dev machines and comes from the
host environment (e.g. a secrets manager) in production.

The app fails fast and loudly at startup - not on the first request -
if the variable is missing or doesn't decode to valid service account
JSON.

### Local development (macOS Keychain)

Store the key once, after downloading `service-account.json` from the
Firebase console (Project settings -> Service accounts -> Generate new
private key):

    security add-generic-password -a "$USER" -s gymbuddy-firebase-credentials \
      -w "$(base64 < /path/to/service-account.json)"

Delete the downloaded JSON file afterwards - only the Keychain entry is
needed from here on. Load it into your shell before running the backend:

    export FIREBASE_CREDENTIALS_B64=$(security find-generic-password -a "$USER" -s gymbuddy-firebase-credentials -w)

This is required both for `./gradlew bootRun` and for
`docker compose -f docker/compose.yaml up --build` - compose passes it
through from the calling shell's environment and refuses to start with
a clear message if it isn't set.

Never commit the service account JSON or its base64 form, and never
paste it into `application.yml` or any other tracked file.

## Config placeholders

Sync retention/page-size limits and the in-memory rate limiter's
capacity/refill rate are externalised in `application.yml` with local
defaults. `gymbuddy.sync.read-only` (`SYNC_READ_ONLY`) is wired up
(Group F: it makes `POST /v1/sync/push` reject every request with 503
while pull keeps working); tombstone-retention-days and the pull
page-size limits remain placeholders for Group G.

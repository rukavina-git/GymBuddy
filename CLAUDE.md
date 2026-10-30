# GymBuddy

Offline-first workout tracker. Native Android and iOS clients against a
Kotlin backend, sharing nothing but the OpenAPI contract.

## Repo
- api/      OpenAPI 3.1 contract (openapi.yaml), Spectral lint, docs site
- android/  Kotlin, Compose, Hilt, Room client (has its own CLAUDE.md)
- backend/  Kotlin, Spring Boot, PostgreSQL, Flyway
- ios/      Swift/SwiftUI client, not started (README only)
- docker/   compose.yaml for local Postgres + backend
- docs/     roadmap (docs/roadmap.md, details in roadmap-details.md),
            plans (docs/plans/), ADRs (docs/adr/), architecture plan,
            status (docs/status.md)

## Commands (run inside each folder)
- api:      npm ci · npm run lint · npm run generate:kotlin
- backend:  ./gradlew build · ./gradlew test (needs Docker: Testcontainers)
            ./gradlew bootRun (needs DATABASE_URL, FIREBASE_CREDENTIALS_B64)
- android:  ./gradlew assembleDebug · ./gradlew testDebugUnitTest
            (JDK 17, needs app/src/google-services.json)
- Root settings.gradle.kts is an IDE-only composite build (includes
  backend and android). Don't add build logic there; keep both
  wrappers on the same Gradle version as the root one.
- No ktlint/detekt. Android Lint (./gradlew lint) exists but CI doesn't run it.

## API contract
- api/openapi.yaml is the source of truth. It is hand-written, not
  generated from the backend.
- Android generates its Retrofit client from it at build time
  (openApiGenerate -> build/generated/openapi), never committed.
- Backend: OpenApiSpecDriftTest fails if controllers drift from the spec.
- Wire rules in api/README.md: epoch-ms timestamps, metric units,
  server-assigned updatedAt/revision/deletedAt, opaque cursor.

## Conventions
- Package root com.rukavina.gymbuddy (backend and android).
- Client-generated UUIDv7 ids. Soft deletes (tombstones).
- Metric storage, imperial only at display.
- Backend packages: api, domain, persistence, sync, auth, config.
- Android: domain/ has no Android or Room imports. Room entities map to
  domain models in data/local/mapper.
- Flyway migrations: backend/src/main/resources/db/migration/V<n>__name.sql
- Decisions are ADRs: docs/adr/NNNN-*.md.
- UI is designed in Figma first. UI changes follow a linked Figma frame
  (from roadmap Plan 3 on).

## Rules
- Keep summaries short. I often read on my phone.
- Never change api/openapi.yaml without saying so explicitly.
- Never commit directly to main. Work on develop or a feature branch off
  develop. main only via PR from develop, once CI is green.
- Pushing develop (or a feature branch) is allowed. Never push main.
- Commits end with a Co-Authored-By line for Claude. No Claude-Session
  line, and no "Generated with Claude Code" footer in PRs.

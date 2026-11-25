# GymBuddy

Offline-first workout tracker. Native Android and iOS clients against a
Kotlin backend, sharing nothing but the OpenAPI contract.

## Repo
- api/      OpenAPI 3.1 contract (openapi.yaml), Spectral lint, docs site
- android/  Kotlin, Compose, Hilt, Room client (has its own CLAUDE.md)
- backend/  Kotlin, Spring Boot, PostgreSQL, Flyway
- ios/      Swift/SwiftUI client, not started; comes after Android v2
- docker/   compose.yaml for local dev; server/ is the VPS stack
- docs/     index in docs/README.md: roadmap, status, plans, ADRs,
            infrastructure.md (hosting, CI/CD, backups), third-party.md
- design/   DESIGN.md is the Compose build guide (tokens, components)
- legal/    privacy, terms, account deletion pages (build.py)
- PRODUCT.md  users, positioning, public name "Gym Tracker"

## Where we are
- Active plan: docs/plans/ACTIVE → that folder's progress.md (newest
  facts, gotchas). /step runs the next step; /council only for big
  decisions.
- Release loop: android/README.md "Release" + ADR 0017. Bump
  appVersion on develop, add a changelog in
  android/fastlane/metadata/android/en-US/changelogs/, log the
  build in docs/plans/release/closed-test-updates.md.

## Commands (run inside each folder)
- api:      npm ci · npm run lint · npm run generate:kotlin
- backend:  ./gradlew build · ./gradlew test (needs Docker: Testcontainers)
            ./gradlew bootRun (needs DATABASE_URL, FIREBASE_CREDENTIALS_B64)
- android:  ./gradlew assembleQaDebug · ./gradlew testQaDebugUnitTest
            (JDK 17, needs app/src/{qa,prod}/google-services.json;
            flavors qa → test backend, prod → prod backend)
- Root settings.gradle.kts is an IDE-only composite build (includes
  backend and android). Don't add build logic there; keep both
  wrappers on the same Gradle version as the root one.
- No ktlint/detekt. Android Lint runs in CI (lintQaDebug, baseline
  app/lint-baseline.xml); only new issues fail. Run lintQaDebug
  with build + tests before every commit.

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
- UI is designed in Figma first. UI changes follow a linked Figma frame;
  on-device feedback wins over Figma.

## Rules
- Keep summaries short. I often read on my phone.
- Nothing may block future monetization: only commercial-friendly
  licences (MIT, Apache 2.0, BSD, OFL, CC0, CC BY with credit) for
  code, fonts, icons and data; never NC, ND, SA or GPL/AGPL. Record
  each third-party asset's source and licence in docs/third-party.md.
  Flag choices that
  would make paid features or ads hard later.
- Never change api/openapi.yaml without saying so explicitly.
- Never commit directly to main. Work on develop or a feature branch off
  develop. main only via PR from develop, once CI is green.
- Pushing develop (or a feature branch) is allowed. Never push main.
- Commits end with a Co-Authored-By line for Claude. No Claude-Session
  line, and no "Generated with Claude Code" footer in PRs.

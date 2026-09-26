# GymBuddy

## Architecture & Execution Plan

**Multiplatform rebuild: Kotlin backend, native Android, native iOS**

Version 2.0 — 15 August 2026
Author: Karlo Rukavina
Status: Phases 0, 1 and 2 complete. Phase 3 next.

*Supersedes version 1.0 (9 August 2026). This is a full replacement, not a diff — where the original plan and the built system disagree, this document describes what was built and why the plan changed.*

---

## 1. How to use this document

This is the reference you build from. It records what has been built, what remains, and why each decision was made, so that in four months you can re-read the rationale instead of re-deriving it.

Four kinds of statement appear, and they carry different weight:

- **Decisions** are settled. Changing one is a deliberate act with a cost, not a preference to revisit while coding. They are collected in §4, and amendments made during execution are marked.
- **As-built** descriptions record what exists in the repository now. These are facts, not intentions.
- **Specifications** describe the target state for work not yet done. These are precise and should be implemented as written.
- **Recommendations and open questions** are flagged explicitly. Where the reasoning is uncertain, the document says so rather than projecting false confidence.

### 1.1 The governing scope rule

This rule determined what belonged in Phase 1 and continues to govern scope decisions:

> **If a feature requires changing the shape of an existing table, it goes in v1.**
> **If a feature only adds a new table, or is derived from data already stored, it can wait.**

Restructuring an existing table after a backend exists costs a schema migration, a data migration, a client migration, and a compatibility window. Adding a new table later costs one migration and nothing else. Derived features — statistics, personal records, charts, volume trends — cost nothing to defer because the data is already being captured.

In practice this rule produced an unusual outcome that is worth naming explicitly, because it looks like over-engineering to anyone who doesn't know the rule: **the database contains a number of columns with no user interface.** `setType`, `restTakenSeconds`, `defaultRestSeconds`, `isFavorite`, `supersetGroup` and `derivedFromId` are all populated with defaults, round-tripped through mappers, and carried in the sync payload, but nothing in the app reads or writes them. That is deliberate. Each represents a v2 feature whose *storage* was expensive to add later and whose *interface* was cheap to defer.

### 1.2 The working method

Implementation is delegated to an agent (Claude Code) working against scoped prompts, with review before each commit. The architect role — deciding what to build, in what order, and why — stays with the developer.

This has three consequences the document reflects:

**Estimates are unreliable in a specific direction.** Mechanical work — type propagation, transcription, schema changes with a compiler as ground truth — completes far faster than a hand-coding estimate. Work requiring judgement the agent cannot verify — UI feel, cross-cutting correctness, anything on an unfamiliar platform — compresses much less. §13 revises the schedule accordingly.

**Prompts need explicit scope boundaries.** Every task specification in this document includes an out-of-scope list. Without one, an agent will helpfully do adjacent work, producing diffs that are correct but unreviewable.

**Verification cannot be delegated.** A green build proves types align. It proves nothing about whether hidden state survives a reference refresh, whether a unit conversion round-trips losslessly, or whether a rejected save leaves the app in a coherent state. Each phase below carries manual verification steps for exactly this reason, and the ones that matter are marked.

---

## 2. Status

### 2.1 Completed

| Phase | Description | Status |
|---|---|---|
| 0 | Repository restructure into a monorepo | Complete |
| 1 | Schema hardening — offline-first, sync-ready data model | Complete |
| 2 | OpenAPI contract, hand-authored and published | Complete |

### 2.2 Remaining

| Phase | Description | State |
|---|---|---|
| 3 | Spring Boot backend | Next |
| 4 | Android sync engine | Blocked on 3 |
| 5 | Android v1 complete and released to closed testing | Blocked on 4 |
| 6 | Native iOS client | Blocked on 3; may begin in parallel with 5 |
| 7 | Store submission for both platforms | Blocked on 5 and 6 |

### 2.3 As-built summary

The repository is a monorepo at `github.com/rukavina-git/GymBuddy`:

```
api/         OpenAPI 3.1 specification, tooling, published docs
android/     Native Android client — Kotlin, Compose, Hilt, Room
backend/     Empty. Phase 3.
ios/         Empty. Phase 6.
docs/        Architecture notes, ADRs
docker/      Empty. Phase 3.
```

The Android app carries roughly 19,000 lines of Kotlin across 160-odd files, builds against a schema at database version 24, and holds 60 default exercises and 13 default templates as bundled seed data. Domain-layer test coverage is 85% of lines, measured by JaCoCo with UI and generated code excluded.

The API specification defines two sync endpoints and 24 schemas, lints clean under Spectral, generates a compiling Kotlin Retrofit client, and is published as Swagger UI at `rukavina-git.github.io/GymBuddy` via a GitHub Actions workflow that rebuilds on every push touching `api/`.

---

## 3. System overview

### 3.1 Shape

```
                    ┌──────────────────────────┐
                    │   Firebase (Google)      │
                    │   Auth · Crashlytics ·   │
                    │   Analytics              │
                    └───────────┬──────────────┘
                                │  ID token (JWT)
                                │
      ┌─────────────────────────┼─────────────────────────┐
      │                         │                         │
┌─────┴──────┐          ┌───────┴────────┐         ┌──────┴─────┐
│  Android   │          │  OpenAPI 3.1   │         │    iOS     │
│  (Kotlin)  │─────────▶│   contract     │◀────────│  (Swift)   │
│            │          │                │         │            │
│  Room DB   │          └───────┬────────┘         │  local DB  │
│  Outbox    │                  │                  │  Outbox    │
└────────────┘                  │                  └────────────┘
                                │
                    ┌───────────┴──────────────┐
                    │  Spring Boot (Kotlin)    │
                    │  JWKS verify · REST      │
                    │  Sync engine · Flyway    │
                    └───────────┬──────────────┘
                                │
                    ┌───────────┴──────────────┐
                    │  PostgreSQL              │
                    │  user data · reference   │
                    │  data · change log       │
                    └──────────────────────────┘
```

The two mobile clients are fully independent. Neither knows the other exists. The only shared artefact is the OpenAPI specification, which is hand-authored and was committed before either implementation.

### 3.2 Why not Kotlin Multiplatform

KMP was evaluated and rejected before Phase 0. The reasoning, recorded so it does not get relitigated:

With a backend that owns business logic, the shareable surface shrinks to a few hundred lines of orchestration glue. The domain layer at the time was 808 lines, most of it thin CRUD coordination that migrates *into* the backend once one exists. Building KMP infrastructure — the Objective-C interop boundary, SKIE or KMP-NativeCoroutines, expect/actual declarations, a multiplatform build toolchain — to share that much code is a poor trade.

The stated career goal is employability as both an Android and an iOS developer. Compose Multiplatform in particular would allow shipping to iOS without meaningfully learning iOS, which defeats the purpose. "I shipped native Android and native iOS clients against a Kotlin backend I wrote" is a stronger and more legible claim than "I used KMP."

**What is given up:** genuine duplication of the offline persistence and sync engine across Kotlin and Swift. This is real and remains the strongest argument for KMP in this specific project. It is mitigated — not eliminated — by deliberately designing the sync protocol to be as simple as possible (§6), which reduces the duplicated logic to a few hundred lines per platform.

**What Phase 1 revealed about this decision.** The domain layer grew from 808 lines to roughly 1,400 during schema hardening, and now includes non-trivial shared logic: `WorkoutSetValidator` (six tracking types with per-type required-field rules), `Uuid7Generator`, and the snapshot-stamping use cases. All three will need Swift equivalents. That is more duplication than the original estimate assumed, and it is worth being honest that the KMP case is marginally stronger now than when the decision was made. It is not strong enough to reverse — the interop cost and the career argument both still hold — but the duplication is real and Phase 6 will feel it.

### 3.3 Technology decisions

| Layer | Choice | Status |
|---|---|---|
| Backend framework | Spring Boot (Kotlin) | Phase 3 |
| Persistence (backend) | Spring Data JPA, with JdbcTemplate for sync-critical paths | Phase 3 |
| Database | PostgreSQL 16 | Phase 3 |
| Migrations | Flyway | Phase 3 |
| Auth | Firebase Auth; backend verifies via Firebase Admin SDK | Phase 3 |
| Local dev environment | Docker Compose — Postgres, backend, Caddy | Phase 3 |
| Crash reporting | Firebase Crashlytics | Phase 5 |
| Analytics | Firebase Analytics, consent-gated | Phase 5 |
| Android local store | Room 2.8.4 | Built |
| Android DI | Hilt | Built |
| Android UI | Jetpack Compose | Built |
| iOS local store | GRDB or SwiftData — undecided | Phase 6 |
| Contract | OpenAPI 3.1, hand-authored | Built |
| Client generation | openapi-generator, Kotlin/Retrofit2/Moshi | Phase 4 |
| Coverage | JaCoCo | Built |
| Static analysis | SonarQube | Deferred |
| Hosting | Docker Compose on an EU VPS | Phase 4 |
| Repository | Monorepo | Built |

---

## 4. Decision register

Settled decisions with compressed reasoning. Each has a stable identifier for reference in commit messages and elsewhere in this document. Decisions D-25 onward were made during execution and did not appear in version 1.0.

| ID | Decision | Rationale |
|---|---|---|
| D-01 | Native Android + native iOS, no code sharing | Career goal is dual-platform employability; shared surface is small once a backend exists |
| D-02 | Backend owns business logic; clients are thin | Minimises duplicated logic across two languages |
| D-03 | Contract-first via hand-authored OpenAPI 3.1 | Spec is the artefact, not a by-product of controller code |
| D-04 | Monorepo | Atomic changes across spec, backend and clients |
| D-05 | Spring Boot with Kotlin | CV recognisability, ecosystem maturity |
| D-06 | PostgreSQL | Correct default; sequences enable robust sync cursors |
| D-07 | Firebase Auth retained; backend verifies ID tokens | Already built; no password handling server-side |
| D-08 | All entity IDs are client-generatable UUIDs | Integer IDs are incompatible with offline-first |
| D-09 | Server assigns all `updatedAt` timestamps | Client clocks skew, drift, and are user-editable |
| D-10 | Soft deletes with tombstones, except overlay tables | Deletes must propagate to other devices |
| D-11 | Opaque sync cursor backed by a change log | Timestamp cursors have a commit-ordering race |
| D-12 | Server wins all conflicts; clients never merge | Keeps duplicated client logic minimal |
| D-13 | Workout sessions sync as whole aggregates | Removes nested-tree merge entirely |
| D-14 | Reference data is bulk-pulled with a version check | Simple, adequate at this data size |
| D-15 | Reference data is read-only on clients; user opinions live in overlay tables | A reference refresh must never destroy user preferences |
| D-16 | Local database is wiped on logout | Makes `userId` implicit on-device; removes a data-leak class |
| D-17 | Logout attempts a sync flush first, warns with specifics if data would be lost | Warning is rare and true rather than frequent and ignored |
| D-18 | Display fields are snapshotted into history at write time | History must not mutate when the library updates |
| D-19 | Canonical units are metric in storage; conversion at display | Single source of truth; avoids unit-mixing corruption |
| D-20 | Profile syncs; app preferences remain device-local | Theme and display units are device concerns |
| D-21 | Android v1 excludes statistics and charts | Derived data; costs nothing to defer |
| D-22 | Full offline for logging and custom entity CRUD; reference data cached read-only | Gym environments have unreliable connectivity |
| D-23 | iOS may ship online-only first, with a repository seam | Avoids rewriting the iOS data layer later |
| D-24 | In-app account deletion built in Phase 3 | App Store guideline 5.1.1(v) and GDPR both require it |
| **D-25** | **UUIDv7 rather than v4, implemented in-project** | Time-ordered IDs give B-tree insert locality and readable creation order; ~20 lines, no dependency, and needed again in Swift |
| **D-26** | **Reference data is deprecated, never deleted** | User templates referencing a removed exercise would break; a `deprecated` flag keeps entries resolvable while hiding them from browsing |
| **D-27** | **`TemplateExercise` snapshots `exerciseName` and `exerciseTrackingType`** | Extension of D-18 to templates; a template referencing an absent exercise must still render, and the tracking type determines how planned targets display |
| **D-28** | **Sync payloads use one typed array per entity type** | `oneOf` + discriminator generates poorly across Kotlin and Swift; the model has no cross-type ordering requirement, so separate arrays cost nothing |
| **D-29** | **Deletions arrive as entities with a non-null `deletedAt`, not as a separate list** | One code path on the client instead of two |
| **D-30** | **No shared Gradle module for the domain layer** | `core/domain` as a JVM module would structurally enforce platform independence, but the disruption mid-migration outweighed the benefit; convention plus review is sufficient at this size |
| **D-31** | **No rest timer in v1; `restSeconds` displays as prescriptive text** | A timer that dies on screen lock is unusable, and a correct one needs notification permissions and alarm scheduling; the guidance value is available for free |
| **D-32** | **`SetType` has four values and no v1 interface** | Warm-up versus working is the distinction that makes v2 statistics correct; recording it by default costs nothing, exposing a picker adds clutter to every set row |
| **D-33** | **RPE dropped entirely** | Unlike the deferred columns, RPE has no default that means anything — an unfilled RPE column is noise, not deferred capability |

---
## 5. Data model — as built

This section describes what exists in the Android database now, at version 24. It replaces §4 of version 1.0, which described a target rather than a result.

### 5.1 What Phase 1 changed

The audit that opened Phase 1 found ten problems. All were fixed. Recording them here matters because each represents a class of mistake that could recur, and because the fixes explain otherwise puzzling aspects of the current schema.

| # | Problem | Resolution |
|---|---|---|
| 1 | `Exercise`, `PerformedExercise`, `TemplateExercise` used integer IDs, incompatible with offline creation | All IDs are UUIDv7 strings, generated by an injected `IdGenerator` |
| 2 | `isHidden` and `note` were columns on the shared exercise row, destroyed by any reference refresh | Moved to `user_exercise_state` / `user_template_state` overlay tables |
| 3 | Workout history rendered exercise names by joining the live table, so a rename rewrote history | `PerformedExercise` snapshots name, category, tracking type and primary muscles |
| 4 | `WorkoutSet` held only weight and reps, unable to represent duration or distance exercises | Four nullable measurement fields plus `ExerciseTrackingType` on `Exercise` |
| 5 | Exercise order within a session depended on unspecified database order | `orderIndex` added to `PerformedExercise`; explicit `ORDER BY` in the DAO |
| 6 | `UserProfile` was a domain model carrying Room annotations | Split into `UserProfile` and `UserProfileEntity` with a mapper |
| 7 | Domain models lived under `data/model`, inverting the dependency direction | Moved to `domain/model` |
| 8 | Five filter queries did not exclude hidden exercises | All now join the overlay and filter |
| 9 | Zero test coverage across 18,600 lines | 85% domain-layer line coverage |
| 10 | `fallbackToDestructiveMigration()` enabled | Still enabled — see §5.9 |

### 5.2 Identifier strategy

All entity identifiers are UUIDv7 strings, generated by whoever creates the entity. Clients generate them for user-created data; the server generates them for reference data.

Storage is `TEXT` in SQLite and will be `uuid` in PostgreSQL. On the wire the OpenAPI spec declares `format: uuid`, which causes the generated Kotlin client to produce `java.util.UUID` rather than `String`. **The Phase 4 DTO-to-domain mapper must convert in both directions.** This is a small but real cost of the `format: uuid` declaration, noted here so it is not discovered mid-implementation.

`Uuid7Generator` implements RFC 9562 layout: 48 bits of Unix milliseconds, 4 version bits, 12 random bits, 2 variant bits, 62 random bits. It takes an injected `Clock`, which makes it deterministic under test. One documented simplification: `rand_a` is random rather than a monotonic counter, so ordering within a single millisecond is not guaranteed. At this application's write rate — a handful of entities per user action — that is an acceptable trade for a simpler implementation.

**Reference data UUIDs are permanent.** The 60 exercise IDs and 13 template IDs in the bundled seed JSON must appear identically in the backend's Flyway seed migration. If they diverge, a user seeds locally, syncs, and receives the same exercises under different IDs: the library silently doubles and templates point at rows the server does not know about. This is R-08 in the risk register and it is the single most consequential carry-over into Phase 3.

The seed files are the source of truth until the backend exists. **The Flyway seed migration should be generated from them rather than written by hand.**

### 5.3 Data ownership classes

Every table falls into exactly one class, and the class determines sync behaviour. Getting this classification right is what prevents the Problem 2 failure from recurring in new forms.

| Class | Owner | Sync mechanism | Client writes? |
|---|---|---|---|
| Reference data | Server | Versioned bulk pull | Never |
| User data | User | Per-entity delta via change log | Yes |
| Overlay data | User, keyed to a reference entity | Per-entity delta, upsert-only | Yes |
| Device data | Device | Not synced | Yes, locally |

The general principle, which will recur whenever a feature is added: **server-owned reference data is read-only on the client, and anything the user thinks *about* that data lives in a separate user-owned table.** Hidden flags were merely the first instance. Favourites, personal notes and per-exercise rest preferences are all the same shape, which is why one overlay table handles all of them rather than three mechanisms.

### 5.4 Exercise

Reference and custom exercises share one table, distinguished by `source`. Because both use UUIDs there is no collision risk, and every read path is a single join regardless of source.

```
exercises
─────────────────────────────────────────────────────────────────
  id                  TEXT      PK    UUIDv7
  source              TEXT            EntitySource: DEFAULT | CUSTOM
  ownerId             TEXT?           Firebase uid; null for DEFAULT
  derivedFromId       TEXT?           provenance for future duplicate-and-edit
  deprecated          INTEGER         hidden from browsing, still resolvable

  name                TEXT
  description         TEXT?
  instructions        TEXT            JSON array
  tips                TEXT            JSON array

  primaryMuscles      TEXT            JSON array of MuscleGroup
  secondaryMuscles    TEXT            JSON array of MuscleGroup
  equipmentNeeded     TEXT            JSON array of Equipment
  category            TEXT            ExerciseCategory
  exerciseType        TEXT            COMPOUND | ISOLATION
  difficulty          TEXT            DifficultyLevel
  trackingType        TEXT            ExerciseTrackingType

  videoUrl            TEXT?
  thumbnailUrl        TEXT?

  updatedAt           BIGINT
  deletedAt           BIGINT?
  revision            INT
  syncState           TEXT
```

`isCustom` and `createdBy` from the original schema are gone, replaced by `source` and `ownerId`. `isHidden` and `note` moved to the overlay.

**Write rule:** rows with `source = 'DEFAULT'` are read-only on the client, enforced in the repository layer rather than by hiding the edit button. A UI-only guard is eventually bypassed by a code path someone forgets about.

### 5.5 Tracking types

```kotlin
enum class ExerciseTrackingType {
    WEIGHT_REPS,        // Bench press, squat — the common case
    REPS_ONLY,          // Push-ups, crunches
    WEIGHT_DURATION,    // Weighted plank, loaded hold
    DURATION,           // Plank, wall sit
    DISTANCE_DURATION,  // Running, rowing
    WEIGHT_DISTANCE     // Farmer's walk, sled push
}
```

Seed data distribution: 52 `WEIGHT_REPS`, 7 `REPS_ONLY`, 1 `DURATION`. The three remaining values have no default exercises — they exist so the schema supports custom exercises and future library additions without restructuring.

The classification rule applied was that `REPS_ONLY` is used only where external load is genuinely unusual. Dips, pull-ups, chin-ups, reverse lunges and Russian twists are `WEIGHT_REPS` because they are commonly loaded, and since `weightKg` is nullable this costs nothing when unweighted while preserving the option.

The tracking type drives three things: which fields the set editor presents, which fields validation requires, and how v2 statistics will aggregate. The third is why it could not be deferred — retrofitting it would mean back-filling every exercise *and* every historical performed exercise, since the snapshot captures it too.

### 5.6 Workout set

```
workout_sets
─────────────────────────────────────────────────────────────────
  id                    TEXT      PK
  performedExerciseId   TEXT      FK -> performed_exercises (CASCADE)
  orderIndex            INT

  weightKg              REAL?     kilograms
  reps                  INT?
  durationSeconds       INT?
  distanceMeters        REAL?

  setType               TEXT      WARMUP | WORKING | DROP | FAILURE
  isCompleted           INTEGER
  restTakenSeconds      INT?      no v1 UI — see D-31
```

All four measurements are nullable; which are required is determined by the parent exercise's tracking type:

| Tracking type | Required | Optional |
|---|---|---|
| `WEIGHT_REPS` | `reps` | `weightKg` |
| `REPS_ONLY` | `reps` | — |
| `WEIGHT_DURATION` | `durationSeconds` | `weightKg` |
| `DURATION` | `durationSeconds` | — |
| `DISTANCE_DURATION` | `distanceMeters`, `durationSeconds` | — |
| `WEIGHT_DISTANCE` | `distanceMeters` | `weightKg` |

`WorkoutSetValidator` is a pure object in `domain/validation` implementing exactly this table, returning a sealed result rather than throwing. It is called from `ValidateWorkoutSessionSetsUseCase`, which sits in front of both `CreateWorkoutSessionUseCase` and `UpdateWorkoutSessionUseCase` — the only two paths that persist a session.

**The in-progress carve-out matters.** A set with `isCompleted = false` always passes validation, because a user who has added a set row but not typed into it yet legitimately has every measurement null. Validation applies only at completion. Getting this wrong in either direction is bad: too strict and the app blocks you mid-workout, too loose and invalid sets reach history.

Zero weight is valid — bodyweight movements. Zero reps on a completed set is rejected: a completed set of zero reps is not a set. Zero duration and zero distance are likewise rejected where required.

### 5.7 Performed exercise and the history snapshot

```
performed_exercises
─────────────────────────────────────────────────────────────────
  id                     TEXT   PK
  workoutSessionId       TEXT   FK -> workout_sessions (CASCADE)
  orderIndex             INT

  exerciseId             TEXT   soft reference, NOT a foreign key

  exerciseName           TEXT   ┐
  exerciseCategory       TEXT   │ write-once snapshot
  exerciseTrackingType   TEXT   │ never updated
  exercisePrimaryMuscles TEXT   ┘

  supersetGroup          INT?   no v1 UI — v2 grouping
  notes                  TEXT?
```

The snapshot fields are written once, at creation, and never updated — not when the library refreshes, not when a custom exercise is renamed. This is the invoice-and-product-catalogue pattern: records that must survive their references copy what they display.

`exerciseId` is deliberately not a foreign key. A real FK with `ON DELETE CASCADE` would destroy workout history when an exercise is removed; with `ON DELETE RESTRICT` it would make exercise deletion impossible. As a soft reference it supports "history for this exercise" while tolerating an absent target.

`exerciseTrackingType` must be snapshotted alongside the name because it determines how the historical sets render. If a mis-seeded exercise's tracking type were ever corrected, old rows would otherwise become unrenderable.

**Enforcement.** Three UI construction sites build a `PerformedExercise` with placeholder snapshot values, relying on the use case to overwrite them before persistence. That contract is enforced by a guard in `WorkoutSessionRepositoryImpl` which throws if `exerciseName` is blank at persist time — turning a future silent corruption into an immediate crash. The equivalent guard exists for `TemplateExercise`.

### 5.8 Remaining entities

**`workout_sessions`** carries `startedAt` (renamed from `date`), `endedAt` (null while in progress), `notes`, `templateId` and `templateTitle` as a soft-reference-plus-snapshot pair, plus `durationSeconds`. Duration is retained rather than derived from `endedAt - startedAt` because they differ meaningfully — elapsed wall-clock time includes the twenty minutes spent talking to someone between sets.

**`workout_templates`** and **`template_exercises`** mirror the exercise structure. `TemplateExercise` carries `exerciseName` and `exerciseTrackingType` as snapshots (D-27), plus `plannedSets`, nullable `plannedReps`, and `plannedDurationSeconds`, `plannedDistanceMeters`, `plannedWeightKg` mirroring the measurement fields on `WorkoutSet`. Without those, a template could only express weight-and-reps exercises — the Plank entries in the seed data previously stored 60 in `plannedReps` with a comment explaining it meant seconds.

**`user_exercise_state`** and **`user_template_state`** are the overlay tables:

```
user_exercise_state
  exerciseId          TEXT   PK
  isHidden            INTEGER
  isFavorite          INTEGER   no v1 UI
  note                TEXT?
  defaultRestSeconds  INT?      no v1 UI
  updatedAt           BIGINT
  revision            INT
  syncState           TEXT
```

These tables are **sparse** — a row exists only if the user has expressed an opinion. A 60-exercise library with four hidden produces four rows.

They are **upsert-only, with no tombstones**. This is a deliberate deviation from D-10, documented in KDoc on both entities. "Cleared" means all flags false and note null, not a deleted row. The rows are tiny and never garbage-collected, and the asymmetry removes an entire delete-versus-update race from the sync path.

Custom exercises use the overlay too, even though their `isHidden` could live on the row they own. One mechanism, not two.

**Orphan handling:** when a library update removes a default exercise, the overlay row is left in place. Harmless, and if the exercise returns the preference comes back.

**`user_profile`** syncs. `preferredUnits` was removed from it in Phase 1 and now lives in device preferences (D-20).

**`app_preferences`** is device-local and never synced: theme, `preferredUnits`, notification settings, and — from Phase 5 — `analyticsConsent`.

### 5.9 Sync metadata

Every user-owned entity carries `updatedAt`, `deletedAt`, `revision` and `syncState`. The nested children of an aggregate — `PerformedExercise`, `WorkoutSet`, `TemplateExercise` — deliberately do not: the parent's metadata governs the whole tree (D-13).

`updatedAt` will be server-assigned once a backend exists. Until then it is stamped locally at write time from an injected `Clock`, in the repository implementations, so there is one place to change when the server takes over.

`syncState` (`SYNCED` / `PENDING` / `CONFLICTED`) is local bookkeeping only. It is never transmitted, and it deliberately does not appear in the OpenAPI schema.

`revision` is server-assigned and stays 0 locally. The column exists; nothing increments it yet.

Deletes are tombstones: `deletedAt` is set and `updatedAt` bumped, and every read query filters `deletedAt IS NULL`. The exception is the overlay tables, per §5.8.

**`fallbackToDestructiveMigration()` remains enabled.** This was correct throughout Phase 1 — the schema changed 24 times and there were no users. **It must be removed in Phase 5, replaced by real Room migrations, before any build reaches a store.** This is the single most dangerous line in the Android codebase right now, because it is easy to forget and its failure mode is total user data loss on the first schema change after release.

### 5.10 Units

Storage is metric everywhere: kilograms, centimetres, metres. Column names carry the unit (`weightKg`, `heightCm`, `distanceMeters`) so a mismatch is visible at the call site.

Conversion happens only at the display boundary. `UnitConverter` formats to two decimals with trailing zeros trimmed, so 10 kg reads as "22.05 lb" and 20 kg reads as "20 kg" rather than "20.00 kg". **The round trip must be lossless** — metric to imperial and back returns the stored value exactly, because rounding is applied to display strings only and never written to storage.

This was verified by hand and is covered by `UnitConverterTest`. It matters more than it looks: a lossy round trip means stored data degrades slightly every time a user toggles the setting.

---
## 6. Sync protocol

### 6.1 Design principles

The protocol is deliberately unsophisticated. Because client logic is implemented twice — once in Kotlin, once in Swift — every unit of cleverness is paid for twice and offers two independent opportunities to diverge subtly. Simplicity here is the mitigation for the main cost of rejecting KMP.

1. The server owns all conflict resolution. Clients never merge anything.
2. The server assigns all timestamps and revisions. Client clocks are untrusted.
3. Clients push, then pull. Never simultaneously, never interleaved.
4. The server's response is authoritative and overwrites local state without question.
5. All writes are idempotent, keyed by entity UUID.
6. Aggregates sync whole.

If a design question arises later that these principles do not answer, prefer the simpler option.

### 6.2 The change log and cursor

Delta pull is driven by a server-side change log rather than by timestamps.

```sql
CREATE TABLE change_log (
    seq          BIGSERIAL PRIMARY KEY,
    user_id      TEXT        NOT NULL,
    entity_type  TEXT        NOT NULL,
    entity_id    UUID        NOT NULL,
    operation    TEXT        NOT NULL,   -- UPSERT | DELETE
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_change_log_user_seq ON change_log (user_id, seq);
```

One row per mutation, written in the same transaction as the mutation itself.

**Why not a timestamp cursor.** Timestamp cursors have a failure mode that is rare, silent, and permanent. Transaction A begins at T1 and commits at T3. Transaction B begins at T2 and commits at T2. A client syncing at T2.5 reads everything up to T2.5 and advances its cursor. Transaction A's change, stamped T1, is now behind the cursor and will never be delivered. The client is permanently missing data with no error and no way to detect it.

**An honest caveat about sequences.** `BIGSERIAL` does not fully eliminate this. Sequence values are allocated at insert time, not commit time, so two concurrent transactions can commit out of sequence order: T1 takes seq 100, T2 takes seq 101, T2 commits first. A reader in that window sees 101 and not 100, and advancing the cursor to 101 loses row 100 permanently. This is the same class of bug, narrower in window.

The rigorous fixes are transaction-snapshot tracking (`pg_current_snapshot()`, comparing `xmin`) or refusing to read past the oldest in-flight transaction. Both are more machinery than this system warrants.

**The practical fix, and the one to implement:** take a Postgres advisory lock keyed on the user ID for the duration of each sync mutation transaction. Since all writes for a given user originate from that user's own devices, and the number of concurrent devices per user is one or two, contention is negligible. Serialising change-log inserts per user makes the sequence strictly monotonic per user, which is all the cursor needs.

This is worth doing rather than hoping, because the failure is silent data loss and the fix is roughly five lines.

**The cursor is opaque to clients:**

```
cursor := base64url(json({"v": 1, "seq": 128374}))
```

Encoding it opaquely means the internal representation can change later — to a composite cursor, or a different ordering key — without a breaking API change. If the cursor were a raw sequence number, the implementation would be frozen into the public contract on day one.

Clients must never parse, construct, or compare cursors. This is stated in the OpenAPI description of the field because it is exactly the kind of thing a future implementer is tempted to do.

### 6.3 Tombstone retention

Tombstones are retained for **90 days**, after which a scheduled job hard-deletes them along with their change-log rows.

A client offline longer than the retention window cannot be brought up to date by deltas, because the deletions it missed no longer exist to be replayed. The server detects this by comparing the client's cursor against the oldest retained sequence number and responds `410 Gone` with `CURSOR_EXPIRED`.

The client's response is to flush its outbox, discard all local user data, and perform a full initial sync. Custom exercises, templates and history all come back from the server; only unsynced local changes are lost.

Deciding this now costs a paragraph. Discovering it in production means a user with corrupted state and no diagnostic path.

### 6.4 Push

`POST /v1/sync/push` takes one typed array per entity type (D-28) and returns a per-entity result array.

Per-entity status values:

| Status | Meaning | Client action |
|---|---|---|
| `APPLIED` | Accepted; `updatedAt` and `revision` returned | Overwrite local, clear outbox row |
| `CONFLICT` | Stale `revision`; server state is newer | Overwrite local with server state, discard local change, log |
| `INVALID` | Failed server-side validation | Drop from outbox — retrying will never succeed |
| `FORBIDDEN` | Not owned by caller | Drop from outbox, log |

**Batch semantics.** Each entity is applied in its own transaction, and a failure does not abort the batch. This matters: a single malformed entity must not block the queue behind it, because a permanently stuck outbox is unrecoverable without a reinstall.

**Idempotency** is keyed on the entity UUID rather than a separate mutation ID. Re-pushing an entity the server has already applied at the same revision returns the existing state rather than duplicating. This handles the case where the server commits successfully but the response is lost.

**Revision checking.** The client sends the entity carrying the `revision` it last received. The server compares against stored state: matching means apply and increment; mismatched means `CONFLICT`. A newly created entity carries revision 0 and no stored row exists, so it inserts at revision 1.

**Conflict policy (D-12).** The server always wins; the client discards its version. For workout sessions this is nearly theoretical given single-device usage, but the policy must be defined for cases where it is not — profile edits from two devices, an overlay change made offline on one phone while the other syncs.

Discarded conflicts should be logged with enough context to reconstruct what was lost. The conflict rate is the metric that tells you whether last-write-wins is causing real harm.

**Aggregate replacement.** A pushed `WorkoutSession` arrives with its full tree of performed exercises and sets. The server replaces the entire tree — deletes existing children, inserts the new ones — inside one transaction. There is no child-level merge.

### 6.5 Pull

`GET /v1/sync/pull?cursor={opaque}&limit=200` returns changed entities in the same per-type array shape, plus `nextCursor` and `hasMore`.

Omitting the cursor requests a full initial sync.

**The client loops while `hasMore` is true**, persisting `nextCursor` only after each page has been fully committed to the local database. If the app is killed mid-pagination, the next sync resumes from the last fully-applied page. Persisting the cursor before applying the page would silently skip data.

Deletions arrive as ordinary entities with a non-null `deletedAt` (D-29), so the client applies one code path rather than two.

### 6.6 Reference data sync

Reference data uses versioned bulk pull rather than the change log (D-14). It is server-owned, identical for every user, and small — 60 exercises and 13 templates, comfortably under a megabyte.

```
GET /v1/reference/version
→ { "exerciseLibraryVersion": 8, "templateLibraryVersion": 5 }

GET /v1/reference/exercises
GET /v1/reference/templates
```

The client compares the server version against its local value, which is exactly what the existing `ExerciseVersionEntity` and `TemplateVersionEntity` single-row tables already do. That mechanism survives; only its source changes from bundled asset to backend endpoint.

On a mismatch the client replaces all `source = 'DEFAULT'` rows in one transaction. Custom exercises are untouched. Overlay rows are untouched — which is the entire point of the overlay design, and the property that was verified by hand at the end of Phase 1 Group I.

**First-launch behaviour.** The bundled JSON seeds the app so it is functional before its first successful sync, including for a user who installs on a plane. After a successful reference sync the bundled data is never read again.

### 6.7 Sync triggers

| Trigger | Action |
|---|---|
| App foregrounded | Push, then pull |
| Workout session completed | Push immediately |
| Network becomes available with a non-empty outbox | Push, then pull |
| Manual pull-to-refresh | Push, then pull |
| Periodic background (WorkManager, ~6h) | Push, then pull |
| Before logout | Push only |

Deliberately **not** a trigger: every individual local write. Local writes go to the outbox and wait.

### 6.8 Logout

Logout wipes the local database (D-16), which makes the flush-and-warn flow load-bearing rather than cosmetic.

1. User taps log out.
2. Outbox empty → log out immediately, no dialog.
3. Outbox non-empty → progress indicator, attempt a flush.
4. Flush succeeds → log out immediately, no dialog. **This is the common path.**
5. Flush fails or no network → warning dialog.

The warning must be specific, built by grouping the outbox by `entityType`: "3 workouts, 1 custom exercise, 2 exercise notes have not been backed up." The confirming action names the destruction — "Log out and delete", not "OK". Discarded payloads are serialised to a Crashlytics non-fatal before wiping.

Attempting the flush first is what makes this good rather than merely correct: in practice the dialog almost never appears, so when it does it is believable rather than ignored.

### 6.9 Account deletion

Distinct from logout, and a store blocker rather than a nicety. App Store guideline 5.1.1(v) requires in-app account deletion for any app supporting account creation — a support email does not satisfy it. GDPR Article 17 imposes a parallel obligation.

`DELETE /v1/account` must, in one transaction, remove all workout sessions and their trees, all custom exercises and templates, all overlay rows, the profile row, and all change-log entries for the user. It must then delete the Firebase Auth user via the Admin SDK.

**Hard delete, no grace period** (resolving Q-03 from version 1.0). Simpler, unambiguously GDPR-compliant, and a portfolio project should not carry an unnecessary data-retention argument.

The client flow requires re-authentication immediately before deletion — Firebase requires a recent credential for sensitive operations — plus a typed confirmation and a local wipe on success.

---

## 7. Backend architecture

Not yet built. This section specifies the target; Phase 3's detailed group breakdown is a separate document.

### 7.1 Layering

```
com.rukavina.gymbuddy
├── api            REST controllers, request/response DTOs, error mapping
├── domain         Entities, value objects, domain services, validation
├── persistence    JPA repositories, JdbcTemplate for sync paths, Flyway
├── sync           Change log writer, cursor codec, push/pull orchestration
├── auth           Firebase token verification, security filter chain
└── config         Spring configuration, OpenAPI wiring
```

DTOs are distinct from domain entities and from persistence entities. Three mappings is more boilerplate than one shared class, and it is worth it: the wire format is a public contract that must remain stable independently of internal refactors.

### 7.2 Persistence

Spring Data JPA for ordinary entity access, dropping to `JdbcTemplate` for the sync-critical paths — bulk upserts, change-log writes, cursor queries. JPA's change tracking and lazy loading actively fight the pattern where you want a single explicit `INSERT ... ON CONFLICT DO UPDATE`, and the sync engine is exactly the code where implicit behaviour is most dangerous.

This split is a deliberate choice rather than a compromise. It is also the shape most Spring codebases end up in.

### 7.3 Authentication

Clients send the Firebase ID token as a bearer token. A Spring Security filter verifies it on every request using the Firebase Admin SDK, which handles JWKS fetching, caching, signature verification, and issuer and audience checks.

The Admin SDK is chosen over hand-rolled JWKS verification (roughly 100 lines with `nimbus-jose-jwt`) because account deletion needs it anyway, and having one Firebase integration is better than two mechanisms.

**This does not change the client's login options.** The Admin SDK is server-side only; Google Sign-In via Credential Manager on Android is unaffected.

On first sight of a `uid`, the backend creates the profile row — just-in-time provisioning. Tokens expire after one hour; clients refresh via the Firebase SDK and retry once on `401`.

**Sign in with Apple must be enabled as a Firebase provider in Phase 3**, not later. App Store guideline 4.8 requires it for any app offering third-party sign-in, and Google Sign-In is already implemented. Because Firebase normalises providers into the same token format, adding it early costs almost nothing; adding it late means revisiting token handling and account-linking under submission pressure.

### 7.4 Data ownership enforcement

Every user-data query filters on `ownerId` from the security context. This is not optional and must never depend on a client-supplied identifier.

Enforce it at the repository layer with a mandatory owner parameter, so a forgotten filter is a compile error rather than a data leak.

Reference data is readable by any authenticated user and writable by nobody through the API — library updates ship as Flyway migrations.

### 7.5 Testing

| Layer | Approach |
|---|---|
| Domain validation | Plain JUnit, no Spring context |
| Repositories | Testcontainers with real PostgreSQL |
| Sync engine | Testcontainers, explicit concurrency and cursor-expiry scenarios |
| Controllers | `@WebMvcTest` with mocked services |
| Auth filter | Hand-crafted JWTs signed by a test key pair |

Testcontainers over H2 specifically: the sync engine depends on PostgreSQL sequence semantics, advisory locks and transaction visibility, none of which H2 reproduces faithfully. Testing sync against H2 would give false confidence about the one component where correctness is hardest to verify.

Scenarios that must have explicit tests, because these are the ones that break in production:

- Two mutations to the same entity in one batch
- A mutation with a stale revision
- Cursor expiry returning 410
- Pagination across a page boundary with a concurrent write
- A repeated push of an already-applied entity
- A delete followed by an upsert of the same entity ID
- Concurrent writes from two simulated devices for the same user

---

## 8. API contract

### 8.1 As built

`api/openapi.yaml` is hand-authored OpenAPI 3.1, published as Swagger UI at `rukavina-git.github.io/GymBuddy` by a GitHub Actions workflow that lints with Spectral before publishing.

Tooling is pinned in `api/package.json` with a committed lockfile, so local and CI runs use identical versions:

```
npm run lint              Spectral validation
npm run docs              local HTML build
npm run generate:kotlin   Retrofit + Moshi client
```

The spec defines 24 schemas covering every Phase 1 entity, plus the sync request and response envelopes. Generated Kotlin models match the domain models exactly in field names, types and nullability — verified by inspection during Phase 2.

### 8.2 The gap

**The specification currently defines only two endpoints: `/v1/sync/push` and `/v1/sync/pull`.** Version 1.0 of this plan listed nine. Seven are missing:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/v1/me` | Bootstrap: profile, reference versions, server time |
| `PUT` | `/v1/me` | Update profile |
| `DELETE` | `/v1/account` | Delete account and all data |
| `GET` | `/v1/reference/version` | Reference library versions |
| `GET` | `/v1/reference/exercises` | Full default exercise library |
| `GET` | `/v1/reference/templates` | Full default template library |
| `GET` | `/v1/health` | Liveness, unauthenticated |

The reference endpoints are the significant omission: without them a client cannot obtain default exercises from the server at all, which means §6.6 is unimplementable as specified.

`PUT /v1/me` is arguably redundant, since `userProfile` already travels in the sync envelope. Worth deciding rather than adding reflexively.

**Closing this gap is the first task of Phase 3**, before any backend code, because the whole point of contract-first is that the contract precedes the implementation.

### 8.3 Design notes

**No per-entity CRUD endpoints.** All user-data writes go through `/v1/sync/push`. Two write paths would mean two places to maintain change-log invariants and two chances to forget one. It also means the offline path and the online path are the same code, which meaningfully reduces what can diverge between platforms.

**Error model.** Uniform across every endpoint: a code, a human-readable message, optional details, and a `traceId` correlating to server logs. The `traceId` is the difference between debugging a user-reported sync failure in ten minutes and not being able to debug it at all.

**Versioning.** The path carries `/v1`. Additive changes do not bump it. Clients send `X-Client-Version` and `X-Client-Platform` on every request — two headers, buying the ability to diagnose "sync broke for Android 1.3 only" and, if ever needed, to force-upgrade a client with a data-corrupting bug.

**Drift protection.** Spring Boot with springdoc generates a spec *from* controllers, which inverts the contract relationship. Controllers will be hand-written, and CI must compare springdoc's generated output against the committed YAML so drift breaks the build. This is what makes "the spec is the contract" true rather than aspirational.

**`api.gymbuddy.app` is listed as the production server and does not exist.** Harmless while nothing calls it; it must point at the real hostname before Phase 4, or Swagger UI's "Try it out" will fail confusingly against a domain nobody owns.

---

## 9. Android architecture — as built

### 9.1 Structure

Single Gradle module. The `core/domain` extraction proposed in version 1.0 was evaluated and declined (D-30): it would structurally enforce platform independence by making `android.util.Log` unavailable on the classpath, but the disruption mid-migration outweighed the benefit at this size.

```
com.rukavina.gymbuddy
├── domain/
│   ├── model         entities, enums — no Android imports
│   ├── id            IdGenerator, Uuid7Generator
│   ├── validation    WorkoutSetValidator
│   ├── repository    interfaces
│   └── usecase       including the snapshot-stamping use cases
├── data/
│   ├── local         Room entities, DAOs, converters, seeders
│   ├── mapper        entity ↔ domain
│   └── repository    implementations
├── ui/               Compose screens and ViewModels
├── utils             UnitConverter, TimeUtils, ImageStorageUtil
└── di                Hilt modules
```

Convention holds the domain layer clean rather than the compiler. Worth revisiting if the codebase grows substantially.

### 9.2 Testing

85% domain-layer line coverage, measured by JaCoCo with `ui/**`, generated code, Hilt factories and Room implementations excluded. XML output at the path SonarQube expects.

Coverage is deliberately uneven: use cases with real logic are at 100%, pure delegation is at 0%. Testing a one-line use case that forwards to a repository asserts that Kotlin calls functions. The uncovered 15% is almost entirely that plus data-class synthetics.

What is covered: `Uuid7Generator`, `WorkoutSetValidator`, `UnitConverter`, `TimeUtils`, both snapshot-stamping use cases, both mappers with full round-trip assertions, the create and update use cases, and the delete guards.

**What is not covered: ViewModels.** This is the real gap. `ActiveWorkoutViewModel` in particular holds a state machine that changed three times during Phase 1, and every change was behavioural. The phantom-workout bug — where a rejected save left the app showing an active session that could never be resumed — would have been caught by a ViewModel test. Turbine and `coroutines-test` are the tools; this belongs in Phase 5.

**UI tests: none, deliberately.** Smoke-level only, and not until the UI stops moving. Compose tests are slow and brittle against layout changes, and the screens will be rewritten during the v2 UI overhaul.

### 9.3 Known issues carried forward

Recorded so they are not rediscovered:

- **`fallbackToDestructiveMigration()` is still enabled.** Must be removed in Phase 5. Highest-consequence item on this list.
- **Filter queries exist that no UI reaches.** `getExercisesByDifficulty`, `getExercisesByType` and others are implemented and correct but not wired to the filter interface, which exposes only muscle group and equipment. A UI decision, not a bug.
- **Three template display sites were unified** to one `formatPlannedTarget` convention during Group K.
- **`ImageStorageUtil` deletes existing files before writing the new one**, so a failed write loses the old profile image. Also does not close its input stream inside a `use` block. Pre-existing, minor, unrelated to any phase.
- **Notes are fetched by a separate repository call** rather than read from the overlay join that already returns them. One redundant query per exercise detail view.

---

## 10. iOS architecture

Detailed design belongs to Phase 6. Constraints that hold regardless:

**A repository protocol between view models and the API client, from the first line of code (D-23).** Even in an online-only v1, view models must depend on a protocol, not on the generated client, and must consume an async stream rather than awaiting a call result.

This is the difference between adding offline later as an implementation swap and rewriting the iOS data layer. Online-only means view model → API. Offline means view model → local store, with a sync engine talking to the API independently. These are different data-flow topologies, and the protocol seam is what lets the second replace the first without touching a single view. It costs roughly a day.

**Sign in with Apple is mandatory** (guideline 4.8).

**Local store undecided.** GRDB is more predictable with direct SQL control, which matters for the overlay joins. SwiftData is more idiomatic and better portfolio material but younger, and its behaviour under sync-engine write patterns is not something to assume. Evaluate at Phase 6.

**A `PrivacyInfo.xcprivacy` manifest is required** regardless of analytics decisions, and third-party SDKs including Firebase must supply their own.

**What must be reimplemented in Swift**, per §3.2: `Uuid7Generator`, `WorkoutSetValidator`, the unit conversion logic, the snapshot-stamping rules, and the outbox and sync engine. Budget for this explicitly — it is the KMP cost coming due.

---
## 11. Cross-cutting concerns

### 11.1 Time

All timestamps are epoch milliseconds as `BIGINT` on the wire and in local storage; `timestamptz` server-side. Conversion to local time happens only at display. `java.time` and `Foundation.Date` never appear in stored models.

`Clock` is injected into every repository implementation and into `TimeUtils`, so nothing reads the system clock statically. This is what makes the greeting and birthday branches testable, and it is the seam through which server-assigned timestamps will replace local ones in Phase 4.

### 11.2 Validation

`WorkoutSetValidator` will exist in three implementations: Kotlin on Android, Swift on iOS, and Kotlin on the server. The server's is authoritative and non-negotiable — the API is a public surface even if only your own clients call it. The client implementations exist for immediate feedback without a round trip.

Keeping the rule expressed as a small pure function is what makes three implementations comparable by eye. Any change to the required-field table must be applied to all three.

### 11.3 Privacy and GDPR

Not merely a compliance chore — a portfolio asset given the professional context, and cheap when designed in.

- **Consent-gated analytics.** `analyticsConsent` lives in device preferences. Firebase Analytics collection stays disabled until consent is granted. Retrofitting consent is materially harder than building it in. Phase 5.
- **A privacy policy is required** before either store submission, reachable in-app and from a public URL. `rukavina.app` is the intended home.
- **`PrivacyInfo.xcprivacy`** on iOS; Data Safety declarations on Google Play. Both must accurately reflect what Firebase collects, which is more than most developers assume.
- **Account deletion** per §6.9.
- **Data export** is a GDPR Article 20 obligation. Trivial once sync exists — a full pull serialised to JSON — and worth building even though it is not v1-blocking.
- **EU hosting** for backend and database.

### 11.4 Observability

Backend: structured JSON logs with `traceId` and `userId`; a `/v1/health` endpoint; counters for push and pull volume, conflict rate, and cursor-expiry rate. The conflict rate is the number that tells you whether last-write-wins is causing real data loss.

Clients: Crashlytics with sync failures as non-fatals, tagged with `traceId`. A debug screen showing outbox contents, last sync time, cursor and last error — build it in Phase 4, because adding it during a live problem is far harder.

---

## 12. Infrastructure

### 12.1 Local development

Docker Compose on the development machine: PostgreSQL 16, the backend, and Caddy. The database persists between runs so there is real data to inspect and curl against.

This is deliberately the same compose file that will run in production, differing only by environment variables. Anything that works locally and not remotely is then a configuration difference, not an architecture difference.

### 12.2 Hosting

Needed from Phase 4, when the backend must be reachable from a physical phone. Not before.

Hetzner discontinued the CX22 on 13 February 2026; the current entry tier is the CX23 at roughly €5.49/month. The ARM CAX types have been intermittently out of stock, which is capacity fluctuation rather than discontinuation.

Oracle Cloud's Always Free tier remains the only genuinely free option with adequate headroom, with caveats: Oracle halved the Always Free Ampere A1 allowance from 4 OCPU / 24 GB to 2 OCPU / 12 GB on 15 June 2026 without announcement, and instances above the new limits were terminated from 18 August 2026. Even halved it is more than sufficient. The costs are capacity roulette and the trust problem a silent halving implies.

Netcup and IONOS offer entry VPS plans in the €1–3 range; check current listings rather than trusting figures reproduced here.

**Not recommended:** Railway and Render meter memory, and a JVM idling at 300–500 MB is the worst-case workload for that pricing model. Fly.io's PostgreSQL is unmanaged and deprioritised, which is the wrong trade-off for the system of record.

### 12.3 Portability rules

Follow these and changing provider is: provision box, install Docker, `git pull`, restore dump, repoint DNS.

- Ship a **Docker image**, never a buildpack deploy. Buildpacks are the main mechanism by which a PaaS becomes sticky.
- PostgreSQL reached **only via a `DATABASE_URL` environment variable**. No provider-specific client libraries, no proprietary pooler in the application.
- **No provider-native managed services.** If object storage is ever added, use an S3-compatible API.
- **Caddy or nginx in the compose file**, not a provider's ingress configuration.
- **Nightly `pg_dump` to storage the provider does not own.** This is disaster recovery, migration path and escape hatch in one cron job. **Test the restore at least once** — an untested backup is not a backup.

### 12.4 CI

Currently one workflow beyond the docs publisher: `android-build.yml`, scoped by paths, running `testDebugUnitTest` and `assembleDebug` on JDK 17.

`api-docs.yml` lints the spec with Spectral and publishes Swagger UI to GitHub Pages, gated so a failing lint does not publish.

To add:

| Phase | Addition |
|---|---|
| 3 | `backend-build.yml` — Gradle build, Testcontainers suite, springdoc drift check |
| 4 | Client regeneration check — regenerate from the spec, fail if the committed client differs |
| 5 | SonarQube with a new-code coverage gate; release build workflow triggered by tags |
| 6 | `ios-build.yml` on macOS runners |

SonarQube's default quality gate uses **new code** coverage rather than overall, which is the right choice here: 14,000 lines of untested Compose does not have to be retroactively covered, but everything new does.

---

## 13. Execution plan

### 13.1 Revised estimates

Version 1.0 estimated Phases 0 through 2 at roughly five to six weeks. They took approximately two days.

That is not a small correction and it warrants an explanation rather than a silent revision. The original figures assumed hand-coding. The actual method — scoped delegation with review — compresses mechanical work by something like an order of magnitude: type propagation across 61 call sites, JSON regeneration with reference remapping, schema field additions with mapper updates. All of it is verified by a compiler, which is exactly the condition under which delegation works well.

The compression is not uniform, and the plan should not pretend otherwise:

| Work type | Compression | Why |
|---|---|---|
| Mechanical refactoring | Very high | Compiler is ground truth |
| Transcription (spec, schemas) | Very high | Source of truth already exists |
| Greenfield with known patterns | High | Backend CRUD, Flyway migrations |
| Cross-cutting correctness | Moderate | Requires constructing scenarios by hand — overlay preservation, unit round-trips, sync races |
| UI feel and polish | Low | No automated signal for "this is wrong" |
| Unfamiliar platform | Low | Cannot evaluate whether output is idiomatic |

Phase 3 is mostly the third and fourth rows. Phase 6 is mostly the last two.

| Phase | v1.0 estimate | Revised | Notes |
|---|---|---|---|
| 0 | 1–2 days | Complete | Hours |
| 1 | 3–4 weeks | Complete | ~1.5 days |
| 2 | 1 week | Complete | ~0.5 days |
| 3 | 4–6 weeks | **4–7 days** | Sync correctness is the slow part, not the CRUD |
| 4 | 3–4 weeks | **3–5 days** | Outbox and replay; two-device convergence testing is manual |
| 5 | 2–3 weeks | **1–2 weeks** | Real migrations, ViewModel tests, store setup, closed testing has a fixed waiting period |
| 6 | 10–14 weeks | **3–6 weeks** | First Swift project; the least reliable figure here |
| 7 | 2–4 weeks | **2–4 weeks** | Review cycles are wall-clock, not effort |

**Revised total: roughly two to three months** rather than ten to fourteen, at the observed working pace.

Two caveats worth stating once. Phase 5's closed-testing requirement and Phase 7's review cycles are calendar time that no amount of delegation compresses. And the Phase 6 figure remains a guess — a first project on a new platform routinely takes twice its estimate, and this one carries reimplementation of the validator, ID generator and sync engine in an unfamiliar language.

### 13.2 Phase 3 — Backend

*Detailed group breakdown in a companion document.*

Outline:

1. Complete the OpenAPI spec — the seven missing endpoints (§8.2)
2. Spring Boot skeleton, Docker Compose with PostgreSQL, health endpoint
3. Flyway migrations for the full schema plus the change log
4. Reference data seed migration, generated from the bundled JSON
5. Firebase token verification, Sign in with Apple enabled as a provider
6. Domain and persistence layers
7. Push endpoint with per-entity results, revision checking, advisory locking
8. Pull endpoint with cursor encoding, pagination, 410 on expiry
9. Account deletion, tombstone retention job
10. Testcontainers suite covering the scenarios in §7.5
11. Backend CI with springdoc drift check

**Exit:** all sync scenarios pass; a manual push-then-pull round trip via curl returns correct deltas; cursor expiry returns 410; account deletion removes every trace; the committed spec and springdoc's output agree.

### 13.3 Phase 4 — Android sync

1. Outbox table, DAO, coalescing logic
2. Generated API client wired into the project, DTO ↔ domain mappers including UUID conversion
3. `SyncPusher`, `SyncPuller`, `ReferenceSyncer`, `CursorStore`
4. WorkManager scheduling per §6.7
5. Firebase token attachment and 401-refresh-retry interceptor
6. Sync status surfaced in the UI
7. Logout flush-and-warn flow
8. Account deletion flow with re-authentication
9. Full-resync path on 410
10. Debug screen: outbox, cursor, last sync, last error
11. Deploy the backend to real hosting

**Exit:** two devices converge on the same data; airplane-mode edits sync on reconnect; the logout warning appears only when genuinely warranted; cursor expiry triggers a clean full resync.

### 13.4 Phase 5 — Android v1 complete

1. **Remove `fallbackToDestructiveMigration()`; write real Room migrations**
2. ViewModel test coverage, particularly the active workout state machine
3. Frozen feature list only (§14)
4. ProGuard/R8 verified against a release build
5. Crashlytics wired; analytics consent gate implemented
6. Privacy policy published and linked
7. SonarQube in CI with a new-code gate
8. Play Console setup, Data Safety declarations, closed testing track

**Exit:** a release build passes a full manual pass; closed testing begins.

Google requires a closed test with a minimum tester count over a fixed period before a personal developer account can publish to production. This has been 12 testers for 14 days, but the policy has changed more than once and must be verified against current Play Console requirements before planning a launch date.

### 13.5 Phase 6 — iOS

1. Xcode project; API client generated or hand-written from the same spec
2. Firebase Auth including Sign in with Apple
3. Repository protocol seam from the first commit (D-23)
4. Swift reimplementation of `Uuid7Generator`, `WorkoutSetValidator`, unit conversion
5. Feature parity with the frozen Android v1 scope
6. Local persistence and sync engine, possibly after an online-only first release
7. `PrivacyInfo.xcprivacy`; App Store privacy labels

**Exit:** feature parity; both platforms converge on the same account data.

### 13.6 Phase 7 — Launch

Store listings, screenshots, descriptions. App Store submission — expect at least one rejection cycle. Production monitoring and backup verification. README with architecture diagram and rationale.

---

## 14. Scope

### 14.1 Android v1 — frozen

Authentication (email, Google, Apple); exercise library with search, filter, hide and personal notes; custom exercise CRUD; template CRUD including planned rest; active workout logging across all six tracking types; workout history with immutable snapshots; user profile with sync; device-local settings; full offline per §5; bidirectional sync; account deletion.

### 14.2 Deferred to v2

The distinguishing feature of this list is that **the storage for most of it already exists.** Each column was added during Phase 1 because adding it later would have required a schema migration; the interface was deferred because deferring an interface costs nothing.

| Feature | Storage state |
|---|---|
| Set type picker (warmup / working / drop / failure) | `setType` column populated, defaults to `WORKING` |
| Rest timer with actual-rest capture | `restTakenSeconds` column exists, always null |
| Per-exercise default rest | `defaultRestSeconds` on the overlay, always null |
| Favourites | `isFavorite` on both overlay tables, always false |
| Supersets and circuits | `supersetGroup` on `PerformedExercise`, always null |
| Duplicate-and-edit a default exercise | `derivedFromId` on exercises and templates, always null |
| Statistics and charts | Derived from stored data |
| Personal records | Derived from stored data |
| Body weight history | New table, purely additive |
| Health Connect / HealthKit | New integration |
| Data export | Trivial once sync exists |
| User-uploaded exercise images | New table plus object storage |
| Plate calculator | No storage impact |
| Widgets | No storage impact |
| UI overhaul, including full filter surface | No storage impact |

**The freeze is the point.** Anything not on the v1 list goes to the backlog without exception, regardless of how small it seems while already in the relevant file. Scope creep in a familiar codebase with no deadline remains the most likely cause of this project not shipping.

---

## 15. Risk register

| # | Risk | Impact | Likelihood | Status |
|---|---|---|---|---|
| R-01 | Scope creep on Android; iOS never starts | Fatal | Medium | Mitigated by §14 freeze; Phase 1 held scope well |
| R-02 | iOS takes far longer than estimated | High | High | Open. Estimate treated as optimistic |
| R-03 | Sync bugs corrupting user data | High | Medium | Open. Server-wins policy and advisory locking specified; Testcontainers scenarios required |
| R-04 | Sustained hours not achievable | High | Medium | Reduced — the revised schedule needs far fewer total hours |
| R-05 | Phase 1 migration destabilises a working app | Medium | — | **Closed.** App remained functional throughout |
| R-06 | App Store rejection | Medium | Medium | Open. 4.8, 5.1.1(v) and privacy manifest addressed in advance |
| R-07 | Play closed-testing requirement delays launch | Low | High | Open. Verify current policy; start the track early in Phase 5 |
| R-08 | Seed UUID mismatch between client and backend | Medium | Medium | **Elevated.** Now imminent — Phase 3 writes the seed migration. Generate it from the JSON, never by hand |
| R-09 | Backend costs or hosting instability | Low | Low | Open. Portability rules; tested restores |
| R-10 | Motivation loss over a long project | High | Low | Reduced by the revised timeline |
| **R-11** | **`fallbackToDestructiveMigration()` reaches production** | **Fatal for users** | **Medium** | **New.** Total data loss on the first post-release schema change. Explicit Phase 5 gate |
| **R-12** | **Change-log sequence gap loses a change silently** | High | Low | **New.** Mitigated by per-user advisory locking (§6.2); must be tested with concurrent writes |
| **R-13** | **Delegated code passes review but is subtly wrong in an untestable way** | Medium | Medium | **New.** Manual verification steps per phase; the sync engine is where this matters most |

R-02 and R-08 are the ones to watch in the near term. R-11 is the one that would be unrecoverable.

---

## 16. Open questions

| # | Question | Recommendation | Blocks |
|---|---|---|---|
| Q-01 | UUIDv4 or v7? | **Resolved: v7** | — |
| Q-02 | Hosting provider | Local Compose until Phase 4; Oracle free tier if cost matters, Hetzner CX23 otherwise | Phase 4 |
| Q-03 | Account deletion: hard or grace period? | **Resolved: hard delete** | — |
| Q-04 | Android module split? | **Resolved: single module** | — |
| Q-05 | iOS local store: GRDB or SwiftData? | Defer to Phase 6, decide with real knowledge | Phase 6 |
| Q-06 | Tombstone retention window | **Resolved: 90 days** | — |
| Q-07 | Rest timer foreground behaviour | **Resolved: no timer in v1** | — |
| Q-08 | Rest timer backgrounding | **Resolved: not applicable** | — |
| **Q-09** | **Is `PUT /v1/me` needed, given profile travels in the sync envelope?** | Probably not — omit unless a bootstrap-only update path emerges | Phase 3 spec completion |
| **Q-10** | **Does the backend need a rate limiter in v1?** | Yes, minimally — a naive client retry loop against an unprotected sync endpoint is a self-inflicted outage | Phase 3 |
| **Q-11** | **Custom domain for docs, privacy policy and API?** | `rukavina.app` subdomains; decide before Phase 5 when the privacy policy becomes mandatory | Phase 5 |
| **Q-12** | **Does iOS ship online-only first, or wait for offline?** | Decide at Phase 6 with real velocity data | Phase 6 |

---

## 17. Immediate next actions

1. Complete the OpenAPI specification — the seven missing endpoints (§8.2), resolving Q-09 in the process
2. Begin Phase 3 group A: Spring Boot skeleton, Docker Compose, health endpoint
3. Generate the Flyway reference-data seed migration from the bundled JSON, never by hand (R-08)
4. Write ADRs for the decisions made during Phase 1 that are currently only recorded here

---

*End of document.*

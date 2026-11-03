# 04 Body weight

## Problem
Body weight exists only as one number on the profile
(`UserProfile.weight`, kg). The redesign wants a history (You tab,
later Progress/Stats, BMI with height: architecture plan §BMI stats,
council 2026-11-02). Roadmap 6.4: new table, sync, spec. **Spec change
(api/openapi.yaml), Flyway migration, Room schema change.**

## Owner decisions: 1A, 2A (2026-11-03)
1. **Profile weight vs the history**
   - **A (recommended): logging an entry also sets `profile.weight`**
     when the entry is the newest one. One "current weight" for BMI and
     the profile screen, no new rule for readers. Same transaction,
     both queued.
   - **B: independent.** Profile weight stays a separate field; screens
     pick one. Two numbers can disagree.
2. **Entry point before Plan 7**
   - **A (recommended): none.** Data, sync and tests only; the screen
     comes with the You tab (Plan 7). Debug builds show the count in
     Sync debug.
   - **B: plain list + add dialog in Profile.** Throwaway UI.

## Change
- **Spec** (said out loud): new schema `BodyWeightEntry` { id (uuid,
  client UUIDv7), measuredAt (epoch ms), weightKg (number, > 0,
  ≤ 700), updatedAt, revision, deletedAt (server-assigned) }.
  `bodyWeightEntries` arrays in PushRequest and PullResponse;
  `BODY_WEIGHT_ENTRY` in EntityType. Several entries per day allowed.
  Spectral lint green.
- **Backend:** `V5__body_weight_entries.sql` (user_id, soft delete,
  revision, indexes like workout_sessions); sync + pull service and
  repository in the existing pattern (server-wins, FORBIDDEN for
  another user's id, change_log); AccountDataDeleter and the tombstone
  retention include it. OpenApiSpecDriftTest green.
- **Android:** Room v28: `body_weight_entries` table, Migration 27→28,
  schema JSON committed, MigrationTestHelper test. Entity, DAO, mapper,
  domain model, `BodyWeightRepository` (add, delete, observe) through
  OutboxRecorder. SyncLocalStore prepare/apply/wipe and the sync log
  handle the new type. GuestMerge copies it (merge and keep-as-guest).
- Decision 1A: in `add`, if newest, update the profile row too.

## Tests
- Backend: push create/update/delete, conflict (server wins), another
  user's id → FORBIDDEN, invalid weight → INVALID, pull returns it and
  its tombstone, account deletion removes it. Drift test.
- Android: migration 27→28 keeps data; repository add/delete queue the
  outbox; push/pull round trip against fake SyncApis; guest merge and
  keep-as-guest carry entries; wipe removes them; (1A) newest entry
  sets profile weight, an older one doesn't.

## Out of scope
Screens and charts (Plan 7), BMI, export (07), imperial input (display
only, Plan 7).

## Done when
`npm run lint` (api), `./gradlew test` (backend), `./gradlew
testDebugUnitTest` and `assembleDebug` (android) green.
Manual (me): none, unless 2B.

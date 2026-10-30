# Phase 5 data safety: migrations, crash reports, server logs

## Why
Nothing ships to testers until a schema change can't wipe their data.
`fallbackToDestructiveMigration()` in production is R-11 in the
architecture plan, the one risk that can't be undone. Testers also need
to be observable: crashes and sync failures must reach us without a
user report, and the backend must say which client version sent a bad
request (roadmap-details.md, Plan 2).

## Scope
1. One DB builder, schema export, v26 baseline, migration tests.
2. Remove the destructive migration fallback; a test fails if the DB
   version has no migration path from v26.
3. Crashlytics: crashes, sync failures as non-fatals with traceId,
   discarded logout data (§6.8). Off in debug builds.
4. Backend JSON logs with traceId/userId, sync counters; Android sends
   `X-Client-Version` and `X-Client-Platform`, the backend logs them.
   Spec change (documentation only).
5. (me) CI green, PR develop → main.

Out of scope: Firebase Analytics (Q-C), any schema change beyond the
baseline, alerting/dashboards, UI changes, iOS.

## Invariants
- Sync rules in android/CLAUDE.md hold.
- The DB file name and schema v26 stay as they are on installed
  devices today (`gym_buddy_db`, the one Hilt opens). No user loses
  data by updating to a build from this plan.
- From step 02 on, every Room version bump ships with a `Migration`
  and a `MigrationTestHelper` test. No destructive fallback in any
  build type.
- No crash report, non-fatal or log line contains a Firebase ID token,
  password, email or the Neon URL. User ids are the Firebase uid only.
- The backend stays a plain Docker image reached via `DATABASE_URL` +
  `DB_PASSWORD`. No provider SDKs, no log shipper.
- The only `api/openapi.yaml` change is documenting the two client
  headers in step 04. The generated client's method signatures don't
  change.
- Each step builds and passes its tests on its own.

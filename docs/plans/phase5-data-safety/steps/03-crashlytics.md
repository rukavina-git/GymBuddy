# 03 Crashlytics

## Problem
Testers' crashes and sync failures reach nobody. `SyncScheduler`
only `Log.w`s a failed cycle; `SyncException` carries the backend's
`traceId` since phase4-closeout step 02, but it's never reported.
Architecture plan §6.8 says discarded logout payloads go to a
Crashlytics non-fatal before the wipe; nothing does that today.
Firebase is already set up (Auth, `google-services` plugin).

## Change
- Add `firebase-crashlytics` (via the existing Firebase BoM) and the
  Crashlytics Gradle plugin. No Analytics (Q-C); remove the unused
  `firebase-analytics` entry from libs.versions.toml.
- Collection off in debug builds
  (`setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)` at startup,
  or the manifest flag with the release override).
- A small `CrashReporter` interface in data/ (or a core package), with
  a Crashlytics implementation bound by Hilt and a fake for tests, so
  sync code doesn't import Firebase.
- Sync failures: `SyncScheduler.runAndRecord` reports a non-fatal on
  failure, with custom keys `traceId` (if the exception has one),
  `reason`, HTTP status. Not on `CancellationException`, not on plain
  offline (`IOException`), to keep noise down.
- Logout discard (§6.8): before the wipe in the "Log out and delete"
  path, report one non-fatal with the outbox grouped by entity type
  and the entity ids. **Decision for the plan step:** §6.8 says the
  full payloads; those are workout data, which the Data Safety form
  (roadmap 4.5) must then declare. Recommend counts + ids only, unless
  I say otherwise.
- `setUserId(firebaseUid)` on sign-in, cleared on logout.

## Tests
- `SyncScheduler`: a failing cycle reports once with traceId; a
  cancelled or offline cycle doesn't.
- `LogoutCoordinator`: the discard path reports before the wipe; the
  normal path (flush succeeded) reports nothing.
- Release build: `./gradlew assembleRelease` builds with the plugin
  (mapping upload may need to be off locally).

## Out of scope
Analytics, performance monitoring, ANR tuning, backend changes.

## Done when
Tests above pass, `./gradlew testDebugUnitTest` green, release build
assembles.

Manual (me): a forced test crash in a release build shows in the
Firebase console.

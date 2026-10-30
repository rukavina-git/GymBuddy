# 02 Sync debug screen

## Problem
There is no way to see sync state on a device. When the step 06 device
checks or a tester hit a sync problem, the only evidence is logcat.
Architecture plan §11.4 asked for this screen in Phase 4, "because adding
it during a live problem is far harder".

## Change
- `SyncDebugViewModel` + `SyncDebugScreen` in ui/settings/debug.
- Shows, live from Room / the engine:
  - outbox: count by entity type, then rows (type, id, operation,
    changeVersion)
  - pull cursor (shortened), last sync time, last outcome
    (SyncOutcome), last error with its traceId
  - discarded-change log entries (from the pending-edit pull rule)
  - a "Sync now" button that goes through `SyncTriggers`, not the
    engine directly
- If the engine doesn't keep last sync time / last error yet, keep them
  in memory in `SyncScheduler` (no Room change, lost on process death,
  which is fine for a debug tool).
- Entry point: a "Sync debug" row in Settings, only when
  `BuildConfig.DEBUG`.
- Plain Material components, no Figma frame (dev-only tool, roadmap
  §2 exception).

## Tests
- ViewModel: outbox counts by type, cursor and last error mapping,
  empty state, "Sync now" calls the trigger.

## Out of scope
Editing or clearing the outbox from the screen, release-build access,
Crashlytics.

## Done when
ViewModel tests pass, `./gradlew testDebugUnitTest` and
`./gradlew assembleDebug` green. The Settings row isn't in a release
build (check `BuildConfig.DEBUG` guard in review).

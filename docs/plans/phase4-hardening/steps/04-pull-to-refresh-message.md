# 04 Pull-to-refresh message and tests

## Problem
`WorkoutSessionViewModel.refresh()` (ui/workout) calls
`SyncRequester.syncNow(MANUAL)`, which returns a Boolean. `false` covers
offline, server error and signed out, so a signed-out user sees "Couldn't
sync. Check your connection and try again." Their connection is fine;
retrying never helps. `refresh()` has no tests.

## Change
`syncNow` returns a small result instead of a Boolean, e.g.
`enum class SyncOutcome { SYNCED, SIGNED_OUT, FAILED }` in domain/sync
(no Android imports). `SyncScheduler.syncNow` returns SIGNED_OUT on the
early return, FAILED when the cycle throws. `refresh()` maps:
- SYNCED: keep the current errorMessage (as today).
- SIGNED_OUT: "Sign in to sync your workouts."
- FAILED: the current connection message.
Update every `syncNow` caller and fake (RecordingSyncRequester etc.).
`requestSync` doesn't change.

## Tests
WorkoutSessionViewModelTest, with a fake SyncRequester returning each
outcome:
- isRefreshing is true while syncNow runs, false after.
- SYNCED: no error message; an earlier error message is kept.
- SIGNED_OUT: the sign-in message.
- FAILED: the connection message.
- A second refresh() while one runs doesn't call syncNow again.
SyncSchedulerTest: syncNow returns SIGNED_OUT when signed out (no cycle
runs) and FAILED when the cycle throws.

## Done when
`./gradlew testDebugUnitTest` passes. No Room or openapi change.

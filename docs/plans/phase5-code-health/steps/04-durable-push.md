# 04 Durable push via WorkManager

## Problem
An offline edit is pushed by the network callback in `SyncScheduler`.
If the user swipes the app away before the network returns, the
callback dies and the edit waits for the next app open (Plan 2 T10).
The periodic `SyncWorker` (~6 h) is the only fallback.

## Change
- When the outbox goes from empty to non-empty (observe the outbox
  count flow, or hook the outbox insert), enqueue a unique one-off
  work `push-outbox` with `NetworkType.CONNECTED`,
  `ExistingWorkPolicy.KEEP`, exponential backoff.
- The worker calls `syncNow(SyncReason.OUTBOX_PENDING)` (new reason;
  domain enum only) and retries on FAILED, like `SyncWorker`. Signed
  out → success, no retry.
- Keep the network callback for the app-open case; the sync lock
  already prevents double runs.

## Tests
- `work-testing` with `TestDriver`: outbox insert enqueues one job;
  a second insert doesn't add another; constraints met → worker runs
  `syncNow`; FAILED → retry; signed out → success.

## Out of scope
Changing the periodic worker, push batching, server changes.

## Done when
`./gradlew testDebugUnitTest` green. Manual: airplane mode, log a set,
swipe the app away, airplane off: the set reaches the backend without
opening the app (check the sync debug screen or backend logs).

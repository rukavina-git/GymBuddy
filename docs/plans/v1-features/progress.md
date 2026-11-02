# Progress

- [x] 01 Per-account local data (steps/01-per-account-data.md) — HASH
- [ ] 02 Guest mode
- [ ] 03 Auth gate
- [ ] 04 Body weight
- [ ] 05 Profile fields
- [ ] 06 Plans + default templates
- [ ] 07 Data export
- [ ] 08 CI green, PR develop → main (me)

## Deviations
- 01: Signed out opens `gym_buddy_signed_out.db` (the step file didn't
  say). Step 02 should turn it into the guest file.
- 01: The owner check sits in SyncEngine (`isSignedIn` = auth uid ==
  holder owner, checked under the lock, also in flushOutbox). The step
  put it in the workers. Workers and scheduler are unchanged.
- 01: OutboxRecorder's write lambda now receives the transaction's
  AppDatabase, so a write and its outbox entry land in the same file.
- 01: Plan 4 push-trigger fix: KEEP while a push waits, APPEND_OR_REPLACE
  when one is RUNNING. No test for the RUNNING case.
- 01: Backup rules: comments only. The defaults already back up every
  Room file.

## Gotchas
- From 01 (reviewer, not fixed):
  - `switchTo` closes the old file right after publishing the new one. A
    repo read or write in flight, or `getPerformedExercisesWithSets`
    inside a flow, can hit the closed DB and crash. Step 02/03 should
    delay the close (after the new DB's first emission) or ref-count.
  - The UI isn't gated on the switch: after B signs in, screens show the
    previous file until `switchTo` gets the sync lock. Harmless now
    (empty signed-out file), but a guest → B leak once step 02 adds
    guest data. Gate the UI on `holder.owner == uid`.
  - AppModule deletes the legacy file if `currentUid` is null. It also
    is null when Firebase errors (`runCatching`). Only delete when
    Firebase is known to be up.
  - Legacy rename moves the main file before `-wal`. Move side files
    first.
  - Seeders read `holder.db`, not the DB the flow emitted.
  - `observeSignIn` has no test (switch before SIGNED_IN, no flip
    through the signed-out file on cold start).
- Repos/stores take `DatabaseHolder`; tests use `DatabaseHolder.fixed(db)`.
- Open from Plan 4 (manual, owner): step 03 "Don't keep activities"
  check and a release build reading a draft; step 04 airplane-mode
  push check.
- From Plan 4 step 04: `push-outbox` uses KEEP, so a set logged while
  the job is still running can wait up to 6 h. Fix it if step 01
  touches the workers anyway.
- ProfileViewModel reads the Firebase uid statically; step 05 should
  inject AuthSession so its save paths become testable.

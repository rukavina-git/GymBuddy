# Progress

- [x] 01 Per-account local data (steps/01-per-account-data.md) — 4b61dce
- [x] 02 Guest mode (steps/02-guest-mode.md) — ab07df5
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
- 02: No sign-up vs sign-in check. Rule: account empty after a good
  sync → merge without asking; otherwise (data, or sync failed) ask.
  A new account is always empty.
- 02: Sign-in completes before the question; Cancel signs out back to
  the guest (Figma asks before sign-in completes).
- 02: DatabaseHolder keeps the previous file open until the next switch
  (step 01 gotcha), instead of closing it on the switch.
- 02: Guests got a plain "Sign in or create account" button in Settings
  (reviewer BLOCKER: nothing led a guest back to Login). Not in the
  step file.
- 02: Profile and Settings read the uid per call (`?: "guest"`), so a
  switch can't leave them writing under the old owner.

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
- From 02 (reviewer, not fixed):
  - Guest flag isn't cleared when a guest with no data signs in. After
    that account logs out, Splash opens Main as a guest, not Login.
  - `active_workout_draft` is one row per file: INSERT OR IGNORE drops
    the guest's draft if the account has one, and `guestData()` ignores
    drafts (not in the outbox), so a draft-only guest never merges.
  - No tests for "erase clears the flag" (SettingsViewModel) or the
    `ownerReady` gate.
  - Merge commits, then deletes the guest file. A crash in between
    asks again and re-adds the outbox. Clear the guest outbox first.
  - `ownerReady` can skip its false frame, so `MainContent` isn't
    recreated and init-only state (Settings `isGuest`) can go stale.
  - The uid collector waits on `syncNow`; a sign-out during it waits.
- Step 03 should reuse `GuestMerge` for "account deleted elsewhere:
  keep as guest" (copy into the guest file is the reverse direction;
  not built).


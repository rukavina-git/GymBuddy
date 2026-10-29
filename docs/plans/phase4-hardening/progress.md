# Progress

- [x] 01 Logout under the sync lock (steps/01-logout-under-sync-lock.md) (a24dc90)
- [x] 02 Logout errors never leave a stuck dialog (steps/02-logout-error-states.md) (c13e7bf)
- [x] 03 Scheduler never drops a request (steps/03-scheduler-lost-request.md) (dfb0d20)
- [x] 04 Pull-to-refresh message and tests (steps/04-pull-to-refresh-message.md) (8777ad5)
- [ ] 05 Manual device checks (me)

## Deviations
- 01: SyncEngine had no sign-in knowledge, so it now takes
  `isSignedIn: () -> Boolean = { true }`. Only `sync()` checks it after
  taking the lock. flushOutbox/push/pull/syncReferenceData don't.
- 01: LogoutCoordinator's `runExclusive` has no default, so
  LogoutE2eTest changed too.
- 01: the commit hash is recorded in a follow-up commit (a commit can't
  contain its own hash).
- 02: SettingsViewModel calls FirebaseAuth.getInstance() in its
  constructor, so SettingsViewModelTest is Robolectric + FirebaseApp
  (like ProfileViewModelTest), not plain fakes. LogoutCoordinator is real,
  over in-memory Room, with fake flush/auth/runExclusive.
- 02: Failed carries `afterWarning` so `retryLogout()` repeats the step
  that failed (logout() vs confirmLogoutAndDelete()). Message is generic
  ("Couldn't log out. Please try again.") because sign-out can fail
  after the wipe already ran.
- 02: the step 02 hash is in a follow-up commit, as in step 01.
- 03: a second race, not in the step file: requestSync set
  rerunRequested only after a failed CAS, so the runner could do its
  last check and exit in between. The flag is now set before the CAS.
  The step's re-check alone still failed ~1 in 12 runs.
- 03: the "hammer from many coroutines" test became a racer test: per
  trial (x2000) one thread spins until the cycle returns, then
  requests. A 16x200 coroutine hammer passed on the old code, so it
  proved nothing. New tests are in SyncSchedulerTest (Robolectric for
  Log); SyncTriggersTest is unchanged.
- 03: the step 03 hash is in a follow-up commit, as before.
- 04: SyncWorker also called syncNow. It now retries only on FAILED;
  SIGNED_OUT counts as success, like its own signed-out check.
- 04: the scheduler's SIGNED_OUT/FAILED cases are the existing
  syncNow asserts in SyncTriggersTest, updated. No SyncSchedulerTest
  change. Message strings are constants in WorkoutSessionViewModel's
  companion so tests can reference them.
- 04: docs: KDoc on SyncOutcome, syncNow and refresh(); a hardening
  line in docs/status.md (user asked for docs to be current).
- 04: the step 04 hash is in a follow-up commit, as before.

## Gotchas
- 01: the queued-cycle test was order-dependent (reviewer SHOULD-FIX).
  It's fixed in a follow-up commit: the lock is held inside runExclusive
  and the cycle is queued with CoroutineStart.UNDISPATCHED. Removing
  the isSignedIn guard now makes it fail every time.
- 01: requestLogout drops the lock between flushOutbox and
  logOutAndDelete. An edit made in that gap is wiped without a warning.
  This predates step 01.
- 01: to make a Room write fail in tests, use a SQLite RAISE(ABORT)
  trigger. db.close() doesn't make writes throw.
- 02: "requestLogout throws" can only be faked via runExclusive (the
  wipe). The coordinator already swallows flush errors.
- 02: Room queries leave the main test dispatcher. Wait with
  `logoutState.first { it != Flushing }` or an UNDISPATCHED
  `logoutEvent.first()`, never read `.value` right after the call.
- 02 reviewer, not fixed: SHOULD-FIX: no test for retry after a plain
  logout failure (afterWarning=false). NIT: no test that data survives a
  failed wipe. NIT: Failed.message is always the same string.
- guard-bash.sh greps the whole command for push checks, so a push
  chained with text containing " main " is blocked. Push in its own
  command.
- 03: the race test is probabilistic. Old code failed it in 2 of 3
  runs, the fixed code passed 15 of 15. If it ever flakes, suspect a
  real regression before blaming the test.
- 03: the outer loop exits if signed out but leaves rerunRequested set.
  That's harmless, since the next request sets it anyway.
- 03 reviewer, not fixed: NIT: no many-requester test. NIT: the
  "exactly 2 cycles" check uses delay(1), so it's weak. NIT: follow-up
  cycle failures log the first request's reason.
- 04 reviewer, not fixed: NIT: no worker test for SIGNED_OUT ->
  Result.success() (sign-out between the uid check and syncNow). NIT:
  syncNow's runCatching also catches CancellationException, so a
  cancelled refresh reports FAILED (older code). NIT: docs/status.md
  still says CI green at b7334dd; update once CI runs on this work.
- 05 (me): signed-out pull-to-refresh should now say "Sign in to sync
  your workouts."

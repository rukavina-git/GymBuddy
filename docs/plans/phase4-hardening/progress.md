# Progress

- [x] 01 Logout under the sync lock (steps/01-logout-under-sync-lock.md) HASH
- [ ] 02 Logout errors never leave a stuck dialog (steps/02-logout-error-states.md)
- [ ] 03 Scheduler never drops a request (steps/03-scheduler-lost-request.md)
- [ ] 04 Pull-to-refresh message and tests (not detailed yet)
- [ ] 05 Manual device checks (me)

## Deviations
- 01: SyncEngine had no sign-in knowledge, so it now takes
  `isSignedIn: () -> Boolean = { true }`. Only `sync()` checks it after
  taking the lock. flushOutbox/push/pull/syncReferenceData don't.
- 01: LogoutCoordinator's `runExclusive` has no default, so
  LogoutE2eTest changed too.
- 01: the commit hash is recorded in a follow-up commit (a commit can't
  contain its own hash).

## Gotchas
- 01: test `a cycle queued behind logout does nothing once signed out`
  is order-dependent (reviewer SHOULD-FIX, not fixed). It releases a
  separate exclusive{} before logOutAndDelete re-locks, and Mutex is
  FIFO, so a cycle that is already queued runs first. Fix: block inside
  the runExclusive wrapper itself, and wait until sync() is queued.
- 01: requestLogout drops the lock between flushOutbox and
  logOutAndDelete. An edit made in that gap is wiped without a warning.
  This predates step 01.
- 01: to make a Room write fail in tests, use a SQLite RAISE(ABORT)
  trigger. db.close() doesn't make writes throw.

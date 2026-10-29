# 01 Logout under the sync lock

## Problem
`LogoutCoordinator.logOutAndDelete()` (data/sync/LogoutCoordinator.kt)
calls `auth.signOut()` then `store.wipeEverythingForLogout()` without
holding `SyncEngine`'s mutex.
- A cycle already inside `SyncEngine.sync()` can commit a pull page and
  its cursor after the wipe. The old user's data survives into the next
  account.
- If the wipe throws, the user is signed out with their data still on
  the device.

## Change
- Give SyncEngine a way to run a block while holding its mutex (e.g.
  `suspend fun <T> exclusive(block: suspend () -> T): T`).
- `logOutAndDelete()` runs, inside that block: wipe first, then sign out.
  Wiping first means a failed sign-out leaves an empty, signed-in device
  that pulls from scratch next time, which is safe.
- A cycle that was queued on the mutex must re-check sign-in after it
  acquires the lock and do nothing if signed out.
- Inject the lock runner into LogoutCoordinator as a function, like
  `flushOutbox`, so its unit tests stay plain.

## Tests
- Wipe happens before signOut; signOut not called if the wipe throws.
- Logout waits for a running cycle; nothing written after the wipe
  (fake store with a suspended pull page).
- A cycle queued behind logout does nothing once signed out.

## Done when
Tests above pass, existing LogoutCoordinatorTest and SyncTriggersTest
still pass, `./gradlew testDebugUnitTest` green.

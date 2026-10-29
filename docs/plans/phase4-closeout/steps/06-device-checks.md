# 06 Device checks against the hosted backend (me, deferred)

Deferred until I'm on the Mac. Release build pointed at
api.rukavina.app (step 05), two devices or a device and an emulator.

## From phase4-hardening step 05
- Pull-to-refresh signed in: syncs, spinner stops.
- Pull-to-refresh signed out: "Sign in to sync your workouts."
- Pull-to-refresh offline: failure message, no stuck spinner.
- Logout, clean outbox: immediate, no old data.
- Logout with unsynced edits offline: warning, "Log out and delete"
  wipes.
- Logout failure: "Couldn't log out. Please try again.", retry and
  close work.
- Logout during a running sync, then a different account: no data from
  the first.

## Phase 4 exit criteria
- Two devices, same account: an edit on one appears on the other after
  pull-to-refresh.
- Airplane mode: log a workout, reconnect, it reaches the other device.
- 410: (needs a stale cursor; skip if it can't be forced, note it).
- Account deletion: delete a test account; it can't sign in again, and
  the server has no rows for it (ask Claude to check with a query).
- Sync debug screen shows the outbox and last sync correctly.

## Minified release build
All of the above on a minified release build: no R8 crashes.

Report back what failed. Each failure becomes its own step.

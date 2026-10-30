# Phase 4 hardening: logout and sync scheduling

## Why
A review of 4affacc (sync triggers, logout flush, pull-to-refresh) found
concurrency and error-handling bugs in the Android sync engine that unit
tests and CI don't catch. The worst one can leak one account's data into
the next account on the same device.

## Scope
1. Logout wipes under the sync lock, before signing out.
2. Logout errors in Settings end in a closable state, never a stuck dialog.
3. SyncScheduler never drops a request that arrives as a run finishes.
4. Pull-to-refresh: right message when signed out, plus tests.
5. Manual device checks (me): pull-to-refresh, logout dialogs, minified
   release build.

Out of scope: Group K (favourites, rest timer), Phase 5 migrations.

## Invariants
- Sync rules in android/CLAUDE.md hold: server owns conflicts, push then
  pull never overlapping, server response overwrites local state.
- After logout, no row, outbox entry or cursor of the old user remains,
  whatever sync was running when logout started.
- No Room schema change expected. If one happens, bump the DB version.
- No api/openapi.yaml change.
- Each step builds and passes `./gradlew testDebugUnitTest` on its own.

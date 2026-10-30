# Phase 4 close-out: account deletion, debug screen, hosting

## Why
Phase 4's exit criteria (architecture plan §13.3) need a real backend
that a phone can reach: two devices converge, offline edits sync, a 410
gives a clean resync. Two pieces of Phase 4 never shipped either: account
deletion that reaches the server, and the sync debug screen. The
re-plan of 2026-10-29 (docs/roadmap-details.md, gaps 1, 3 and 4) found
all three.

## Scope
1. Account deletion calls `DELETE /v1/account`, then wipes under the
   sync lock.
2. Sync debug screen (debug builds).
3. Hosting config: Render (Docker) + Neon, nightly encrypted backup,
   restore script.
4. (me) Accounts, secrets, first release on the host, DNS, restore test.
5. Release build and spec point at `https://api.rukavina.app`.
6. (me, deferred) Device checks against the hosted backend.
7. (me) CI green, PR develop → main.

Out of scope: Crashlytics, Room migrations (Plan 2), Figma work
(Plan 3), any UI redesign, iOS.

## Invariants
- Sync rules in android/CLAUDE.md hold.
- After account deletion, no row, outbox entry, cursor or profile image
  of the old user remains on the device, and the server holds nothing
  for that uid.
- Nothing is deleted locally unless the server confirmed the delete
  (204), except when the server account is provably already gone.
- The backend stays a plain Docker image reached only via
  `DATABASE_URL` + `DB_PASSWORD` (architecture plan §12.3). No
  provider SDKs.
- No Room schema change. The only `api/openapi.yaml` change is the
  production server URL in step 05.
- Each step builds and passes its tests on its own.

# 02 Guest mode

## Problem
Splash sends anyone without a Firebase user to Login; the app can't be
used without an account. ADR 0014 wants a local-only guest whose data
merges into an account on sign-up or sign-in. Step 01 already opens
`gym_buddy_signed_out.db` when signed out and never syncs it
(SyncEngine checks `holder.owner == auth uid`), and OutboxRecorder
queues every write regardless of sign-in. Profile and Settings read
`FirebaseAuth.currentUser` directly and do nothing without a uid.

## Owner decision: A (2026-11-03)
- **A (recommended): copy, one path.** On sign-in/sign-up the guest's
  user rows and outbox entries are copied into the account's file in
  one transaction, then the guest file is deleted. Same code for a new
  and an existing account. Rows already in the account file win
  (server-wins, D-12).
- **B: rename for a new account, copy for an existing one.** Rename is
  instant, but two paths to test, and the rename must not race the
  holder's switch.

## Change
- Guest file: `gym_buddy_signed_out.db` becomes `gym_buddy_guest.db`
  (renamed on first start, side files first). Owner `null` = guest.
- Guest flag (DataStore): set by "Continue without account", cleared
  after a merge or erase. Splash: user → main; guest flag → main;
  else Login.
- Login: plain `TextButton` "Continue without account" (entry point
  only; real onboarding is Plan 7.4, Figma 114-20520).
- `GuestMerge` (data/sync): `pendingCounts()` (sessions, custom
  exercises, custom templates), `mergeInto(uid)` (copy rows, outbox,
  user states; guest profile row re-keyed from `"guest"` to uid unless
  the account has one; delete guest file), runs under the holder lock.
- Flow after a uid appears while the guest file has user data:
  - Sign-up: merge without asking.
  - Sign-in: switch, run the SIGNED_IN sync, then if the account file
    has sessions (or the pull failed) ask once: "Add N workouts from
    this phone to x@…?" Add → merge, sync, toast receipt. Cancel →
    sign out, back to the guest, nothing lost. Empty account → merge
    without asking.
  - Plain `AlertDialog` hosted in MainScreen, driven by a
    `GuestMergeCoordinator` StateFlow.
- Profile: guest uses uid `"guest"` (local only, never pushed: the
  outbox only pushes for an account, and merge re-keys it).
- Settings for a guest: hide sign out, password, delete account; show
  "Erase data on this phone" (confirm dialog, reuses
  `wipeEverythingForLogout` on the guest file, then back to Login).
- Gotchas from 01 in scope: gate screens on `holder.owner == uid`
  during a switch (guest → B leak), delay closing the old file until
  the new one's first emission, only delete the legacy file when
  Firebase is known to be up.

## Tests
- Guest writes land in the guest file and its outbox; nothing pushes
  (fake SyncApis sees no call).
- Sign-up merge: all guest rows and outbox entries in the account
  file, profile re-keyed, guest file gone, first sync pushes them.
- Sign-in, account has data: coordinator asks with the right counts;
  Add merges (account rows win on id clash); Cancel signs out, guest
  file intact, guest flag still set.
- Sign-in, empty account: merges without asking.
- Pull failed: asks.
- Account A with unsynced changes, then guest, then sign in as B:
  A's file untouched, only guest data asked about.
- Rename `signed_out` → `guest` migration.
- Erase as guest: guest file empty, flag cleared.

## Out of scope
Account offer card after workouts (Plan 7), "account deleted, keep as
guest" and session expired (03), real onboarding screens (7.4), export
(07), server changes, Room schema change (none needed).

## Done when
`./gradlew testDebugUnitTest` green; `./gradlew assembleDebug` builds.
Manual (me): fresh install → Continue without account → log a
workout → create account → workout synced. Second phone: guest
workout → sign in to that account → asked, Add → both workouts there.

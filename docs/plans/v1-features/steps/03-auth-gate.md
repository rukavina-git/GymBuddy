# 03 Auth gate

## Problem
The open Room file follows Firebase's uid (step 01: `observeSignIn`
switches on every uid change). Firebase signs the user out by itself
when the refresh token dies (revoked, password changed elsewhere,
account deleted or disabled). Today that switches to the guest file:
the account's data vanishes from the screens and new sets land in the
guest file. A sync 401 is only logged. ADR 0014 / roadmap 6.3: one
place tells expired, deleted and offline apart; no forced Login, no
wipe.

## Owner decisions: 1A, 2A (2026-11-03)
1. **Account deleted elsewhere → "Keep on this phone"**
   - **A (recommended): copy into the guest file.** Generalise
     GuestMerge to copy owner → owner. The account's rows go to the
     guest file with revision 0 and an outbox entry each (so a later
     account gets them all), then the account's file is deleted.
     Works whether or not the guest file has data.
   - **B: rename the account's file to the guest file.** Only possible
     when the guest file is empty; still needs the outbox/revision
     reset. Two paths.
2. **Logout** (step 01 deferred "logout without a wipe" to here)
   - **A (recommended): unchanged.** Flush, warn, wipe, as today. It's
     an explicit choice and the warning already exists.
   - **B: keep the file when it has unsynced changes**, no warning;
     they push when that account signs in again. Leaves data on a
     phone the user may be handing over.

## Change
- `DataOwner` (DataStore): the device-local owner (ADR 0014
  "dataOwnerUid"). The holder opens the data owner's file, not
  Firebase's uid. Set on sign-in; cleared only by a voluntary exit
  (logout, account deletion here, merge Cancel, erase), which calls
  `release()` before `signOut()`.
- `AuthGate` (data/sync), the one place:
  - Firebase uid == owner: `SignedIn`. Different uid: switch (step 01/02
    rules). Null and the owner released: `Guest`.
  - Null without a release, or a sync 401: probe with
    `getIdToken(true)` while a user is still there; classify by
    Firebase's error: network → offline (nothing changes), user not
    found / disabled → `AccountDeleted(owner)`, anything else →
    `SessionExpired(owner)`. If Firebase already dropped the user,
    the last probe result decides; none → `SessionExpired`.
  - `SessionExpired`: the owner's file stays open, logging works, the
    outbox waits (SyncEngine already skips: auth uid ≠ owner).
    Signing in again as the same uid → `SignedIn`, sync pushes. As
    another uid → that account's file; A's stays apart (step 01).
  - `AccountDeleted`: Keep → decision 1; Erase → delete the file,
    become guest.
- SyncScheduler reports a `SyncException` with 401 to the gate.
- Plain UI until Plan 7.4, wording from Figma 114-20520 frames 16
  and 20: banner on MainScreen "Backup paused. Sign in again to keep
  backing up." + Sign in; dialog "k…@gmail.com was deleted. Your 14
  workouts, 3 templates and 2 exercises are still on this phone."
  Keep on this phone / Erase… (Erase asks again with counts).
- Splash: owner set or guest flag → Main; else Login. Fixes the 02
  gotcha (flag left set after a no-data guest signs in): sign-in
  clears the flag.

## Tests
- Gate classification with a fake probe: network, not found,
  disabled, token expired, no user left.
- Involuntary sign-out keeps the owner's file open; a write lands
  there; nothing is pushed; same uid signs in → that write pushes
  (fake SyncApis).
- Voluntary logout releases first → guest file.
- Sign-in as B while A is expired: B's file, A's outbox intact.
- 401 from sync → `SessionExpired`.
- Keep as guest: rows in the guest file, revision 0, one outbox entry
  each, account file gone; with existing guest data too.
- Erase after deletion: file gone, guest.
- Splash routing (owner / guest flag / neither).

## Out of scope
Real screens (7.4), delete-account-in-app "keep as guest" choice
(7.x UI; the data path from decision 1 is reusable), server changes,
Room schema change (none needed), other step 02 reviewer items.

## Done when
`./gradlew testDebugUnitTest` green; `./gradlew assembleDebug` builds.
Manual (me): sign in on the phone, change the password on the web →
banner, log a set, sign in again → set syncs. Delete the account from
another device → dialog; Keep → data still there as guest.

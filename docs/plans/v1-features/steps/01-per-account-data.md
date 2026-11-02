# 01 Per-account local data

## Problem
There is one Room file (`gym_buddy_db`). Logout wipes it, so today
only one account's data ever exists on the phone. ADR 0014 wants data
to belong to one owner at a time (an account, later the guest) and
an account's unsynced changes to survive another account signing in.
Hilt provides `AppDatabase`, the DAOs and the repositories as
singletons (AppModule, SyncModule, the seeders, OutboxRecorder,
SyncLocalStore), so whatever is open at injection stays open.

## Owner decision: A (2026-11-03)
- **A (recommended): one Room file per owner.** `gym_buddy_<uid>.db`
  (guest file comes in step 02). A `DatabaseHolder` owns the open
  database and switches it when the signed-in uid changes; everything
  that injects `AppDatabase` or a DAO gets it through the holder.
  No Room schema change. Wiping an owner = deleting their file.
  Cost: DI refactor at ~12 injection sites, and flows that are already
  collecting must resubscribe on a switch.
- **B: an owner column on every user table.** One file, every query
  filters by owner. Room schema change (v28) on ~9 tables. Cost: one
  missed `WHERE` leaks data; the sync cursor and meta also need owner
  keys.

## Change
- `DatabaseHolder`: `current: StateFlow<AppDatabase>`, `switchTo(owner)`
  under the sync engine's lock (no cycle runs across a switch); the old
  file is closed only after the switch.
- First start after the update: the existing `gym_buddy_db` is renamed
  to the signed-in uid's file. Nobody signed in: it is deleted (it's
  empty after logout today).
- Repositories, SyncLocalStore, OutboxRecorder, seeders read the DAO
  from the holder per call, not at construction. Seeders run per new
  file.
- `FirebaseAuthSession.observeSignIn`: a new uid switches the holder
  before it requests the SIGNED_IN sync.
- Logout: unchanged in this step (flush, warn, wipe). Signing in as a
  different account without logging out (session expired, account
  deleted elsewhere) keeps the old file untouched. Logout without a
  wipe comes with step 03.
- Workers (`SyncWorker`, `PushOutboxWorker`) sync the current owner
  only; another owner's outbox waits until they sign in again.
- Auto Backup rules: include the Room files, exclude nothing else for
  now (ADR 0014 consequence).

## Tests
- Holder: switching uid opens another file; data written as uid A is
  not visible as uid B; back to A, it's there again (in-memory or
  temp-dir Room, Robolectric).
- Migration of the legacy file: signed in → renamed to the uid file;
  signed out → deleted.
- A's non-empty outbox stays after B signs in; B's sync never sends it
  (fake SyncApis).
- Switch waits for a running sync cycle (lock).
- Existing repository and sync tests still pass through the holder.

## Out of scope
Guest file and merge (02), session-expired banner and logout without
wipe (03), any UI change, server changes.

## Done when
`./gradlew testDebugUnitTest` green; `./gradlew assembleDebug` builds.
Manual (me): with A signed in, install the update over the old build:
A's data is still there. Logout still warns and wipes as before.

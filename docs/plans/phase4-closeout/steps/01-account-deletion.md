# 01 Account deletion reaches the backend

## Problem
`SettingsViewModel.performAccountDeletion` (ui/settings) clears Room with
`database.clearAllTables()`, deletes profile images, then calls
`FirebaseAuth.currentUser.delete()`. It never calls `DELETE /v1/account`.
- The server keeps every session, template, custom exercise, overlay
  and profile row for that uid, and once the Firebase user is gone
  nobody can delete them. GDPR Art. 17, Play and App Store blocker.
- The wipe runs outside the sync lock, so a running cycle can commit a
  pull page after it (same bug as hardening step 01).
- It wipes before anything has succeeded remotely, so a failed delete
  leaves an empty device with a live account.

The backend (`AccountDeletionService`) already deletes the data in one
transaction under the user's advisory lock, then deletes the Firebase
user. The client must not delete the Firebase user itself.

## Change
- Add `AccountApi` to `SyncApis` (generated from the spec,
  `operationId: deleteAccount`).
- New `AccountDeletionCoordinator` in data/sync, built like
  `LogoutCoordinator` (injected `runExclusive`, plain unit tests):
  1. Under `runExclusive`: call `deleteAccount()`. On 204: wipe local
     user data (`wipeEverythingForLogout`), delete profile images,
     `auth.signOut()`.
  2. Network error or 5xx: wipe nothing, return a retryable failure.
  3. 401 right after a successful re-auth means the token is stale:
     retryable failure that asks to sign in again.
- Unsynced outbox entries are not flushed first: the account is being
  deleted, so they're discarded. The confirmation dialog says so if the
  outbox isn't empty.
- `SettingsViewModel`: the existing re-auth paths (password, Google)
  call the coordinator instead of `performAccountDeletion`. Remove
  `performAccountDeletion` and the `user.delete()` call. Keep
  `accountDeletedEvent` for navigation.
- Deletion UI states follow the step 02 logout pattern of
  phase4-hardening: in progress, failed (message + retry + close),
  done. Never a stuck dialog.

## Tests
- Coordinator, over in-memory Room with a fake API:
  - 204: user data, outbox, cursor gone; reference data kept; signOut
    called; images deleted.
  - IOException / 500: nothing wiped, signed in, retryable failure.
  - A sync cycle queued behind the delete does nothing afterwards.
- SettingsViewModel (Robolectric, as in SettingsViewModelTest):
  failure ends in a closable state; success emits accountDeletedEvent.

## Out of scope
Re-auth UI changes, typed confirmation (keep what exists), Crashlytics,
backend changes.

## Done when
Tests above pass, existing LogoutCoordinatorTest, SettingsViewModelTest
and SyncTriggersTest still pass, `./gradlew testDebugUnitTest` green.

Manual (me, with step 06): delete a test account on a device, then
check the server has no rows for that uid and Firebase has no user.

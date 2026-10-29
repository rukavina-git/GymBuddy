# 02 Logout errors never leave a stuck dialog

## Problem
`SettingsViewModel.logout()` and `confirmLogoutAndDelete()`
(ui/settings/SettingsViewModel.kt) catch nothing. If `requestLogout()`
or `logOutAndDelete()` throws, the coroutine fails and `_logoutState`
stays `Flushing`. `SyncingBeforeLogoutDialog` has `onDismissRequest = {}`
and no buttons, so the user is stuck.

## Change
- Catch failures in both functions (rethrow CancellationException).
- New `LogoutUiState.Failed` with a short message, shown in a dialog the
  user can dismiss (back to Idle) or retry.
- `confirmLogoutAndDelete()` also guards against double taps, like
  `logout()` already does.

## Tests
Add SettingsViewModel tests (none exist yet) with fakes:
- empty outbox → LoggedOut path
- flush fails → UnsyncedWarning with counts
- requestLogout throws → Failed, then cancel → Idle
- logOutAndDelete throws → Failed

## Done when
Tests pass, no stuck state reachable, `./gradlew testDebugUnitTest` green.

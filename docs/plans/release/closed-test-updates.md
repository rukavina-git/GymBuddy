# Closed test updates (for the production access form)

Play's production form asks what changed during the closed test. One
entry per update, with facts we can back up from Crashlytics, git and
the device checks. Plan: 3-4 more after 0.2.1 (see progress.md cadence).

## 0.2.1 (2001), released 2026-11-19
- Crash found by testing: Crashlytics showed a high crash rate on
  0.1.0/0.2.0 (42 crashes, 6 users, several devices). Sync without
  internet crashed the app while refreshing the sign-in token.
- Fixed the cause: a failed token refresh offline now fails only that
  sync, which retries later; an expired session goes back to sign-in.
  Reproduced on a real phone (airplane mode) and verified the fix there.
  Unit tests cover offline, expired and deleted sessions.
- Workout start times now show in local time (were 1 h early).
- Set card fits on shorter screens (16:9).
- Backup reminder card hides once you create an account.
- To check after release: crash-free rate on 0.2.1 in Crashlytics.

## 0.2.2 (planned ~11-23)

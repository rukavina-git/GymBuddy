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

## 0.2.2 (2002), planned 2026-11-24
- In-app updates: the app asks Play for a newer version on every open.
  Priority per release (android/fastlane/update-priority.yml) decides:
  4-5 forced, 1-3 offered once, 0 left to Play. Lets us push urgent
  fixes to testers fast from now on. 0.2.2 itself ships with 0.
- Security pass (Ward / OWASP MASVS static check, 2026-11-23):
  - Play services security provider updated at startup (TLS fixes on
    older Android).
  - Password fields hide other apps' overlays (tapjacking), Android 12+.
- Sync failure-mode tests (offline, expired session) added.
- Known: the 4 old Gemini exercise images are removed (2026-11-25:
  images parked for v2, still frames don't show the movement); the new
  Qwen-Image set lands in the next update, so no exercise shows an
  image in 0.2.2.
- To check after release: crash-free rate on 0.2.2, and that 0.2.3
  shows the update prompt on a tester phone.

## 0.2.3 (2003), 2026-11-24
From the paid testers' report (docs/plans/release/tester-feedback-2026-11-24.md):
- Sign out now asks first, also when everything is backed up.
- Play rating prompt (In-App Review) after the 3rd finished workout,
  once more after the 10th, never mid-workout.
- Store listing rewritten (ASO council), pasted in Play Console.
- Update priority 4: first forced update, tested from 0.2.2 phones.
- To check after release: a 0.2.2 phone is forced to update; crash-free
  rate on 0.2.3.

## 0.2.4 (2004), 2026-11-26
- In-app feedback (You > Help > Send feedback): one text box, no email
  app. Works for guests and accounts; offline messages queue and send
  on reconnect. Owner gets a ping and reads/triages them on an admin
  page. Privacy policy and Play Data safety updated (optional "Other
  user-generated content").
- Start screen stays up while Play's update check runs (no flash of
  Home behind an update prompt).
- Backend code-quality pass (Sonar).
- Update priority 0 (nothing urgent).
- To check after release: feedback from a tester phone arrives on
  prod; crash-free rate on 0.2.4.

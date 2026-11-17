# 04a Signed-out dialog

Figma: 03 Locked designs › "Signed out · locked 2026-11-16"
(https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=271-7257),
frames R22-1 … R22-5. Council: docs/council/2026-11-16-signed-out-dialog.md
(+ owner override).

## Problem
Step 03's device check: after the account was deleted on the server,
the app showed only the slim "Backup paused" banner, and You still said
"Backed up · just now". The owner wants it impossible to miss.
Also a bug: Firebase's own sign-out reaches `AuthGate.onFirebaseUid(null)`
before `AuthSession.probe()` records DELETED (logcat 20:22:32.539 vs
.541), so a deleted account lands in SessionExpired, not AccountDeleted.

## Change (Claude)
1. **Race fix** (`AuthSession`, `AuthGate`): `lastFailure` becomes a
   StateFlow. In `onFirebaseUid(null)` with an owner and no failure yet,
   wait up to 2 s for one (`withTimeoutOrNull`) before picking the state.
   Plus a one-way SessionExpired → AccountDeleted when DELETED arrives
   later. AccountDeletedDialog (R16-16) is unchanged.
2. **Dialog** (R22-1/2/3) replaces `SessionExpiredBanner`, in MainScreen
   so it covers every tab, the running workout and the summary.
   "You're signed out" / "Your workouts stay on this phone." / Sign in
   (lime, full width) / Continue without account (text, full width).
   Not dismissable: no tap-outside, no Back. Taps ignored for 500 ms after
   it appears. Sign in opens the existing sign-in route; backing out of
   it brings the dialog back (no choice made yet).
3. **When it shows** (`SignedOutPrompt`, pure logic, unit tested):
   at once when the state becomes SessionExpired. After "Continue without
   account" it shows again on the next app open only if ≥ 48 h passed since
   the last Continue and Continue was tapped < 3 times. Then only markers.
   Stored in its own SharedPreferences file `signed_out_prompt`, excluded
   from Auto Backup and device transfer (reinstall resets it). Cleared on
   sign-in.
4. **Markers while signed out**: Home chip "Signed out" next to the
   wordmark (R22-4, tap = dialog, 48 dp touch target). You card (R22-5):
   cloud-off + "Signed out. Your workouts stay on this phone." and a lime
   "Sign in" button; never "Backed up". The Account group's "Sign out"
   row is hidden while signed out.
5. **Strings**: plain words; `expired_banner` removed.

Data stays the account's (no guest move). No openapi change. No Room
schema change. No new data collected (the counter stays on the phone).

## Tests
- AuthGateTest: Firebase null before DELETED → AccountDeleted (race);
  DELETED after SessionExpired → AccountDeleted; no failure in 2 s →
  SessionExpired.
- SignedOutPromptTest: first time shows; Continue → hidden; < 48 h
  hidden; ≥ 48 h shows; after 3 Continues never; sign-in resets.
- Compose test: dialog shows over the active workout, Back doesn't
  close it, both buttons work.
- Backup rules: `signed_out_prompt.xml` excluded in both xml files.
- Device: delete a test account with delete-account.sh mid-workout →
  dialog at once; Continue → Home chip + You card; deleted account case
  shows R16-16. 200% font: dialog buttons don't clip.

## Done when
On the phone, a deleted account shows the deleted dialog, an expired
login shows the blocking signed-out dialog (also mid-workout), the
re-show rule works, the markers match R22-4/5, and
`testQaDebugUnitTest` + `lintQaDebug` are green.

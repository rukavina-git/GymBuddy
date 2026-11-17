# Council 2026-11-16: Signed-out dialog

Owner (2026-11-16): recommendation accepted. Body "Your workouts stay on this
phone."; button "Sign in" (not "Sign in again"). Dialog at most once every 2 days,
at most 3 times in total (no spamming users); the marker stays. Counter is local
on the phone and resets on reinstall: keep it in its own prefs file, excluded from
Auto Backup (allowBackup is on, so normal prefs would come back after a reinstall).
Owner override (2026-11-16, after Figma R21): Q2 = a. The dialog (R21-D1, stacked full
width) shows at once over everything, including a running workout, and blocks the app
(no tap-outside, no Back dismiss) until Sign in or Continue without account. The
council's Q2 b (wait until Finish) is rejected.

```
COUNCIL BRIEF: Signed-out dialog (Gym Tracker, native Android, offline-first workout log)

Today: when the login dies (expired token, password changed, account deleted elsewhere) the app keeps the
account's data open on the phone and shows only a slim top banner: "Backup paused. Sign in again to keep
backing up." [Sign in]. The You card still shows name, email and "Backed up · just now". Owner tested it and
found it far too quiet: "It should be SUPER clear the user is not logged in. Normal users are used to logging
in again; no tech words like sync." Owner wants a dialog: "You're signed out", primary "Sign in", secondary "Continue without account"; nudge toward signing in. (Account deleted elsewhere
already has its own dialog "This account was deleted" [Keep on this phone] [Erase…], decided 2026-11-02.)

Q1 Wording of the dialog (title / max 1-line body / primary / secondary):
  a) "You're signed out" / "Sign in to keep your workouts safe." / Sign in / Not now
  b) "You're signed out" / "Your workouts stay on this phone." / Sign in again / Continue without account
  c) "Sign in again" / "Your account needs you to sign in again." / Sign in / Later
  d) Plain M3 standard: "Signed out" / none / Sign in / Cancel
Q2 When it shows:
  a) At once, everywhere, even over an active workout
  b) At once, except during an active workout: then after Finish (banner meanwhile)
  c) Only at next app open
Q3 After "Not now"/"Continue without account", and how often it returns:
  a) Same account data stays, clear "Signed out" marker on You card + Home; dialog again at every app open
  b) Same as a, but dialog again once per day max
  c) Same as a, dialog never again; the marker + a Sign in button on You are the only reminders
  d) Become a guest (data moves to guest; signing in later merges)
Q4 You card when signed out: a) "Signed out" + Sign in button in place of "Backed up"; b) hide the account
  card, show a guest-style "Sign in" card; c) keep as is (status line only)
Q5 Account deleted elsewhere: a) keep its own dialog (Keep on this phone / Erase…); b) same "You're signed out"
  dialog, deleted case adds one line and Erase as a third choice; c) same dialog, Erase only lives in You

Constraints: locked: no forced full-screen Login, no data wipe, offline-first (logging always works),
plain words, no "sync/backup/session/token".
Bug fixed regardless: deleted accounts briefly classified as expired (race). Out of scope: new sign-in methods.
Files: PRODUCT.md, docs/plans/figma-foundation/design-brief.md, docs/council/2026-11-02-onboarding.md,
android/app/src/main/java/com/rukavina/gymbuddy/data/sync/AuthGate.kt, .../ui/guest/AuthGateUi.kt,
.../ui/MainScreen.kt, android/app/src/main/res/values/strings_onboarding.xml (paths under repo root
/Users/karlo/Dev/GymBuddy).
```

## Members (verbatim)

### Design director (impeccable)
VOTE: Q1 b, Q2 b, Q3 b, Q4 a, Q5 a
WHY:
- Q1 b: Operate-surface copy states what happened and what's safe. "Your workouts stay on this phone" answers the first fear. (a) "keep your workouts safe" is fear framing (PRODUCT.md principle 3: no fake motivation). (d) "Cancel" cancels nothing; M3 wants specific verbs on dialog buttons.
- Q2 b: android.md: "Dialogs only for decisions that must interrupt." Craft-floor.md failure list: "A modal for a task that needs neither interruption…". Sign-in can wait for Finish; principle 1 (the set is sacred) decides it.
- Q3 b + Q4 a: Nielsen #1 (visibility of system status) via persistent Home/You marker; dialog capped to once a day satisfies the owner's "super clear" without breaking principle 4 (guide without nagging). 4a keeps identity, swaps only the status line: one card, zero new learning.
- Q5 a: deleted is a different decision (can't sign in). Three-button M3 dialog with destructive Erase adds load and mis-tap risk.
RISK of my pick: once-a-day can still feel naggy for someone who chose offline deliberately.
I'D VETO: Q2 a. Modal over an active workout breaks principle 1 and android.md's dialog rule.
IDEA: Stop auto-showing after 3 "Continue without account" taps; from then on only the marker remains (tapping it opens the same dialog).

### Product owner
VOTE: Q1 a (but secondary = owner's "Continue without account"), Q2 b, Q3 b, Q4 a, Q5 a
WHY:
- Q1: "Keep your workouts safe" gives a reason to sign in (the nudge the owner asked for); Principle 2 says "continue without account" is always offered, so that label stays. "Not now"/"Later" frame signing in as mandatory.
- Q2: Principle 1, the set is sacred: no dialog over an active set. Banner during workout, dialog after Finish.
- Q3: Principle 4, guide without nagging. Every open = nagging; never again = today's quietness. Once a day + a clear "Signed out" marker on You/Home hits both.
- Q4/Q5: 4a is a small copy/state change; 5a reuses the dialog locked 2026-11-02 and the case differs (can't sign in again). Both fit the v1 scope freeze.
RISK of my pick: once a day can still feel naggy for a deliberate offline user; a "Don't remind me" after several dismissals would help but adds scope.
I'D VETO: Q3 d. Moving data to guest then merging is new v1-freeze logic, risks silent data loss with the A/B owner rule, and reopens ADR 0014.
IDEA: "Sign in" goes straight to the same account (Credential Manager filtered / email prefilled): one tap, and it avoids signing into a different account by accident (which triggers the wipe path).

### Android architect
VOTE: Q1 b, Q2 b, Q3 b, Q4 a, Q5 a
WHY:
- No new data path: SessionExpired already keeps the owner's file open and the outbox waiting; the dialog is pure Compose over the existing gateState in MainScreen. Q2 b: MainScreen already has `activeWorkoutViewModel.hasActiveWorkout()`, so deferring until Finish is cheap.
- Q3 b: a DataStore "last shown" timestamp is ~10 lines. Q4 a: YouScreen swaps its status line from the existing state.
- Q5 a: AccountDeletedDialog + keepAsGuest/erase already exist and are tested; merging them is rework for no gain.
RISK of my pick: "Continue without account" suggests becoming a guest while data stays tied to the account; the You marker must make that clear.
I'D VETO: Q3 d. Moving an expired account's data to guest then merging back runs the account's own pending changes through GuestMerge (duplicates/conflicts) for a merely expired session.
RACE FIX: make `lastFailure` a StateFlow; in `onFirebaseUid(null)` with an owner and a null failure, wait up to ~2 s (`withTimeoutOrNull`) before choosing the state; also allow a one-way SessionExpired → AccountDeleted if DELETED arrives later. The wait also stops the signed-out dialog flashing before the deleted one.
IDEA: If the user signs in with a different account from the dialog, warn first ("Workouts from x@y stay with that account"), or that file and its pending changes are left behind silently.

### Accessibility and gym-floor ergonomics
VOTE: Q1 b, Q2 b, Q3 b, Q4 a, Q5 a
WHY:
- Q2b: a dialog mid-set can steal the tap meant for "done set" or the kg stepper, so a set gets logged wrong. Nothing is lost by waiting until after Finish, because logging works offline. Show it there, and keep a visible marker during the workout.
- Q1b: the body says the data is safe, so nobody panics or reaches for the wrong button. "Continue without account" says what it does, which matters for TalkBack users and anyone reading at 200% font. "Not now" and "Later" are vague. "Cancel" is misleading, since nothing gets cancelled.
- Q3b: popping up at every launch trains people to swipe it away blind, and once that habit forms they will also blind-dismiss the dialogs that matter. Once a day keeps it noticeable.
- Q4a: the marker and its button sit where the stale "Backed up" line is now. Text plus an icon, not colour alone, so it stays readable in harsh light.
RISK of my pick: if the workout runs long, the user stays signed out for an hour without the dialog. The in-workout marker has to be clearly visible, not a slim grey line (AA contrast, 48dp tap target).
I'D VETO: Q2a. A modal over an active workout costs speed and precision exactly when the user has the least of either.
IDEA: stack the dialog buttons vertically at full width, Sign in on top. That keeps every button at least 48dp and stops labels clipping at large font sizes, and TalkBack announces the title first. Run a test with system font at 200%.

### Beginner lifter
VOTE: Q1 b, Q2 b, Q3 b, Q4 a, Q5 a
WHY:
- My first thought on "signed out" is "did I just lose my 3 weeks?" Option b answers that in the body: "Your workouts stay on this phone." "Keep your workouts safe" (a) makes me think they're in danger right now.
- I'm usually staring at a video of the exercise mid-set. A popup then would make me panic and skip the set. After Finish is fine, because I'm already looking at the phone.
- Once a day is enough nagging. Every single app open would get me tapping "Continue" without reading it.
- "Account deleted" is scary and a different thing. Keep it as its own dialog so I don't mix it up with a normal sign-in.
RISK of my pick: "Continue without account" is long and sounds like I'm giving up my account for good. I might tap it and then wonder where my account went.
I'D VETO: Q2a. A popup over my active workout means I'd lose track of my set. Also Q3d: if my data quietly "moves to guest" I'd be totally confused about whose workouts I'm looking at.
IDEA: When the dialog shows after Finish, first confirm "Workout saved," then ask me to sign in. That way I know nothing was lost before you ask me to do something.

### Veteran lifter
VOTE: Q1 b, Q2 b, Q3 b, Q4 a, Q5 a
WHY:
- In the middle of a set I only care about one thing: my log still saves. "Your workouts stay on this phone" says that straight out. "Keep your workouts safe" in (a) makes me think they're at risk right now, and I'd stop to sort it out between sets.
- A dialog over an active workout is a dealbreaker. Strong once threw a popup at me mid-superset and I hated it. Banner during the session, dialog after Finish, when I'm resting anyway.
- I open the app 5 or 6 times a day: plans, checking last week's numbers, plate math. A dialog on every open is nagging. Once a day still nudges me without wrecking my flow.
- "Backed up · just now" while I'm signed out is a lie. I'd trust the app less. Show "Signed out" plus a Sign in button in that spot.
RISK of my pick: once a day might still be too easy to ignore, so someone could log offline for months, lose the phone, and lose PRs they assumed were saved.
I'D VETO: Q2 a. A modal over a live workout breaks the one promise that logging always works and is fast.
IDEA: After Finish, put the line in the summary: "12 workouts only on this phone. Sign in to keep them." Lifters care about their history, so a count of what's at risk persuades better than a vague warning.

## Synthesis (chair)

| Member | Q1 | Q2 | Q3 | Q4 | Q5 |
|---|---|---|---|---|---|
| Design (impeccable) | b | b | b | a | a |
| Product owner | a* | b | b | a | a |
| Architect | b | b | b | a | a |
| Accessibility | b | b | b | a | a |
| Beginner | b | b | b | a | a |
| Veteran | b | b | b | a | a |

\* a's body, with the owner's "Continue without account".

- Agree: 6/6 on Q2 b, Q3 b, Q4 a, Q5 a. Q1 5/6 for b; the split is only the body
  line ("safe" nudge vs reassurance). Four members read "keep your workouts safe" as fear.
- Vetoes: Q2 a (4 members: no modal over a live workout); Q3 d (3: no move to guest).
- Ideas worth keeping: buttons stacked full width, Sign in on top (a11y); Sign in goes
  straight to the same account (PO); "Workout saved" first when shown after Finish
  (beginner); stop auto-showing after 3 dismissals, marker stays (design).
  Already built: the different-account warning (architect's idea) exists as the
  2026-11-02 "other account" dialog.
- Race fix (architect): `lastFailure` as StateFlow, wait ≤2 s in onFirebaseUid(null),
  one-way SessionExpired → AccountDeleted.

Recommendation:
- Dialog "You're signed out" / "Your workouts stay on this phone." / [Sign in] (filled,
  top) / [Continue without account] (text, below), stacked full width.
- During a workout: no dialog; a clear "Signed out · Sign in" marker instead of the slim
  banner. Dialog after Finish (after the saved confirmation).
- Afterwards: "Signed out" + Sign in on the You card (no "Backed up") and a marker on
  Home; dialog at most once a day.
- Account deleted keeps its own dialog; race fixed.

Owner decides: Q1 body (reassure vs "safe" nudge); stop after 3 dismissals or keep daily forever; Sign in label ("Sign in" vs "Sign in again").

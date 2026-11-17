# 03 Prod release-build pass (10.1)

No Figma frame: no UI change.

## Problem
Nobody has walked the R8-minified prod build from Play end to end.
Testers land on it soon (enrollment runs early, see 00-overview).

## Change
(Claude, USB phone, Play-installed 0.2.0 / 2000, prod backend)
1. Baseline: owner's signed-in data (templates, workouts) noted.
2. Sign out of the owner's account. Data leaves the phone.
3. Guest: start as guest, log a short workout, history shows it.
4. Email sign-up with a throwaway account: guest data merge dialog,
   workout syncs (no "waiting to back up").
5. Sign out, sign back in to the throwaway: workout comes back.
6. Delete the throwaway account in the app: lands on Welcome, sign-in
   with it fails.
7. Owner's account back (Google sign-in): baseline data returns.
8. Crash check: `adb logcat` for FATAL / R8 errors (missing classes,
   serialization) through the whole pass.

Findings go to progress.md; fixes become refinement steps (10.1a).
No code change in this step.

## Done when
- Every item above passed or has a finding logged.
- Owner's account and data are back on the phone.

# 03 Active workout failure paths + process death

## Problem
`ActiveWorkoutViewModelTest` already covers a failed save, an empty
workout and unit conversion. Not covered: what happens to an
in-progress workout when Android kills the process (backgrounded at
the gym for 40 min is normal). The ViewModel holds the session in
memory only: no SavedStateHandle, no draft in Room. Today the whole
workout is lost.

## Owner decision (before "go")
- **A. SavedStateHandle.** Small, no schema change. Survives process
  death, not a force-stop, swipe-away or reboot. ~1 MB Bundle limit is
  fine for one workout.
- **B. Draft in Room.** Survives everything; the redesign's
  resume-on-open fits it. Room schema change (v27 + Migration + test),
  local-only table, never synced.
- **C. Tests only now**, persistence in 7.6.
Recommendation: B. A lost workout is the worst bug a gym app can have,
and 7.6 would need it anyway.

## Change
- Audit the 5 ViewModel test files; list gaps in progress.md.
- Add tests for: save rejected by the repository (exception, not just
  a false result), double-tap save, set edits after a failed save.
- Implement A or B per the decision, with a resume test (A:
  SavedStateHandle round-trip; B: draft written on each change,
  restored by a fresh ViewModel, deleted on save/discard).

## Out of scope
UI changes (resume prompt is 7.6). Rest timer (v2).

## Done when
`./gradlew testDebugUnitTest` green; for B also the migration test.
Manual: start a workout, enable "Don't keep activities", leave the
app, come back: the workout is still there.

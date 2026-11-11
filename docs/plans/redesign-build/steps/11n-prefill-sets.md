# 11n Feedback: F18 prefill sets from last session or template

## Problem
F18: every set starts at 0 kg, even for a lift done last week. Owner:
start from the last session's numbers; else the template's target.

Today:
- From a template, sets take `plannedReps` / `plannedWeightKg` /
  duration / distance (`startWorkoutFromTemplate`). Most templates have
  no weight, so 0 kg.
- Added exercises (`addExercises`) get 3 × 10 reps, no weight.
- `addSet` already copies the previous set; keep that.

## Change
1. Domain, pure (`domain/usecase/workout/LastPerformance.kt`):
   `lastSets(sessions, exerciseId): List<WorkoutSet>?` = the done sets
   of that exercise in the most recent finished, not-deleted session
   that has any (sessions are newest first). Sets with no values are
   skipped. Superset/order don't matter; first matching
   PerformedExercise in that session wins.
2. Prefill rule, pure (`prefillSets(planned, last, fields)`):
   - Last session exists → its values set by set (reps, weight,
     duration, distance, only fields the tracking type has). The set
     count stays the planned count (template or default 3); extra
     planned sets copy the last session's final set.
   - No last session → today's behaviour (template targets, else 3 × 10).
   - Weight stays in kg in the domain; the VM formats with
     `displayWeight(kg, units)` as today.
3. VM: `startWorkoutFromTemplate` and `addExercises` load sessions
   once (`getAllWorkoutSessionsUseCase().first()`, errors → empty) and
   apply the rule. No new DAO query; the session list is already in
   memory elsewhere and small. A dedicated query is a later optimization.
4. Values are prefilled, not done: sets stay un-done, Done works at once.

No api/openapi.yaml, Room schema, sync or backend change. Draft JSON
unchanged.

## Note on scope
The overview lists "last-time numbers" under out of scope (history
features). F18 is an owner device-feedback request; this step only
prefills values, it shows no "last time" label or history UI.

## Figma
No frame; no new UI, only starting values. Ask owner to waive (like
11f–11m).

## Out of scope
- Showing "last time: 80 × 8" or any history label.
- Progression suggestions (+2.5 kg).
- Template editor changes or writing last numbers back to templates.
- Prefill in Edit workout (09b) `addSet`.

## Done when
- Unit test: lastSets (newest session wins, deleted/unfinished ignored,
  undone/empty sets skipped, exercise not found → null); prefillSets
  (last beats template, more planned than last copies final set, fewer
  planned truncates, fields filtered by tracking type, no last → template).
- Unit test (VM): template start with a past session prefills weight;
  added exercise with a past session prefills; no history → old values.
- `./gradlew testDebugUnitTest` and `assembleDebug` green.
- Phone via adb: start a template with a lift done before, see last
  weights; add that exercise ad hoc, same.
- F18 ticked in device-feedback.md with the commit hash; progress.md
  line points to this file.

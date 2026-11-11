# 09 Session detail

## Problem
Every session row (Progress Recent, All sessions, exercise page history)
opens the old `WorkoutSessionFormDialog` through `SessionEditBridge`
(08 bridge, state `editingSession` in MainScreen). That dialog, its
`WorkoutSessionViewModel` (440 lines), `WorkoutSessionDraft`,
`WorkoutSessionUiState` and the rest of `ui/workout/WorkoutScreen.kt`
(816 lines) are v1 UI. Roadmap 7.9.
No Room schema or spec change expected: `WorkoutSessionRepository` has
update + soft delete; undo needs a restore call like `restoreExercise`
(07), a DAO query only.

Figma (03 Locked designs):
- Session detail https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=107-15763
  (R14-1 detail 107:15911, R14-2 ⋯ menu 107:16730, R14-3 from history
  107:16817, R14-4 edit 107:16270, R14-5 deleted 107:16414, R14-6 a run
  107:16621; notes 107:16726).

## Owner decisions (proposed, need "go")
1. **Split**: 09a detail + menu + delete/undo + wiring + deletes;
   09b Edit workout screen. Recommended: split. Until 09b, ⋯ "Edit
   workout" is hidden (old dialog is deleted in 09a; nothing else edits
   past sessions for a step).
2. **Undo delete**: Recommended: delete at once (tombstone + outbox
   DELETE), Undo = `restoreWorkoutSession(id)` clearing `deletedAt` and
   recording an UPSERT, like `restoreExercise`.
3. **Tiles** (R14-1/6): Recommended: weight sessions → time · sets ·
   lifted (same as `WorkoutSummaryScreen`); no weighted sets and any
   distance → time · distance · pace per km (or mi); else time · sets.
4. **Exercise row line**: "N sets · top 82.5 kg × 8" = heaviest set
   (Progress rule); no weight → best by the type's `SetOrder`
   ("1 set · 5.2 km · 28:14", "2 sets · 1:00"). PR pill = Progress PR
   rule (reuse 08's function).
5. **"Start Push A again"**: only when `templateId` points to a live
   template; starts it like Home (`startWorkoutFromTemplate`); if a
   workout is running, same guard as Home.

## Change (09a): Session detail
- Route `session/{id}?exerciseId=`; `ui/progress/session/SessionDetailScreen`
  + VM. Top bar back + ⋯; large title; "Mon 26 Oct · 18:24–19:16";
  tiles (decision 3); note line; one collapsible card per performed
  exercise (snapshotted name, line from decision 4, PR pill, chevron).
  Open card: numbered sets "80 kg × 8" and "Bench Press page ›" →
  exercise page.
- `exerciseId` arg (R14-3): that card starts open and gets a cobalt
  outline for ~1.5 s, scrolled into view.
- ⋯ menu (R14-2): Start again (decision 5), Delete (red). Delete →
  pop back, snackbar "Push A deleted" + Undo (R14-5), no confirm.
- Missing/deleted session → pop back (fixes 08 gotcha on the bridge).
- MainScreen `openSession` navigates to the route; exercise page rows
  pass their exerciseId. Delete `SessionEditBridge`,
  `WorkoutSessionFormDialog`, `WorkoutSessionViewModel`,
  `WorkoutSessionDraft`, `WorkoutSessionUiState`, WorkoutScreen.kt;
  fix the KDoc mention in `SyncTriggers.kt`.
- Repository: `restoreWorkoutSession(id)` + DAO query.

## Change (09b): Edit workout
- Route `session/{id}/edit`; `ui/progress/session/SessionEditScreen` +
  VM (R14-4): ✕ + "Edit workout" + Save; name field; date + start → end
  row (date/time pickers; duration = end − start); note row; same
  exercise cards, open card shows set chips that open the workout
  `Keypad`; swipe a set to delete (Undo); "+ Add set". Save validates
  through `ValidateWorkoutSessionSetsUseCase` then `updateWorkoutSession`.
- Discard on ✕ with changes: same discard dialog as the template editor.
- ⋯ "Edit workout" shown again.

## Tests
- Unit: tiles per data shape, exercise line + PR, start-again
  visibility, delete + restore (repo, outbox op), highlight arg.
- Unit (09b): edit VM keypad commit, add/delete set + undo, time change
  → duration, save calls update, discard detection.
- Compose UI: card expand shows sets; menu hides Start again without a
  template.

## Out of scope
Adding/removing/reordering exercises in Edit, set types (v2), calendar
entry point (Home week strip changes), sharing.

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

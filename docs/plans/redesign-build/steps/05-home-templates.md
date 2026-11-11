# 05 Home + Templates

## Problem
Home (`HomeScreen.kt`, 498 lines) is v1: quote of the day, no week
strip, no recent templates. The Workouts tab is `CombinedWorkoutsScreen`
(Templates | Sessions top tabs) around `WorkoutTemplateScreen` (885
lines, list + editor + `ExercisePickerDialog` + `ExerciseConfigDialog`).
`WorkoutsScreen.kt` is an unused placeholder. Roadmap 7.5.
No data, sync, Room or spec change expected.

Figma (03 Locked designs):
- Home https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=87-11340
  (R11-1 Home 87:11343, R11-2 new user 87:11422, R11-3 Start sheet
  87:11501, notes 87:12486). R11-4…R11-7 (All sessions, calendar) are
  7.8; R11-8 (runs) needs Plan 4 library data, out.
- App frame https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=85-9890
  (Workouts 85:10177, scrolled 85:10257, starter plans off 85:10336,
  Home new user 85:10079, Home while a workout runs 85:10122).
- Templates https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=92-12488
  (Page A 92:12491, Editor A 93:12841, Picker A 93:13183, Starter page
  93:13464, New template 93:13588, 12 exercises 93:13736, Keypad on reps
  93:13879, Discard 93:14065, Picker search 93:14148, Page 200%
  93:14244, After Add 3 93:14347, notes 93:14345, flags 93:14346).

## Split (like 02a/02b)
- **05a Home + Workouts tab + template page**: read-only and start.
- **05b Template editor + exercise picker**: editing, keypad, reorder.
Each leaves the app usable: 05a keeps the old editor reachable from
the pencil / "New template" until 05b replaces it.

## Owner decisions: all as recommended (2026-11-03)
1. **Split** 05a/05b? (recommended: yes)
2. **Sessions tab** of `CombinedWorkoutsScreen` (old `WorkoutScreen`)
   must stay until 7.8/7.9. Recommended: move it under the Progress
   route (replacing the `StatisticsScreen` placeholder) until 7.8.
3. **Week strip tap** should open Progress › All sessions › Calendar
   (7.8, not built). Recommended: switch to the Progress tab for now.
4. **"about 45 min"**: Figma flag says your own average for that
   template, hidden until there's data. Recommended: average of the
   template's finished sessions (endedAt − startedAt), hidden with
   none. Computed in a domain use case, no stored field.
5. **`nextInPlan` / derived templates** (open gotcha): Home notes say
   no prediction, no Next up in v1. Recommended: 7.5 doesn't use
   `nextInPlan`; the gotcha moves to the weekly-schedule item (v2).
6. **Drag reorder (05b)**: the old editor has own code. Recommended:
   own code (long-press handle + TalkBack "Move up/down"), no library.
7. **Missing prefs**: there is no "Hide starter plans" pref and no
   default rest setting today. Recommended: Starter plans always
   shown in 7.5 (toggle + pref in 7.10); new exercises get rest 2:00
   from a constant that 7.10 swaps for the setting.
8. **Typo-forgiving picker search (05b)**: same search as Exercises
   (app frame "Search forgives typos"). Recommended: small own matcher
   in domain/ (substring, then edit distance ≤ 1 per word), reused by
   7.7.

## Change (05a)
- `ui/home/HomeScreen` + `HomeViewModel`: wordmark top bar, week strip
  card (done days this week, "N workouts · h m", chevron), lime "Start
  workout" → Start sheet, "Your templates" = 3 most recent by last
  session (play icon starts it right away), "All templates" link →
  Workouts tab. New user with no sessions: the active starter plan's
  templates fill the 3 rows (R11-2). Workout running: button and rows
  hide (85:10122). Line: "N exercises · last done" (relative).
- Start sheet (R11-3): your templates by recency (right: last done),
  then Starter plans (one-liner: plan name, "day x of n", "about N
  min" if known; right: "N ex"), then "+ Empty workout".
- `ui/workouts/WorkoutsScreen` (tab root, large title, + search ⋯):
  "Your templates" (name, "N exercises · about N min", last done or
  "Never done"), "+ New template", "Starter plans" section (85:10336,
  starter plans off, comes with the pref in 7.10).
- `ui/template/TemplatePage` (Page A): back, pencil, ⋯; title, "N
  exercises · N sets · last done", numbered rows (sets × reps · kg,
  muted "rest m:ss" right, note line), lime "Start workout" pinned
  (hidden while a workout runs). ⋯: Duplicate, Delete (snackbar Undo).
  Starter (R12-8): "Starter plan · N exercises"; ⋯ Make a copy, Hide
  this plan; pencil = make "<name> (copy)" and open the editor.
- Delete `ui/HomeScreen.kt`, `HomeViewModel.kt` (quote of the day goes;
  its pref stays until 7.10), `CombinedWorkoutsScreen.kt`, old
  `WorkoutsScreen.kt`, the list part of `WorkoutTemplateScreen`.

## Change (05b)
- `ui/template/editor/TemplateEditorScreen` + VM (Editor A, R12-9/10):
  × and lime Save, Name field first ("Give it a name" when empty), one
  card per exercise: name, drag handle, chips "N sets / N reps / N kg"
  (or time/distance by tracking type) that open the workout `Keypad`
  sheet with Next to the following chip (R12-11), timer + rest value
  (tap → keypad), "+ Add note" / note text. Swipe to remove + Undo.
  Outline "+ Add exercise". Back/× with changes → discard dialog
  (R12-12). New exercises: 3 × 10, kg empty, rest 2:00 (decision 7).
  Running workout is not changed by edits.
- `ui/template/picker/ExercisePicker` (Picker A, R12-13/15): search,
  muscle chips, multi-pick numbered in order (cobalt), "In template"
  tag still pickable, picks survive searches, lime "Add N" appends in
  pick order, back drops picks. Shared with 7.6 (add exercise).
- Fixes the 02b gotcha: Keypad parser accepts "." and ","; decide the
  titleless sheet variant here (the editor needs it first).
- Delete `WorkoutTemplateScreen.kt`, `ExercisePickerDialog.kt`,
  `ExerciseConfigDialog.kt`, `TemplateExerciseListItem.kt`,
  `WorkoutTemplateDraft/UiState` if unused.

## Tests
- 05a unit: HomeViewModel (3 most recent by last session, new-user
  fallback to active plan, week strip days + total time, rows hidden
  while running), Start sheet ordering, average-duration use case,
  TemplatePage VM (delete + undo, starter copy naming, hide plan).
- 05a Compose UI: row play tap starts that template; Start hides when
  a workout runs.
- 05b unit: editor VM (add in pick order, remove + undo, reorder,
  empty name blocks save, discard only with changes, keypad Next
  order), search matcher (typo, accents, empty), keypad parser.
- 05b Compose UI: swipe remove + Undo, Move up/down actions, picker
  numbering.
- Existing template/workout tests move with the code or stay green.

## Out of scope
All sessions list/calendar (7.8), runs (Plan 4 data), active workout
screen (7.6), exercise page (7.7), Settings "Hide starter plans" toggle
UI and Hidden templates screen (7.10), weekly schedule (v2).

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

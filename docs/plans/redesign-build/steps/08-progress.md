# 08 Progress tab

## Problem
The Progress tab shows the old `WorkoutScreen` (1056 lines: session list
with sort menu, FAB + `WorkoutSessionFormDialog` for manual sessions,
edit/delete dialog) under a large title (05a). `ui/SessionsScreen.kt`
(19-line placeholder) is unused. Roadmap 7.8. The exercise page (07)
waits for its Progress block (07 gotcha).
No Room schema or spec change expected: sessions, sets and
`ExerciseHistory` already hold everything; the chosen lift is a
DataStore preference.

Figma (03 Locked designs):
- Progress tab https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=62-5195
  (R7-1 Progress 62:5199, R7-2 not enough data 62:5275, R7-3 empty
  62:5316, R7-4 change lift sheet 62:5346, R7-5 exercise progress
  62:5400, R7-6 drag to read 62:5487, R7-7 All sessions 62:5579, R7-8
  filter by exercise 62:5660, R7-9 no results 62:5731, R7-10 font 200%
  62:5766; notes 62:5801, 62:5802).

## Owner decisions (2026-11-06): 2–6 as recommended
1. **Split**: no, one step (owner).
2. **Session rows before 7.9**: the old `WorkoutScreen` is the only way
   to edit or delete a past session. Recommended: rows open the old
   `WorkoutSessionFormDialog` (moved out of WorkoutScreen) as a bridge
   until 7.9 replaces it; WorkoutScreen's list, sort menu and FAB are
   deleted. Manual "log a past session" goes away (not in Figma).
3. **Which lifts can be charted**: Recommended: tracking types with
   weight only (WEIGHT_REPS, WEIGHT_DURATION, WEIGHT_DISTANCE), sets
   with a weight count, "heaviest set" per session. Reps only / Time / Distance never get a chart in v1.
4. **Default lift**: Recommended: the most-logged eligible lift
   (3+ logs over 2+ weeks, note 2); stored choice in DataStore
   `progress_lift_id`; falls back to the default when that lift is gone,
   hidden or no longer eligible. "Show on Progress" off = default again.
5. **Chart window + sentence**: Recommended: last 8 weeks (all sessions
   if fewer weeks), sentence "You lift X kg more than N weeks ago." =
   latest point minus first point in the window; "the same as" / "less
   than" variants; weeks rounded down, min 2.
6. **"This week"**: Monday first (like 05a), finished sessions only;
   "N sessions · h m · kg" where kg = sum of weight × reps of done sets
   in user units. Recent = last 5 sessions.

## Change: Progress tab
- `ui/progress/ProgressScreen` + VM (R7-1/2/3/10): large title, "This
  week" counts line, chart card (lift name, chevron, "Change lift" chip,
  sentence, sparkline, "Heaviest set each session · a → b kg"), Recent
  (5 `SessionCard`s: title, PR tag, "64 min · Squat 110 kg × 5", date),
  "All sessions" secondary button. No card without an eligible lift
  (R7-2). Empty (R7-3): trending-up icon, copy, "Start a workout"
  (opens the start sheet). At font ≥ 2.0 the sparkline drops and
  "Change lift" becomes a button (R7-10).
- Card tap → exercise page scrolled to its Progress section; chip →
  Change lift sheet (R7-4): eligible lifts with "N sessions", radio,
  selected cobalt.
- Session subtitle: duration · best set of the session's top lift
  (heaviest weight; else first exercise). PR = a set beat that
  exercise's best from all earlier sessions.
- `ui/design/LiftChart` (R7-5/6): line + dots, 3 y ticks, 3 x dates;
  press-and-drag snaps to the nearest session and shows "date · value"
  above; nothing opens. One TalkBack stop with a summary sentence
  (DESIGN.md). `stroke.*` tokens.
- Exercise page (R7-5): Progress block after the note: sentence, chart,
  "Show on Progress" switch (cobalt), rows "Mon 26 Oct · 87.5 kg × 5"
  with "Latest" tag; rows open the session (decision 2). Only shown for
  eligible lifts. Replaces the 3-row History for those lifts.
- Delete `ui/SessionsScreen.kt` and WorkoutScreen's list/sort/FAB
  (decision 2); `WorkoutSessionViewModel` keeps only what the dialog
  needs.

## Change: All sessions
- `ui/progress/sessions/AllSessionsScreen` + VM (R7-7/8/9): back, search
  (workout names, `TypoMatcher`), "Exercise" chip → exercise picker
  (single pick) → cobalt chip with check + ✕; month headers "October
  2026 · 13 sessions · 41,200 kg" (filtered: "4 sessions"); filtered
  rows show that lift's sets inline "Bench press 87.5 × 5 · … kg".
  No results (R7-9): copy + "Clear search".
- Route `all_sessions?exerciseId=`; exercise page "All N sessions" link
  opens it filtered (07 gotcha).

## Tests
- Unit: week counts (Monday start, unfinished left out, units), eligible
  lifts (3+ logs, 2+ weeks, weight types), default + stored lift
  fallback, sentence variants, chart points, PR flag, session subtitle.
- Unit: All sessions search, exercise filter, month grouping + totals,
  inline sets text.
- Unit: chart drag snapping (pure function x → nearest index).
- Compose UI: empty state; card hidden without an eligible lift.

## Out of scope
Session detail screen (7.9; rows use the old dialog until then), sort
options, manual past-session logging, charts for non-weight lifts,
export (7.10).

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

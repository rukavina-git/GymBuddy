# 06 Active workout

## Problem
The workout is v1: `ActiveWorkoutScreen.kt` (558 lines) with text
fields, `SetTrackingFields.kt`, `components/NumberPicker.kt` wheels and
the active part of `WorkoutScreen.kt` (1056 lines). No ⌄, no keypad, no
swipe, no rest timer. Leaving is system Back. Roadmap 7.6.
No spec or Room schema change expected: the draft is `payloadJson` in
`ActiveWorkoutDraftEntity`, so new draft fields (rest start, current
set) are JSON only, with defaults for old drafts.

Figma (03 Locked designs) https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=67-6561
- Logging: Main screen 53:2417, Resting 53:2504, Rest passed 53:2595,
  Keypad with hint 53:2686, Limits 53:2796, Keypad rules 53:2873,
  Last set / timed 53:2983.
- Around: Delete a set 53:3058, ⋯ menu + Undo 53:3154, All exercises
  53:3272, Left the workout 53:3414, Lock screen 53:3470.
- Finishing: Finish 53:3493, Finish with sets left 53:3594, Summary
  53:3697.
- Motion: R5-A1…A4 53:3741–53:3840 (set lands), map 53:3890.

## Split (like 05a/05b)
- **06a Logging**: screen, set card, keypad, steppers, Done, rest,
  swipe delete + Undo, ⌄ to live bar, finish + summary. Replaces the
  old screen; app usable.
- **06b Around the workout**: ⋯ menu (Add exercise via 05b picker,
  Reorder, Workout note, Rest timer), All exercises sheet + Add set,
  "+ Empty workout" on the Start sheet (05a deviation), Home "Workout
  in progress" card, live bar rest variant.
- **06c Rest buzz + lock screen** (split from 06b, 2026-11-03): rest buzz
  (VIBRATE + channel), ongoing lock screen notification with Open only
  (foreground service, POST_NOTIFICATIONS). Step section written when
  06c starts.

## Owner decisions: all as recommended (2026-11-03)
1. **Split** 06a/06b? (yes)
2. **Lock screen** (53:3470, Done/Open from the notification) needs a
   foreground service + POST_NOTIFICATIONS. (06b: ongoing notification
   with Open only; Done from the lock screen later, it can lose a set
   if the app is dead.)
3. **Done + couldn't save** two pulses? (yes, CONFIRM then a new
   `Haptic.SaveFailed`, LONG_PRESS fallback below API 30: 01 gotcha.)
4. **Steppers on 360dp** (02b gotcha). (Stack value panels vertically
   below 380dp width; per-panel descriptions "Decrease weight".)
5. **First-workouts hint** "Tap a number to type": (shown in the user's
   first 3 workouts, counted from finished sessions; no motion.)
6. **Finish with sets left**: undone sets are dropped (as today). Plan
   stays unchanged; no "update template" offer (v2).
7. **Summary backup card**: guests only, "Create account" →
   `NavRoutes.signIn(SignInMode.CREATE)` (04 gotcha), "Not now" hides
   it for this summary only.
8. **Reorder exercises (06b)**: same own code as the 05b editor.

## Change (06a)
- `ui/workout/active/WorkoutScreen` + reworked `ActiveWorkoutViewModel`:
  top bar ⌄, title, clock, "3 / 15" exercise count, ⋯ (06a: Finish,
  Discard only). Exercise tabs row (check on done ones), shared axis X
  between exercises. Exercise header "equipment · sets × reps · rest",
  "How to" (opens exercise page from 7.7; 06a: hidden).
- Set list: done sets as rows "80 kg × 8"; current set as `SetCard`
  with kg/reps (or time/distance) `ValuePanel`s, − / + steppers (± 2.5
  kg / ± 1 rep / ± 5 s, odometer digit, hold repeats); tap a value opens
  the titleless `Keypad` sheet ("Set 3 · weight", Cancel, Next: reps /
  Save). Limits + rules: at least 1 rep, kg ≥ 0 (53:2873 message).
- Lime "Done · set N" (72dp, `Button/Primary large`); last set says
  "Done · finish workout". Set lands per R5-A1…A4 (one node swapping
  content, not `animateBounds`), CONFIRM haptic.
- Rest line under the last done set: counts "1:24 rest", passed shows
  "2:00 rest · +0:45" (no motion). Rest from the exercise's restSeconds.
  Rest start stored in the draft (survives process death).
- "Up next" row with the next exercise.
- Swipe left on a done set (or long-press → Delete): no confirm, gap
  closes 250 ms, "Set deleted · Undo" snackbar.
- ⌄ minimises into the live bar (container transform, motion.md #10,
  replaces the 03 stand-in); tap bar reverses. Home "Workout in
  progress" card (53:3414) if 05a's running state doesn't already show it.
- Finish dialog (53:3493 / 53:3594, sets-left line), Discard red with
  confirm, then Summary screen (53:3697: time, sets, kg lifted, top set
  per exercise, "+ N more", guest backup card, lime Done).
- `workoutRunning` vs `hasActiveWorkout()` (05a gotcha): one source.
- Delete `ActiveWorkoutScreen.kt`, `SetTrackingFields.kt`,
  `components/NumberPicker.kt`, the active part of `WorkoutScreen.kt`
  (Sessions part stays until 7.8).

## Change (06c)
Figma: Lock screen 53:3470 (incl. ring 53:3478). motion.md "Rest buzz",
DESIGN.md open questions (VIBRATE, channel, raw 80/120/80 ms).
- `WorkoutService` (foreground, `specialUse` type with a
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` "workout timer"): runs while
  `ActiveWorkoutUiState.running`, stops on finish/discard. Ongoing
  notification per 53:3470: title, "Set 3 of 4 done · rest 1:12"
  (`liveBarRest()` text, countdown via `setUsesChronometer` +
  `setChronometerCountDown`, no per-second posts), else the workout
  clock; one action "Open" (content intent too). Channel "Workout"
  (low importance, silent).
- Rest buzz: opt-in, off by default. `AppPreferencesRepository.restBuzz`
  (DataStore) + a switch "Buzz when rest ends" in the 06b Rest timer
  sheet (Settings row comes in 7.10). `RestBuzzScheduler` interface:
  VM calls `schedule(leftSeconds)` / `cancel()` on rest start, rest
  timer change, undo, next set, finish/discard, pause. Real impl in the
  service: one-shot coroutine, then `Vibrator` waveform 80·120·80 with
  `USAGE_NOTIFICATION` attributes (silent/DND = no buzz). `VIBRATE`
  permission. Raw ms constants named, DESIGN.md §4 row added.
- POST_NOTIFICATIONS (33+): asked once on the first workout start; no
  = no notification, workout and in-app buzz still work (service still
  runs; Android hides its notification).
- Manifest: service, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_SPECIAL_USE`, `POST_NOTIFICATIONS`, `VIBRATE`.
- No Done from the lock screen (decision 2). No Room/spec change;
  `restBuzz` is DataStore only.
- Flag: `specialUse` needs a Play Console justification at release
  (5.x). Alternative `health` type needs sensor permissions; not used.

## Tests
- 06a unit: VM Done advances set/exercise, rest start/elapsed/passed
  (fake clock), stepper bounds and steps per unit, keypad rules (0 reps
  rejected), delete + undo, finish drops undone sets, summary totals
  and top set, draft round-trip with new fields + old draft without
  them, haptics on save failure.
- 06a Compose UI: keypad Next order, stepper hold, swipe delete + Undo,
  Done lands the set.
- 06b unit: add exercise from picker, add set, reorder, note, rest
  override. Compose UI: Move up/down.
- 06c unit: buzz scheduling with a fake `RestBuzzScheduler` (rest
  start, change, undo, finish, buzz off = never scheduled); notification
  text builder; service start/stop follows `running` (fake starter).
  No instrumented service test.
- Existing ActiveWorkoutViewModelTest moves with the code or stays green.

## Out of scope
Session detail (7.9), Progress list (7.8), exercise page "How to"
target (7.7), last-time numbers and plan updates (v2), default rest
setting UI (7.10).

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

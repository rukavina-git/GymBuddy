# 11i Feedback: F24b save as template + F6 height and weight wheels

(11j folded in: owner asked for one step.)

## Problem
F24b: after an empty workout (started without a template) the user has
to rebuild the same routine by hand in the template editor. Round C
consult: the offer fits on the summary (Figma 53:3697).

## Change
1. `WorkoutSummary` gets `offerTemplate: Boolean` (true when the
   session had no `templateId`) plus what a template needs: the kept
   exercises in order (exerciseId, name, tracking type, done set count,
   top set reps/duration/distance, rest).
2. `ActiveWorkoutViewModel.saveSummaryAsTemplate()`: builds a CUSTOM
   `WorkoutTemplate` (new UUIDv7, title = session title, one
   `TemplateExercise` per kept exercise: plannedSets = done sets,
   plannedReps / duration / distance from the top set, rest kept, no
   weight) and saves it with `CreateWorkoutTemplateUseCase`. Sync goes
   through the normal outbox. Then the summary shows "Saved as
   template" with an "Edit" link to `templateEditor(id)`. Once only.
3. `WorkoutSummaryScreen`: a quiet card or `AppTextButton` "Save as
   template" under "What you did", above the backup card. Done stays
   the only lime button.
4. Strings in values/strings.xml (and any locale files present).

No api/openapi.yaml or Room change (templates already sync).

## F6 wheels
5. `ui/design/WheelPicker.kt`: a snapping vertical wheel (LazyColumn +
   snap fling, centre row bright, others dim, haptic tick per row).
   Foundation only, no new library.
6. Height (Profile): wheel replaces the keypad NumberSheet. Metric:
   one wheel 50–272 cm. Imperial: ft (2–8) + in (0–11) wheels; stored
   as cm. Starts at the current value, else 175 cm / 5 ft 9.
7. Body weight log sheet and Profile target weight: whole wheel +
   ".1" wheel in kg or lbs (20–700 kg range), starting at the last
   value (log: latest entry, else 70 kg). Replaces the keypad and the
   ± steppers on the log sheet. Clear stays on Profile rows.
8. Delete keypad paths these replace (NumberSheet, log keypad state).
   Sets keep keypad + steppers.

## Figma
Owner waived Figma for this step (device feel wins, like 11f).

## Out of scope
- Save as template from session detail or history.
- Offering it after a template workout that drifted from the template.
- Wheels for set weight/reps (decided: keypad + steppers stay).

## Done when
- Unit test: `saveSummaryAsTemplate` builds the expected template
  (order, set counts, reps, no weight) and only once; template workouts
  get `offerTemplate = false`.
- Unit tests: wheel value mapping (cm <-> ft/in, whole + tenth <-> kg
  and lbs), body weight log starts at the latest entry.
- `./gradlew testDebugUnitTest` and `assembleDebug` green.
- Phone via adb: empty workout → Finish → Save as template → it shows
  in the Workouts tab and opens in the editor.
- Phone: height and body weight wheels snap, save, and show the right
  value on Profile.
- F24b and F6 ticked in device-feedback.md with the commit hash.

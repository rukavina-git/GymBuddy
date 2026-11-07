# 11l Feedback: F3b typing off sheets

## Problem
F3b: typing in a bottom sheet feels cramped; the keyboard pushes the
sheet up and hides the page. Round C decided (2026-11-07): keep sheets
for short pickers and keypads (start, change lift, rest, set keypad,
avatar, add to template, body weight wheel). Typing moves off sheets:
workout/exercise notes and quick create become screens; name and units
go inline on their page.

## Change
1. `ui/design/Overlays.kt`: new `FullScreenEditor(title, onClose,
   action, content)`: a full-screen `Dialog` (no platform width,
   edge-to-edge, `imePadding`), back bar with ✕ + title, content in a
   scroll, primary button pinned at the bottom above the keyboard (like
   11k). System back = close. The first field gets focus on open.
   Overlay, not a nav route: the caller's ViewModel and state stay as
   they are.
2. Notes on `FullScreenEditor`, same fields and strings as today:
   - `NoteSheet` (workout ⋯ → Workout note) → `WorkoutNoteEditor`.
   - `ExerciseNoteSheet` (exercise page note, workout "Your note") →
     `ExerciseNoteEditor`. Opens straight in edit mode in both places;
     the read mode ("Edit note" button, `editing` flag) goes.
   - Unsaved text + ✕/back: discard silently (notes are short; the
     old sheets did the same). Note this in Deviations if changed.
3. `QuickCreateSheet` → `QuickCreateScreen` on `FullScreenEditor`
   (Exercises tab and picker). Same form, prefill, busy, results.
4. Profile name inline: `NameSheet` goes; the Profile page name row is
   an `AppTextField` (Words caps, `MAX_NAME`). Saves on IME Done and on
   focus loss when changed and valid; blank reverts to the saved name.
5. Units inline: `UnitsSheet` goes; the You → Units row shows
   `SegmentedButtons` (Metric | Imperial), tap saves.
6. Delete the old sheets, their sheet enum entries and unused strings.

No api/openapi.yaml, Room, sync or domain change.

## Figma
No frame. Ask owner to waive (device feel wins, like 11f/11i/11k).

## Out of scope
- Other sheets (pickers, keypads, wheels, export/erase).
- Template editor name/notes fields (already on a screen).

## Done when
- Unit test: name inline rule (trim, blank reverts, max length, saves
  only when changed).
- Compose test: workout note editor saves typed text; ✕ saves nothing;
  units segmented control calls setUnits; quick create screen enables
  Create only with a name.
- `./gradlew testDebugUnitTest` and `assembleDebug` green.
- Phone via adb: workout note, exercise note (page + workout), quick
  create (Exercises + picker), profile name, units. Keyboard never
  hides the button or field.
- F3b ticked in device-feedback.md with the commit hash; progress.md
  line points to this file.

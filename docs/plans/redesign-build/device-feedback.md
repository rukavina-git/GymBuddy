# Device feedback (owner, round 1, 2026-11-07)

Owner's phone pass after 7.11. Fixed in rounds before step 12.
Steps (small, one per /step): 11c = A1, 11d = A2, 11e = B, 11f+ = C/D. Tick an item when its fix
lands, with the commit hash.

Tags: **bug** broken · **feel** works but bad · **idea** new/changed
behaviour · **design** needs an Impeccable consult (and a Figma frame)
before code.

## Round A — bugs (11c = A1: F1–F12, 11d = A2: F17–F21c)
- [x] F1 bug (11g, stale server) · Profile picture: the picked avatar shows for a second,
      then goes back to empty.
- [x] F3a bug (d3b7634) · All bottom sheets: the sheet draws over the Android
      navigation bar (inset bug). Fix the insets now; whether to keep
      sheets at all is F3b.
- [x] F7 bug (d3b7634) · Height sheet: Save is off screen; you have to scroll
      the sheet up to see it.
- [x] F10 bug (d3b7634) · Settings → Set a password: the field borders are
      invisible (the fields work).
- [x] F12 bug? (11g, stale server) · Back up (You): 2 changes waiting, "Back up" says
      it's not possible right now. Find out whether the backend was
      down or it's a real bug.
- [x] F17 bug (3694d71) · Workout: a set can be logged with 0 kg.
- [x] F22 bug (3694d71) · Workout: Done on 0 kg × 0 reps does nothing, with
      no message saying why.
- [x] F19 bug (3694d71) · Workout: opening set 3 after set 2 doesn't scroll to
      it; the keypad and steppers are half hidden (with 5 sets, fully).
- [ ] F21a bug (diagnosed, see progress.md) · Workout: tapping the trash icon on a future set
      does nothing.
- [x] F21b bug (3694d71) · Workout: a long press anywhere on a set deletes it,
      with no warning. Long press must not delete; swipe left and
      the trash icon are fine.
- [x] F21c bug (3694d71) · Workout: Undo on the delete snackbar does nothing.

## Round B — quick cuts and small fixes (step 11e)
- [x] F2 feel (dfeaf5d) · Avatar picker: remove the licence line under the
      pictures (credits move to the licences page, Plan 9).
- [x] F4 feel (dfeaf5d) · Profile: remove "Use your letter". New profiles get a
      random animal avatar by default.
- [x] F8 feel (dfeaf5d) · Profile: remove the "used later for BMI…" footer
      (that belongs in the privacy policy, Plan 9).
- [x] F14 feel (dfeaf5d) · Remove the tip "Text size and animation follow your
      phone settings". Tips must be about the app, not Android.
- [x] F9 feel (dfeaf5d) · Export: the "Saved" message is hard to read (dark on
      dark) and too short. Ties in with the snackbar redesign (F21d).
- [x] F11 idea (dfeaf5d) · Settings → Set / change password: a show/hide eye.
- [x] F16 feel (dfeaf5d) · Progress: the collapsed "Progress" title shows a
      hard square box when scrolling; make it smooth.

## Round C — design consults (11f+: Impeccable, then Figma, then code)
- [x] F3b design · (438ff54) Bottom sheets in general: keep them, or use
      something else?
- [ ] F5 design · iOS-style swipe from the left edge to go back, on
      most screens. Same rules as Back: if leaving would lose data, ask
      first. Big change.
- [x] F6 design · (391a60f) Height and weight input: a 4-digit keypad limit for
      height makes no sense (cap it at 3 digits). Keypad or a
      scroll wheel (most apps use wheels)? Same question for weight.
- [x] F13 design · (192e59b) Delete account: 2 taps is too easy for a delete you
      can't undo. An extra screen ("all data is lost for good") and
      typing a word like "delete" to confirm?
- [ ] F15 design · Progress: 5 recent cards feel heavy, and "Show
      all" is out of sight below them. Show 3?
- [ ] F20 design · Workout: check the animation while holding +/−.
- [ ] F21d design · Snackbars: they read as one more dark card, not a
      snackbar. Undo vanishes too fast, and you can't tell how long
      it has left. Owner likes a thin line shrinking right → left
      until it closes. Talk through the details with the owner first.
- [ ] F23 design · Workout summary: 3 stat cards; the last one is
      taller because "kg" wraps to a new line. Better layout?
- [ ] F24a design · Start sheet: Empty workout comes first, but users
      should be steered to templates. Move it last: "Didn't find one?
      Start an empty workout".

### Round C consult (2026-11-07, Impeccable + phone check)
- F3b decided: keep sheets for short pickers and keypads (start, change
  lift, rest, set keypad, avatar, add to template). Typing moves off
  sheets: workout/exercise notes and quick create become screens; name
  and units go inline on their page.
- F5: owner's phone uses 3-button navigation (`navigation_mode=0`), so
  there is no system edge swipe. Do it later; must not fight the system
  gesture when gesture navigation is on.
- F6 decided: wheel for height (cm, or ft + in) and body weight
  (kg + .1, starts at last value). Sets keep keypad + steppers.
- F13 decided: full screen listing what is lost, typing "delete"
  (localised) enables the button, then password / Google re-auth.
- F15 decided: 3 recent; "See all" link next to the Recent heading.
- F20 confirmed on phone (recorded): while holding +, digits get cut off
  on the left ("87.5" shows as "37.5", "132.5" as ".32.5") because
  AnimatedContent's size animation clips while the width changes. Also
  too fast: a 4.5 s hold went 0 → 167.5 kg. Fix: no clip/size animation,
  slot fits "000.0", slower top speed, a tick every 5 steps.
- F21a decided: short swipe left stops and shows a tappable Delete;
  long swipe still deletes; Undo either way.
- F21d decided: Undo 8 s (or the Android accessibility timeout if
  longer), 2 dp lime line along the bottom shrinking right → left,
  timer pauses while touched, line only on snackbars with an action,
  lighter surface.
- F23 is a bug, not design: tiles are ~104 dp wide, "12 400 kg" wraps,
  the Row has no equal heights. Fix: number on one line, unit in the
  label ("kg lifted"), equal heights. Same on Session detail.
- F24a decided: "Didn't find one? Start an empty workout" as a quiet
  button pinned to the sheet bottom (the list scrolls, so last-in-list
  would bury it). F24b fits on the summary after an empty workout.

### New (found during the consult)
- [x] F25 bug · (f97be80) Template time estimate: "djdhdud, 1 exercise · about
      1360 min" and Full Body A/B "5 exercises · about 5 min". The
      estimate seems to use real session length, and one session ran
      ~22 h (Home: "5 workouts · 22 h 53 m"). Owner: the workout had
      no end limit and was still running the next day. Needs a rule for
      stale workouts (auto-end or ask) plus an estimate that ignores
      outliers.
      Decided (owner): no set for 2 h = forgotten. On open: "Still
      training? Your last set was at 18:42 yesterday." → Finish at last
      set (primary; end time = last set, not now) / Keep going /
      Discard (only if no sets). The lock-screen notification turns into
      "Workout still running? Tap to finish". Not opened for 24 h:
      auto-finish at the last set, snackbar on next open. Time
      estimates skip auto-finished sessions. Touches the domain: own
      step, after F20 and F23. Check whether Edit workout can fix
      the existing 22 h session's end time.

## Round D — features (11f+, own step files)
- [ ] F18 idea · Workout: every set starts at 0 kg, even for a lift
      done last week (holding + up to 120 kg). Start from the
      template's target weight; if there is none, from the last
      session's numbers. Probably always prefer the last session.
      Touches the domain, not just the UI.
- [x] F24b idea · (391a60f) After an empty workout: offer "Save as template".

## Notes
- Owner rule for this list: device feel wins over Figma; Figma gets
  updated after (Round C items get a frame before code).
- Order: A, B, then C and D after talking them through.

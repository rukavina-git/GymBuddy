# Device feedback (owner, round 1, 2026-11-07)

Owner's phone pass after 7.11. Fixed in rounds before step 12.
Steps: 11c = Rounds A + B, 11d = Round C design talk, 11e = C + D build. Tick an item when its fix
lands, with the commit hash.

Tags: **bug** broken · **feel** works but bad · **idea** new/changed
behaviour · **design** needs an Impeccable consult (and a Figma frame)
before code.

## Round A — bugs (step 11c)
- [ ] F1 bug · Profile picture: the picked avatar shows for a second,
      then goes back to empty.
- [ ] F3a bug · All bottom sheets: the sheet draws over the Android
      navigation bar (inset bug). Fix the insets now; whether to keep
      sheets at all is F3b.
- [ ] F7 bug · Height sheet: Save is off screen; you have to scroll
      the sheet up to see it.
- [ ] F10 bug · Settings → Set a password: the field borders are
      invisible (the fields work).
- [ ] F12 bug? · Back up (You): 2 changes waiting, "Back up" says
      it's not possible right now. Find out whether the backend was
      down or it's a real bug.
- [ ] F17 bug · Workout: a set can be logged with 0 kg.
- [ ] F22 bug · Workout: Done on 0 kg × 0 reps does nothing, with
      no message saying why.
- [ ] F19 bug · Workout: opening set 3 after set 2 doesn't scroll to
      it; the keypad and steppers are half hidden (with 5 sets, fully).
- [ ] F21a bug · Workout: tapping the trash icon on a future set
      does nothing.
- [ ] F21b bug · Workout: a long press anywhere on a set deletes it,
      with no warning. Long press must not delete; swipe left and
      the trash icon are fine.
- [ ] F21c bug · Workout: Undo on the delete snackbar does nothing.

## Round B — quick cuts and small fixes (step 11c)
- [ ] F2 feel · Avatar picker: remove the licence line under the
      pictures (credits move to the licences page, Plan 9).
- [ ] F4 feel · Profile: remove "Use your letter". New profiles get a
      random animal avatar by default.
- [ ] F8 feel · Profile: remove the "used later for BMI…" footer
      (that belongs in the privacy policy, Plan 9).
- [ ] F14 feel · Remove the tip "Text size and animation follow your
      phone settings". Tips must be about the app, not Android.
- [ ] F9 feel · Export: the "Saved" message is hard to read (dark on
      dark) and too short. Ties in with the snackbar redesign (F21d).
- [ ] F11 idea · Settings → Set / change password: a show/hide eye.
- [ ] F16 feel · Progress: the collapsed "Progress" title shows a
      hard square box when scrolling; make it smooth.

## Round C — design consults (talk in 11d, build in 11e)
- [ ] F3b design · Bottom sheets in general: keep them, or use
      something else?
- [ ] F5 design · iOS-style swipe from the left edge to go back, on
      most screens. Same rules as Back: if leaving would lose data, ask
      first. Big change.
- [ ] F6 design · Height and weight input: a 4-digit keypad limit for
      height makes no sense (cap it at 3 digits). Keypad or a
      scroll wheel (most apps use wheels)? Same question for weight.
- [ ] F13 design · Delete account: 2 taps is too easy for a delete you
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

## Round D — features (step 11e)
- [ ] F18 idea · Workout: every set starts at 0 kg, even for a lift
      done last week (holding + up to 120 kg). Start from the
      template's target weight; if there is none, from the last
      session's numbers. Probably always prefer the last session.
      Touches the domain, not just the UI.
- [ ] F24b idea · After an empty workout: offer "Save as template".

## Notes
- Owner rule for this list: device feel wins over Figma; Figma gets
  updated after (Round C items get a frame before code).
- Order: A, B, then C and D after talking them through.

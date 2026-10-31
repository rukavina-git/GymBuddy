# Design brief (draft, 2026-10-31)

Product truth lives in /PRODUCT.md. This file is the visual starting
point for Plan 3; it is replaced by DESIGN.md once a direction is built.

## What the v1 app looks like today
Walked through on a real device (SM-S731B), Google test account.
- No visual system: ~5 unrelated accents (green avatar/today, coral
  CTA, salmon card, pale-blue quote card), Material defaults elsewhere.
- Home is filler (quote + month calendar). "Start workout" only jumps
  to the Workouts tab.
- Active workout, the core screen: no previous-performance hint, no
  set-done control, no visible rest timer, no progress (x of 6),
  "Weight (kg)" label wraps inside its field, bottom nav stays visible
  mid-workout, keyboard covers the set being edited.
- Finishing a workout shows no summary; it drops back to Templates.
- Sessions list: only name/date/duration ("1 exercises"). Editing a
  session is a dialog with HH:MM:SS boxes.
- Templates: full-width orange button per card, same orange FAB,
  unexplained hide (eye) icon on every row.
- Headers inconsistent (ALL-CAPS titles vs "Good morning,").
- Stats tab is a placeholder.

## The job the design must do
Operate mode: someone mid-set, phone in one hand, harsh or dim gym
light, glancing for seconds. The active workout screen is the product;
everything else (templates, history, library) supports it.

## Palette (owner's proposal)
- Midnight black #030407: the ground. Makes the app dark-first.
- Electric cobalt #1558C9: brand and "where you are now". 3.2:1 on
  black, so not for body text on black; fine as a fill under white
  text (6.4:1). Lighter tint (~#5B8DEB) for cobalt text on black.
- Acid lime #D4FF24: 17:1 on black, the loudest thing on screen. It
  cannot stay "tertiary" visually; proposed role: the one action and
  done colour (log set, completed set, live rest).

## Directions in Figma (page "00 Directions")
Same content in all ten: Push A, Bench Press, set 3 of 4.
01 Meet Board (from the roll), 02 Plate Code, 03 Logbook (light),
04 Cobalt Field, 05 Focus, 06 Rack, 07 Campaign, 08 Instrument,
09 Standard (category default, light), 10 Soft Dark.

Some directions show ideas that are not v1 features (plate breakdown,
top-set history chart, "try 82.5 today" hint). They are there to show
the style; adopting them would be a scope decision, not a styling one.

## Chosen direction: 10 Soft Dark (2026-10-31)
Owner's pick: clean, polished, one bright colour only on what you can
press. The 3 proposed colours are open; palettes under test on Figma
page "01 Soft Dark": lime, ember, mint, violet, cobalt, light.
Refined screens there: active workout (set + resting), home,
workouts, workout complete, history, exercises.

## Palette: lime (chosen) and contrast rules
Lime = "you can press this". Every start/log/done action is lime,
nothing else is (owner confirmed several lime buttons per screen is
fine when each one starts something).

Measured WCAG contrast (sRGB, 2026-10-31):
- text #F3F4F6: 16.8:1 on ground, 13.1:1 on raised surface. AAA.
- muted text #9AA1AD: 7.1 / 6.3 / 5.5:1 on the three surfaces. AA.
- text on lime button #121418 on #D4FF24: 15.9:1. AAA.
- cobalt "current" ring #4D86F0: 4.7:1 on surface (needs 3:1).
- Failed and fixed: dim grey #5E6571 used for upcoming sets and
  chevrons (3.1:1). Now muted; dim is kept only for disabled
  controls, which WCAG exempts.
- Failed and fixed: control outlines in surface greys (1.2-1.3:1).
  New outline token #717886: 3.2:1 or more on every surface.
- Accepted: progress-bar segments and card fills are low contrast on
  purpose; the same information is in text ("3 of 6").
- Touch targets: "How to" pill raised to 48dp. Filter chips draw at
  36dp; their tap area must be 48dp in code.
- State is never colour-only: done = check icon, current = ring +
  expanded card, upcoming = empty ring.
- Lime vs cobalt stays distinct for red-green colour blindness
  (yellow/blue axis); the rare blue-yellow type still has the icons.

## Picks from the idea board (2026-10-31)
Board: https://claude.ai/artifact/QG6t4FY68Euikci3uRkiDs
- A2 app icon: lime plate with G. W2 wordmark: "gymbuddy." (lime dot).
- B1 icons: outline, 2dp round stroke; 12 custom gym glyphs.
- C1 exercise thumbnails: front/back muscle map from muscle groups,
  plus the C4 detail page (large map, toned photo below).
- D1 number entry: own keypad sheet with ±2.5, +5, last-time chips.
- E1 live workout: mini bar above the tabs (music-player style).
- F2 lock screen: expanded notification; set state with lime Done,
  rest state with −15 s / +15 s / Skip.
- G1 rest over: rest card turns lime and becomes the button.
- H1 swipe right = done, left = delete, undo snackbar (no confirm).
- M1 motion: pop/collapse/grow; M2 fade is the reduced-motion fallback.
- N1 nav: Home, Workouts, Exercises, You (Stats hidden for now).
- K1 You screen with "Your data" first.
- I onboarding, refined: (1) Welcome: lime "Create account" (backup,
  all devices), "Sign in", text button "Continue without account";
  (2) account: Google first, then email, Apple; (3) About you: units
  pre-set from locale + experience; (4) starting plan pre-selected
  from experience. Guests get one "Keep this safe? Create an account"
  prompt after their first finished workout, then only in You → Your
  data. Account preferred, never forced.
- J empty states: both kept (they are different cases); every list
  gets one.
All "new" behaviour items are in scope (owner, 2026-10-31).

## Open decisions
1. Palette: lime chosen. Exact token set comes in step 02.
2. Dark-first: confirm. Light mode stays possible via the token modes.
3. Type: the condensed display + plain UI face pairing, or one family.
4. Whether any non-v1 ideas above earn a roadmap line. Owner:
   History (last-time numbers, summaries, totals) is a big topic of
   its own; treat it as a separate future plan, not part of Plan 3.

## Round log: active workout (Figma page "02 Rounds")
- Round 1: A "Focus card" picked over B (one set), C (logbook), D (standard).
- Round 2 locked: B-style scrollable exercise tabs (no progress bar),
  "3 / 15" pill opens all-exercises sheet (jump, reorder, add), swipe
  left / long-press to delete a set + Undo snackbar, ⋯ menu (Discard
  apart, red, confirms), ⌄ instead of ← (workout keeps running, live
  bar above tabs).
- Number entry locked: − / + on the card (2.5 kg, 1 rep, 5 s; hold to
  repeat) + tap the number for a big keypad sheet. No wheels: impeccable
  flagged them (invented, iOS-style, hidden gesture, scroll conflict).
- Round 3: kept keypad, last set "Done · finish workout", timed sets
  (± 5 s), workout complete + guest backup card. Rejected: big rest
  card and lime "rest over" takeover (too forced; users log every
  few minutes and don't watch the phone).
- Round 4: no hint labels under − / + (hold to repeat). Rest = quiet
  line under the last done set, counts down to target, then "+0:45"
  up; no alarm, no buzz (opt-in soft buzz, off by default), nothing
  to dismiss. Finish always confirms with session numbers only; open
  sets get one plain line. Lock screen: silent, Done + Open.
- impeccable critique after round 4: fixed rest text 13 -> 15px,
  removed "Nice session." (no fake motivation), dialog flag icon not
  lime (lime = pressable only).
- Round 5 (impeccable animate): signature move "the set lands" on Done
  (press 0.97 + haptic; card shrinks into its row, check stamps, lime
  sweep; next row grows into the card; rest line fades in; label digit
  rolls; ~520 ms, never blocks). Motion map: odometer digits on − / +,
  shared-axis X between exercises, ⌄ = container transform into live
  bar, M3 sheets/dialogs, no motion when rest passes, no count-up
  numbers, no bounce/confetti. Remove animations = crossfade, haptic
  stays. Hint only first 3 workouts / until first number tap. Limits:
  floors 0 kg, 1 rep, 5 s; ceilings 999.75 kg, 999 reps, 9:59:59;
  disabled − / + at limits. Keypad: disable keys that make invalid
  numbers; message only for typeable-but-invalid (0 reps); empty =
  keep old value.
- Units (owner, after round 5): store kg, check limits in kg, show in
  the user's unit (0–999.75 kg = 0–2,204 lb). Steps 2.5 kg / 5 lb.
  Switching units converts the shown value without touching storage;
  first − / + snaps to the unit's step. Equal ceilings, so a value
  valid in one unit is valid in the other.
- LOCKED 2026-10-31: active workout. Final frames on Figma page
  "03 Active workout (locked)"; rounds stay on "02 Rounds" as history.

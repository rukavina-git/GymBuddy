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
- N1 nav (changed 2026-11-01, council): 5 tabs Home, Workouts, Exercises, Progress, You. Progress replaces v1 Sessions + Stats. Mon-Sun week strip lives on Home.
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
  "03 Locked designs" (Starter plan: max 3 pages); decided rounds are deleted from "02 Rounds" (2026-11-01; still in Figma version history).

## Round log: Progress tab
Councils: docs/council/2026-10-31-history-and-next-flow.md,
2026-11-01-progress-layout.md, -progress-contents.md, -chart-tap.md.
- Round 6: four layouts (A1 summary line, A2 stats in week headers,
  C card feed, H week strip + sideways cards), first build vs v4.
  Owner: A1, week strip moves to Home.
- Round 7 (final): one vertical scroll. Counts line "This week ·
  3 sessions · 2 h 54 m · 9,850 kg" (first build). One chart card:
  best (heaviest) set of one lift per session, plain sentence above
  ("You lift 7.5 kg more than 8 weeks ago."), "Change lift" chip
  (sheet lists lifts with 3+ logs; selected = cobalt). Card hidden
  until a lift has 3+ logs over 2+ weeks, no placeholder. Then last 5
  sessions (PR pill = new best set), "All sessions".
- Card tap opens the exercise page at its Progress section: big chart,
  "Show on Progress" switch, rows (date, best set) that open the
  session. Chart points are not tappable; press-and-drag shows a value
  tooltip only. Whole chart is one TalkBack stop with a summary. At
  175%+ font the line drops, the sentence stays.
- All sessions: search (workout names), Exercise filter chip (shows
  that lift's sets inline), sticky month headers with counts, empty
  search state with "Clear search".
- No Stats hub until v2+ (records, body weight); it will be linked
  from the Progress top bar.
- LOCKED 2026-11-01: Progress tab. Final frames on "03 Locked designs".

## Round log: App frame
Councils/consults: impeccable (2026-11-01), docs/council/2026-11-01-exercise-visual.md.
- Bottom bar: 5 tabs Home, Workouts, Exercises, Progress, You. Active =
  cobalt pill (25%) + filled icon + bold label. Icons: house, checklist
  (Workouts), dumbbell, trend in a box (Progress), person; each has an
  outline and a filled version. When a label no longer fits (measured),
  the bar shows icons only; TalkBack names + long-press tooltip stay.
- Top bars: M3 large title that collapses on scroll (Workouts,
  Exercises, You). Home has no title: "gymbuddy." wordmark, then the
  week strip, then lime "Start workout" (hidden while a workout runs).
- Workouts: no FAB. + in the top bar and a "New template" row. Two
  sections, "Your templates" and "Starter plans". Starters always on,
  read-only (editing makes your copy), toggle in Settings and ⋯ →
  Hide starter plans (Undo); turning off never deletes data.
- Exercises: pinned search, muscle chips, text-only rows (option A;
  list photos may come later, photos are imported for the exercise
  page anyway). Exercise page: toned photo on top, Last / Best under
  it. Search forgives typos (exact first, close after, no extra line,
  "Create …" as a quiet link; same in All sessions). Nothing close →
  lime "Create …". No "Recent" group (owner: low value across splits).
- Live bar: above the tabs on all 5 tabs and pages pushed from them;
  hidden in the workout, sheets, onboarding. States: on a set,
  resting (counts down), rest over (+0:45, no colour, no buzz), timed
  set; big font cuts the name, never the set.
- You (guest): "saved on this phone only" card, lime Create account +
  Sign in; never forced.
- Motion: tab fade-through, live bar grows from ⌄ and slides away,
  title collapses with scroll. Only with Android "Remove animations"
  does movement become crossfades; haptics stay.
- Muscle icons tried (blocky, two open-source anatomy maps) and
  rejected by owner.
- Later idea (owner): "Suggest with AI" for picking exercises, v2+.
- LOCKED 2026-11-01: App frame. Final frames on "03 Locked designs".

## Round log: Home
Councils: docs/council/2026-11-01-home-contents.md (Round 10 "Next up"
rejected by owner: list order is not a plan; no schedule model in v1).
- Home: wordmark, week strip card, lime Start workout, then "Your
  templates": the 3 most recently used, row = name + "6 exercises ·
  yesterday" + play icon; a row tap starts that workout. Then "All
  templates". Nothing else: no Next up, last-session card, weekly
  totals, greeting or streaks (owner sided with impeccable).
- New user: the rows show the starter plan picked in onboarding.
  While a workout runs, rows hide with the button.
- Start sheet: Your templates by recency (last done on the right),
  Starter plans with a one-line description ("Full body plan, day 1
  of 3 · about 45 min"), lime "+ Empty workout" last.
- Week strip card has a chevron; tap opens Progress › All sessions on
  Calendar, at the current month.
- All sessions: List | Calendar segmented button (selected = cobalt +
  check). Calendar: months stacked vertically, newest first, sticky
  headers with counts, no future weeks, no sideways swipe. Done day =
  filled circle + bold number; today = cobalt ring; 2+ sessions =
  count badge. Day tap: 1 session opens it, 2+ open a sheet. TalkBack
  reads "Wed 14 Oct, 2 sessions, Push A, Run".
- Runs/cardio: Empty workout → add Running (DISTANCE_DURATION, km +
  time). Plan 4 adds Running, Treadmill running, Cycling, Rowing
  machine to default_exercises.json (reference data, no schema change).
- v2: weekly schedule (pin templates to weekdays); Home's row spot
  becomes "Today: Push A".
- Not yet checked: Home at 200% font (harden round).
- Start sheet starter line: no made-up minutes. Show the user's own
  average for that template, hidden until there is data (owner, Round 12).
- LOCKED 2026-11-01: Home. Final frames on "03 Locked designs".

## Round log: Templates
Shaped by impeccable (2026-11-01); owner picked page A, editor A, picker A.
- Template page: ←, pencil, ⋯ in the top bar. Title, then "7
  exercises · 23 sets · last done Tue". Numbered rows: name, "4 × 6 ·
  80 kg", muted "rest 3:00" on the right, note under it if any. Lime
  Start workout pinned at the bottom (hidden while a workout runs).
  ⋯: Duplicate, Delete (Undo). Starter: "Starter plan" line, read-only;
  pencil or ⋯ Make a copy opens "Full Body (copy)" in the editor;
  ⋯ Hide this plan (Undo). Both per-plan hide and the global "Hide
  starter plans" stay (owner).
- Editor: own screen (X, title, lime Save). Name field first (empty:
  "Give it a name" on save). Cards: name, drag handle on the right,
  chips [4 sets] [6 reps] [80 kg] that open the workout keypad (Next
  moves on), then rest as timer + value, no chip (quieter, owner). Note
  per exercise in this template: "+ Add note" when empty, the text
  when filled, tap to edit; shown in the workout under the name.
  Reorder by handle (TalkBack Move up/down); swipe removes + Undo.
  Back/X with changes: "Discard changes?" Keep editing / Discard.
  Chips follow tracking type (reps only, time, km + time).
- Rest: shown wherever the plan shows (page, editor, workout subtitle
  "Barbell · 4 × 8 · rest 2:00" in muted), never as big as sets/reps/kg.
  New exercises: 3 × 10, kg empty, rest from Settings default.
- Picker (editor and workout): full screen, search, muscle chips, text
  rows. Tap toggles; picked = cobalt tonal row + numbered cobalt circle
  (order of adding); lime "Add 3". Picks survive searches. "In
  template"/"In workout" rows can still be picked. Back drops picks.
  Added exercises go to the end in pick order, outlined briefly;
  reorder in the editor.
- Editing a template doesn't change a running workout.
- Changed a locked flow (owner): active workout subtitle gains rest.
- LOCKED 2026-11-01: Templates. Final frames on "03 Locked designs".

## Round log: Exercise page + create
Round 13 (2026-11-01/02). Council: docs/council/2026-11-02-exercise-flags.md.
- Exercise page A, one scroll: photo, name, "Chest · Barbell",
  Last / Best, quiet rest row (timer + muted "Rest 2:00 between sets",
  tap = time sheet, per exercise, used when added to a template or
  workout), your note, Progress (sentence, chart, Show on Progress),
  History (3 rows + "All 14 sessions"), How to, Tips, Muscles and
  equipment. No main button; ⋯ built-in: Add to template, Hide from
  lists; own: Edit, Add to template, Hide, Delete.
- Own exercise: no photo, name on top. Never done: no numbers, no
  progress, nothing empty.
- Create A: one screen (name, what you log, main muscle, equipment;
  More details folded: other muscles, steps, rest). Difficulty,
  category, compound/isolation get defaults. Create C: quick sheet from
  search or the picker, name prefilled.
- Rest is one fixed number (owner): a range (1:00–3:00) would need a
  schema change and confuses the timer. Not planned.
- Delete own exercise (owner, council d): dialog names the templates
  ("They keep it until you change them. Past workouts keep it."),
  Cancel / Hide instead / red Delete. Templates still start with it;
  the editor shows "Deleted exercise" with Swap, Remove, Restore.
  Plan 4: start from the template's snapshot name + tracking type.
- No "Make a copy" of built-ins in v1; doubtful for v2 (owner).
- Notes in a workout (owner + impeccable): one note only. Plan note
  wins; your note sits behind a small note icon (one tap). If only your
  note exists, it shows. Owner: "no loud screen during workout, only
  crucial info."
- Own exercises' ⋯ also has "Hide from lists" (council idea).
- LOCKED 2026-11-02: Exercise page + create. Final frames on "03
  Locked designs" (after Active workout).

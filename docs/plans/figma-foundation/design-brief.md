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
- Per-exercise rest moved to v1 scope (owner, 2026-11-02).

## Round log: Session detail
Round 14 (2026-11-02). Owner picked B: "you mostly don't care what you
lifted in each set".
- Opens from All sessions, the calendar, Progress last 5 and an
  exercise's History. Title, "Mon 26 Oct · 18:24–19:16", tiles (time,
  sets, lifted; a run shows distance and pace), session note.
- One row per exercise: "4 sets · top 82.5 kg × 8", PR pill (new
  heaviest set). Tap opens the sets in place; "Bench Press page ›"
  inside opens the exercise page. From an exercise's history: that row
  opens, outlined in cobalt briefly. No set-type tags (v2).
- ⋯: Start Push A again (only if from a template), Edit workout,
  Delete (Undo snackbar, no confirm).
- Edit: own screen with Save; name, date, start/end, note; same rows,
  open one to edit sets with the workout keypad; + Add set; swipe a set
  to delete (Undo).
- Empty workout sessions are called "Workout"; rename in Edit.
- LOCKED 2026-11-02: Session detail. Final frames on "03 Locked
  designs" (after Progress).

## Round log: You + Settings
Round 15 (2026-11-02). Council: docs/council/2026-11-02-you-settings.md.
Reviewed by impeccable (critique applied, dialogs rebuilt to M3).
- One page (owner + impeccable): account card, then groups Workout
  (weight kg/lb, distance km/mi, default rest 2:00, weight step 2.5 kg,
  rest buzz off, keep screen on in a workout on, workout notification →
  Android settings), Calendar (week starts on, from locale), Lists
  (starter plans, hidden exercises, hidden templates), Your data
  (export; guests also "Erase data on this phone"), Help (show tips
  again, About), Account (change password for email accounts, sign out,
  delete account in red). "Text size and animations follow your
  phone's settings." Cut: quote of the day, sync debug (debug builds),
  Theme and Language rows (v2: light theme, Croatian).
- Switch on = cobalt (a state), not lime. Locked switches (Show on
  Progress) follow in the components step.
- Account card: picture, name, email, one backup line ("Backed up · 2
  min ago" check / "3 changes waiting" cloud-off / "Couldn't back up ·
  Retry" after 24 h), Profile ›. Guest card: "saved on this phone only",
  lime Create account, Sign in. Words: "On this phone" / "In your
  account".
- Profile: picture (your letter or one of 20 Fluent Emoji animals, MIT,
  stored as an id; own photo upload is v2), name, email as plain text,
  body weight, experience, optional About you (height, birth date,
  gender, activity level, goal, target weight; "—" until set; for BMI
  and calorie target in v2).
- Body weight (v1, owner): current value, change over 30 days, chart
  30/90 days/1 year/all, entries, All entries, lime Log weight; sheet
  with − / + (0.1 kg, hold to repeat), tap to type, date.
- Guest mode (ADR 0014): create account says what moves ("Your 14
  workouts… Nothing on this phone is removed"); sign in merges.
- Sign out with unsynced changes: dialog, stacked "Back up and sign
  out" / "Sign out anyway" (red) / Cancel; offline greys out back up.
- Delete account: dialog with radio Keep my workouts (default,
  continue as guest) / Erase them too ("Delete and erase"), then
  re-auth. Guest erase: Export first / Erase all / Cancel.
- Export (ADR 0015): JSON backup (re-importable) + CSV of sets.
- LOCKED 2026-11-02: You + Settings. Final frames on "03 Locked
  designs" (after Session detail).

## Round log: Onboarding + auth
Round 16 (2026-11-02). Research on Strong, Hevy, Fitbod, Strava, NTC,
MyFitnessPal, Duolingo; council docs/council/2026-11-02-onboarding.md
(8 members, 2 rounds) + 5 owner reviews; impeccable critique applied.
- Welcome: logo, "gymbuddy.", "Log your workouts. Works offline, no
  account needed.", lime "Get started", outlined "I have an account".
- Setup, tap = next (options are buttons; Back keeps the answer; Skip
  uses defaults): experience → goal (build muscle, get stronger, lose
  weight, stay fit and healthy, improve endurance) → days a week (1–3,
  4, 5–7, Not sure yet) → starting plan (suggested by days: Full Body,
  Upper/Lower, Push/Pull/Legs, or no plan; goal-based suggestions v2)
  → "You're all set" / "No streaks, no feed, no ads. Just your
  workouts." / 3 Good-to-know tips / lime "Let's go" → Home. Units
  come from the region, never asked.
- Sign-in (centred): Android's account sheet + Google's own dark
  button with the G logo, then email + password (email remembered if
  used last time; one password field with Show; paste and password
  managers; errors under the field: "Email or password is wrong").
  No Apple on Android, no magic links. Sign-in that finds no account
  asks before creating one.
- Create account: says what moves into the account, unticked consent
  box for health data (wording final in roadmap 4.6), Terms/Privacy
  links.
- Guest → account with data: "Add this phone's workouts?" with counts;
  "Stay signed out" keeps the guest; receipt after.
- Account offer: card after a finished workout, ≤ 1 a week, ≤ 5 total.
- Session expired: banner "Backup paused. Sign in again to keep backing
  up"; keep logging. Another account signing in never sees the first
  account's unsynced changes (they wait for that account). Account
  deleted elsewhere: counts, Keep on this phone / Erase….
- Notifications: a quiet "Workout on the lock screen · Turn on" row on
  the first rest after install; system dialog only on tap.
- Later in v1: error/offline messages round; plans + default
  templates rework (roadmap 4.2h). Logo done in step 04 (own ECG
  line, 2026-11-02).
- LOCKED 2026-11-02: Onboarding + auth. Final frames on "03 Locked
  designs" (after You + Settings).

## Round log: Error, offline, empty states (06f)
Brief (2026-11-02). Owner picked all 12 cases.
Already designed, reused not redesigned: You-card backup line, session
expired banner, undo snackbars, sign-in field errors, sign out with
unsynced changes, account deleted elsewhere.
Facts from the code:
- The app works fully offline. Exercises and starter templates are
  bundled (assets/default_*.json), so the library is never empty on
  first launch; only photos and reference updates need the network.
- 410: the sync engine flushes the outbox, wipes local user data and
  pulls everything again (SyncEngine fullResync). Workouts are briefly
  missing on screen while it runs.
- Rate limit / server down: sync fails quietly today; the outbox keeps
  the changes.
Cases:
1. Offline indicator: show at all? How quiet?
2. Backup stuck 24 h+ outside You (Home? live bar? nowhere?)
3. Full re-download (410) while workouts reload
4. Server down / busy while online
5. Couldn't save (local write failed, e.g. storage full) mid-set
6. Workout restored after the app was killed mid-workout
7. Exercises not loaded: photos or reference updates missing offline
8. Image failed (exercise photo, profile picture)
9. Empty-state audit: every list
10. No search results: Exercises, All sessions, add exercise
11. Thin data: charts with 0–1 points, a lift used once, empty week
12. Loading: skeleton vs nothing; where waiting is visible
Constraints: calm workout screen (nothing new while logging unless the
set is at risk); never blame the user; never colour-only; dark only;
no new features (no retry queues UI beyond what sync does).
Shape (impeccable, 2026-11-02). Mode: Operate. Owner picks:
- Offline: no global indicator. Network-only actions (sign in, back up
  now, photos) say "Needs internet" when tapped; You card shows
  waiting changes.
- Backup stuck: quiet Home card after 3 days of failing, with Retry.
  Never on the workout screen.
- Killed mid-workout: reopen straight into the workout, snackbar
  "Workout restored", timer counts from the real start.
Brief:
- Job: tell the truth about state without stealing attention. Offline
  is normal, not an error. Loud only when a set could be lost.
- Loud tier (1 case): Couldn't save. Stays on screen until resolved,
  keeps the typed numbers, says what to do (free up space, Retry).
- Quiet tier: backup stuck, server down (same as backup waiting, no
  separate message), 410 reload ("Getting your workouts…" in place of
  lists, no wipe visible), image failed (muscle map fallback, letter
  avatar), photos offline.
- Empty / thin / no results: one pattern for every list (glyph, one
  line, at most one action); charts with 0–1 points show the number,
  not a broken chart.
- Loading: local reads are instant, so no skeletons on local lists;
  waiting is only visible on network actions (sign in, back up, 410).
Wide round (Figma "02 Rounds", 3 options + 1 category standard each):
A. Empty-state pattern across all lists. B. Couldn't save. C. Backup
stuck Home card + 410 reload. D. Thin-data chart. Rest follow the
picked patterns.
Open: restore after very long gaps (e.g. 6 h+) may need "Finish or
resume?"; owner to confirm in Wide.
Wide round 17 (Figma "02 Rounds", section "Round 17 · Error, offline,
empty"): B couldn't save (B1 inline above button, B2 dialog, B3 banner
under tabs, B0 snackbar), C1 backup stuck (C1a card under Start, C1b
line in week card, C1c card at bottom, C0 M3 banner), C2 re-download
(C2a in place, C2b thin bar, C2c full screen with count, C0 spinner),
D lift done once (D1 sentence, D2 one dot, D3 no section, D0 chart
with one point). Empty-state pattern reuses locked Progress R7-3/R7-9.
Critique flags: C2c blocks Start (breaks "works offline" promise);
B2 interrupts the calm workout screen; C1b Retry needs a 48dp tap area.
Owner picks (round 17): B1 inline above the button, C1b line in the
week card, C2a in place, D3 no Progress section until 3 sessions.
Critique (impeccable, native design review, 31/40, 0 P0, 3 P1), all
6 fixes applied (owner): B1 card gains "Storage settings", body 14;
C1b whole line is a 48dp row, tap = "Backing up…" then the line goes;
C2a keeps "Your templates" + "Coming back from your account.", failure
"Couldn't get your workouts · Retry", Start sheet offers starter plans
+ Empty workout while reloading (owner); D3 one box "First time".
Plan 4 note: sets logged during a 410 reload must survive the wipe.
Restore rule (owner OK): last set < 3 h ago → straight back in; 3 h+
→ Home card "Unfinished workout" with Resume and lime Finish; Finish
sets the end time to the last set.
Row E (follows the picks): E1 workout restored snackbar (4 s), E2
"Unfinished workout" card on Home (3 h+; Resume / lime Finish), E3
backup Retry while offline ("Needs internet. Safe on this phone."), E4
photo not downloaded offline (photos load from the web and are cached;
quiet stand-in, no error), E5 body weight empty, E6 hidden exercises
empty. Already locked elsewhere: no results, Workouts with starters
off, Progress empty, sign in / sign out offline.
Critique row E (native review, 32/40, 0 P0, 0 P1). Copy fixed: E5 "a
chart appears after 3 entries", E6 "Bring it back from here". Open
P2s for the owner: Discard on E2, E3 reverting to Retry.
Owner (row E): E2 shows three buttons, Discard (red, confirms) ·
Resume · lime Finish. E3 "Needs internet" shows 4 s, then back to
"Not backed up for 3 days · Retry"; backup runs by itself once online.
- LOCKED 2026-11-02: Error/offline states. Final frames on "03 Locked
  designs" (after Onboarding + auth). Round 17 removed from 02 Rounds.

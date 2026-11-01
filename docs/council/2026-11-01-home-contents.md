# Council: Home contents, calendar, cardio (2026-11-01)

## Brief
```
COUNCIL BRIEF: Home tab, round 2 (GymBuddy, native Android, Soft Dark redesign)

Locked top of Home: wordmark, 7-day week strip (done days ticked), lime "Start workout"
(opens a sheet: your templates, starter plans, "Empty workout"). Hidden while a workout runs.
Round 10 proposed a "Next up" card = template after the last finished one, in Workouts order.
Owner rejected that logic: "nobody does chest #1, chest #2, chest #3"; it only makes sense with a
fixed week ("chest every Monday"); it gives 0 value if I just go for a run. Owner also says Home
"still looks kinda empty" even with Next up, and wants: tap the week strip -> a real month calendar
(scroll months, see other workouts). "Maybe it exists as stats somewhere as well?"

Q1. What should Home suggest, if anything?
 a) Nothing; show "Your templates" list (category standard: Strong/Hevy)
 b) Rotation only inside a user-made plan (templates grouped e.g. "PPL"); loose templates never rotate
 c) Weekly schedule: user pins templates to weekdays (Mon = Push A); Home shows today's
 d) Weekday focus labels (Mon = "Chest"); Home offers templates matching today's focus
 e) Recently used / most used templates (no prediction)
Q2. What fills Home under the button?
 a) Nothing more (accept a quiet Home)  b) This week: sessions, time, sets vs last week
 c) Last session card (name, date, PRs)  d) Recent PRs  e) Templates quick list (as Q1a)
Q3. Week strip tap / month calendar:
 a) Full-screen month calendar from Home; tap a day -> that session
 b) Calendar is a view inside Progress > All sessions (List | Calendar); strip tap jumps there
 c) Calendar card on Progress tab itself; strip tap opens Progress
 d) No calendar; strip day tap -> that day's session only
Q4. Logging a run or other cardio:
 a) Already possible: Empty workout + an exercise with DISTANCE_DURATION tracking; just surface it
 b) A "Log activity" quick entry on Home (type, time, distance)  c) Out of scope for v1

Constraints / facts:
- v1 feature scope frozen; no Room schema or api/openapi.yaml change in Plan 3. A schedule/plan/focus
  concept does NOT exist in the data model (WorkoutTemplate = title + exercises + source). Options
  b, c, d of Q1 need a new model -> v2 at earliest; say what v1 should do until then.
- Sets already support duration/distance (ExerciseTrackingType: WEIGHT_REPS, REPS_ONLY, DURATION,
  WEIGHT_DURATION, DISTANCE_DURATION, WEIGHT_DISTANCE). Default library: 60 exercises, no running one.
- Progress tab is locked: counts line, one lift chart (hidden until enough data), last 5 sessions,
  All sessions (search, filter, month headers). No Stats hub until v2+.
- Home rules so far: no greeting, no streaks, no fake motivation, no kicker labels, no hero metric,
  no same-size card grids, lime = pressable only, one main action.
Read: PRODUCT.md, docs/plans/figma-foundation/design-brief.md (Round logs), docs/roadmap.md.
```

## Answers

### Design director (impeccable)
VOTE: Q1 e, Q2 e, Q3 b, Q4 a
- Operate mode: success is time-to-task, not fullness. Empty is fine; filler is the risk (craft-floor bans hero metrics, placeholder content). Only what shortens the path to starting earns a place.
- Templates by recency as one-tap rows under lime; recognition over recall; rows not cards, not lime, so one primary action.
- Calendar as List | Calendar in All sessions; strip tap jumps there. No duplicate of Progress.
- Cardio already works via an existing tracking type; make it findable.
RISK: rows partly duplicate the Start sheet; cap the count to screen height.
VETO: Q4b (second entry competes with lime; scope creep). Also Q2b (duplicates counts line, near hero metric).
IDEA: add Run, Bike, Row (DISTANCE_DURATION) to the default library.

### Product owner
VOTE: Q1 a, Q2 b, Q3 b, Q4 a
- Freeze: no plan/schedule model, so v1 can't suggest. "Your templates" one-tap is honest and standard.
- Q2b uses stored sessions only (principle 3), gives the strip a reason. PRs are v2 per §14.2.
- Q3b answers "maybe it exists as stats": no new screen, locked Progress untouched.
- Q4a: in Empty workout, search "run" offers "Create Run".
RISK: list duplicates the Start sheet; cap at 3 rows + "All templates".
VETO: Q4b (new feature, second primary). Q1c/d in v1 (schema change).
IDEA: add "Weekly schedule" to the §14.2 v2 list; design the Home slot now so v2 becomes "Today: Push A" without a Home redesign.

### Android architect
VOTE: Q1 e, Q2 c, Q3 b, Q4 a
- Recency from WorkoutSession.templateId + startedAt (DAO GROUP BY); pure read, offline.
- Last session card = existing queries. No PR detection exists yet; it's a domain use case, no schema change. Ship without PRs if short on time.
- Calendar is cheap: kizitonwose calendar-compose 2.10.1 already a dependency; HomeScreen.kt has a WorkoutCalendar to move. M3 DatePicker can't mark days.
- DISTANCE_DURATION sets already sync.
RISK: last session + recents may still feel thin.
VETO: Q1c/d for v1 (new entity, migration, openapi, backend sync).
IDEA: add Running and Cycling to the default library (reference data, not schema).

### Accessibility and gym floor
VOTE: Q1 e, Q2 c, Q3 b, Q4 a
- 3 recent templates as full-width 56dp+ rows: one-thumb, one tap fewer than button + sheet; "See all" row.
- Last session card = one TalkBack stop; reflows at 200%. Q2b duplicates counts line, Q2e duplicates Q1.
- 7-column grid at 200% leaves ~48dp cells, number only. List view beside it keeps a readable path. Day label "Tue 4 Nov, 1 session, Push A"; done days need shape, not colour only.
- Q4a needs a running exercise to be findable.
RISK: recency ignores rotation; the needed template may be 2nd or 3rd (still one tap).
VETO: Q3c (calendar card fails at 200%, breaks locked Progress).
IDEA: months stacked vertically with sticky headers (like All sessions), not a horizontal pager; 48dp prev/next; open on current month.

### Beginner lifter
VOTE: Q1 e, Q2 c, Q3 b, Q4 a
- I don't follow a fixed week; last 2–3 templates under the button, no thinking. Rotation would guess wrong.
- Last session card is my confidence check. Week vs last week would make me feel behind.
- Strip tap should go to my history, not a new screen to learn.
- I'd log a jog as Empty workout + Running, only if Running is in the library.
RISK: day 1 has no recents or last session; needs a calm empty state pointing to starter plans.
VETO: Q1c/d (schedule setup overwhelms; missing Monday feels like failing).
IDEA: one-line description under each starter plan in the Start sheet ("3 days/week, full body, ~45 min").

### Veteran lifter
VOTE: Q1 e, Q2 c, Q3 b, Q4 a
- On Strong/Hevy I only touch templates on Home. Recency covers fixed split and loose rotation. Q1c fine for v2.
- Last session (name, date, PRs, top set) is the one card I'd read; weekly totals are noise.
- Calendar in All sessions keeps one history.
- A run is a session with a DISTANCE_DURATION exercise; add a reference "Run". One history.
RISK: sparse for a new user; needs "Start from a starter plan" empty state.
VETO: Q4b (splits history, second primary, scope creep).
IDEA: each template row shows "last done 6d ago"; oldest-done-first sort solves loose rotation with no prediction.

## Synthesis (chair)
| | Q1 | Q2 | Q3 | Q4 |
|---|---|---|---|---|
| impeccable | e | e | b | a |
| Product owner | a | b | b | a |
| Architect | e | c | b | a |
| Accessibility | e | c | b | a |
| Beginner | e | c | b | a |
| Veteran | e | c | b | a |

- Agree: no prediction in v1; Home shows your templates as one-tap rows, most recent first (PO's "a" is the same list). Calendar lives in All sessions as List | Calendar, strip tap jumps there (6/6). Cardio = existing tracking type, add Run/Bike/Row to the library (6/6).
- Split: Q2. Four want a last-session card; impeccable wants nothing beyond the list; PO wants a this-week line (impeccable and a11y would veto that as duplicating the counts line).
- Vetoes: Q4b (3), Q1c/d in v1 (3), Q2b (impeccable), Q3c (a11y).
- Ideas kept: "last done 6d ago" on rows (veteran); vertical months, no pager (a11y); starter plan one-liners (beginner); weekly schedule on the v2 list with a Home slot reserved (PO).
- Recommendation: Home = Start workout, then 3 template rows by recency with "last done", then "All templates". Mock Q2 both ways (with and without a last-session card) and let the owner pick; impeccable's view leans no card. Running etc. join the default library in Plan 4 (reference data). Weekly schedule goes on the v2 list.

Owner (2026-11-01): agrees with the council on Q1 e, Q3 b, Q4 a. Q2: impeccable (no card; Home = Start + 3 recent templates). Mocked as Round 11; lock pending.

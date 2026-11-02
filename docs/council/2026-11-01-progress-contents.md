# Council 2026-11-01: Progress tab contents

## Brief
```
COUNCIL BRIEF: what goes on the Progress tab besides history (GymBuddy, native Android, Soft Dark redesign)
Decided by owner: 5 tabs (Home, Workouts, Exercises, Progress, You). Progress is one vertical scroll (layout "A1"). A counts line on top in the first build: "This week · 3 sessions · 2 h 54 m · 9,850 kg". The Mon-Sun week strip lives on Home, not Progress. Owner: Progress must NOT be just a list of past workouts; it should have stats, or entries leading to detailed stats, and grow in v3/v4 (graphs, records, suggestions, body weight). Owner likes the A1 v4 chart card (Bench press top set, 80 -> 87.5 kg in 8 weeks, sparkline, "Show as table") because it breaks the list's monotony and can later open a fuller stats screen.
Figma reference: Round 6 frames R6-A1 first build / R6-A1 v4 (Figma file MdD33rDNKJ3S82azTOYfEk, page "02 Rounds"; you cannot open it, the description above is enough).
Q1. What sits between the counts line and history:
 (a) one featured chart card (like A1 v4); tapping opens a Stats screen that grows over versions
 (b) a short list of entry rows to detail screens (e.g. "Records", "Exercise progress", "Body weight"), no chart on the tab
 (c) one chart card + 1-3 entry rows under it
 (d) nothing extra (category standard in Strong: history tab is a list; charts live elsewhere)
Q2. What the chart shows in the first build (data = finished sessions with sets only):
 (a) weekly volume, last 8 weeks (bars)  (b) one lift's top set over time (user picks/pins the lift; default = most-logged)  (c) sessions per week, last 8 weeks  (d) user can swipe/switch between a few  (e) no chart until v2
Q3. How much history the tab shows, and where the rest goes:
 (a) full infinite list on the tab (category standard)
 (b) last 7 days + "All sessions" button to a full screen
 (c) last 30 days + "All sessions"
 (d) last 5 sessions + "All sessions" (count, not time; works for 1x or 6x/week lifters)
 (e) this week + last week + "All sessions"
Also say: what the "All sessions" screen needs (search, filter by exercise, month headers...).
Constraints: Operate mode, Material 3, offline-first (local Room data), WCAG AA, 48dp, font 200%, TalkBack. Lime #D4FF24 only on pressable things, cobalt #4D86F0 = you are here. No fake motivation, no streak guilt. Honest: never show a chart that has no real data behind it (new user with 0-2 sessions!). No API/Room change in this plan; design only, Plan 4 builds.
Read: PRODUCT.md, docs/plans/figma-foundation/design-brief.md, docs/council/2026-11-01-progress-layout.md (previous council + owner decisions). Repo /Users/karlo/Dev/GymBuddy. Do not edit files.
```

## Answers

### Design director (impeccable)
VOTE: Q1 (a), Q2 (b), Q3 (d)
- Operate: a working tracker first, one signature move; the chart card is it. Entry rows on top of a card drift into craft-floor's same-size-cards and hero-metric bans and add decisions before history. Records/Body weight rows belong inside Stats.
- Q2 (b) is the only chart that shows progress; volume or sessions per week repeat the counts line. Default most-logged lift, "Change lift" in Stats.
- Q3 (d): works for 1x and 6x/week, keeps history in first viewport and tab height steady at 200%.
- Honesty (principle 3, onboard first-use): no chart without real data.
SKETCH: top bar / counts line / chart card ("Bench press top set: 80 → 87.5 kg in 8 weeks", sparkline, "Show as table", whole card opens Stats) / "Recent" / 5 rows (name, date, duration, top set) / "All sessions" (lime) / empty state. All sessions: sticky month headers with counts ("October · 12 sessions · 41,200 kg"), Exercise filter chip, search by session name, no-results with "Clear filter".
RISK: one card can feel thin until Stats fills up.
VETO: Q2 (d) carousel in a scroll; hidden charts, TalkBack fights.
IDEA: silent threshold: card appears once a lift has 3+ sessions over 2+ weeks; no slot, no "coming soon" before.

### Product owner
VOTE: Q1 (a), Q2 (a), Q3 (d)
- v1 freeze puts charts/PRs/body weight in v2. Counts line accepted; weekly volume is the same aggregate over 8 weeks, the cheapest chart. Lift chart needs picking/pinning, waits for v2.
- One card opening Stats is the growth point: v2 lift chart, v3 records, v4 body weight inside Stats.
- (d) works for 1x and 6x/week, history in first viewport.
- Card only after 2 finished weeks with weighted sets.
SKETCH: top bar / counts line / volume card (8 weekly bars, opens Stats) / Recent 5 / All sessions / empty state. All sessions: month headers with counts, "Exercise: Squat" filter chip, search v2.
RISK: volume says little to bodyweight/timed lifters.
VETO: (b) entry rows to v2 features = empty screens or scope creep.
IDEA: one chart component taking data as input, reused by Progress, Stats, exercise detail.

### Android architect
VOTE: Q1 (a), Q2 (a), Q3 (d)
- Stats is one route that grows; entry rows would be empty doors.
- Q2 (a): getWorkoutSessionsByDateRange already feeds counts; 8 weeks gives the bars. Canvas ~100 lines, no library. (b) has no cheap query today: getAllWorkoutSessions() is N+1 over all history.
- Q3 (d): bounded load; unbounded list re-runs the N+1 load in the same LazyColumn.
- Later: index workout_sessions(deletedAt, startedAt); SQL weekly volume sum; MAX(weightKg) per session for one exerciseId with index performed_exercises(exerciseId, workoutSessionId); Paging 3 for All sessions.
SKETCH: title / counts / 8-week volume card (hidden until 2+ weeks) / Recent 5 / All sessions. All sessions: month headers with count, filter chip later, no search in first build.
RISK: volume is less exciting than the bench chart the owner liked.
VETO: Q3 (a) unbounded list on today's N+1 Flow.
IDEA: one 8-week summary use case feeds counts, chart and table so they never disagree.

### Accessibility / gym ergonomics
VOTE: Q1 (a), Q2 (b), Q3 (d)
- One card = one TalkBack stop with a real summary; entry rows add stops and push history out at 200%.
- Single series + text headline is the best text equivalent; line in #F3F4F6 with labelled end points, not lime/cobalt; bars mush in glare.
- (d) keeps tab length steady; time windows leave a 1x/week lifter with 0-1 rows.
- Under 3 sessions: text only "Log bench 3 times to see a trend", no axes, no ghost line.
SKETCH: heading / counts text / chart card (one target, opens Stats, "Show as table") / "Recent" / 5 rows ≥72dp / "All sessions" full-width 48dp. All sessions: month headers as headings, search, Exercise chips, same rows, empty filter "Clear filter".
RISK: chosen lift abandoned; visible "Change lift" + TalkBack action.
VETO: Q2 (d) swipe charts.
IDEA: at font 175%+ drop the sparkline, keep headline + "Show as table".

### Beginner lifter
VOTE: Q1 (c), Q2 (b), Q3 (d)
- One lift going 40 → 47.5 kg answers "am I getting stronger?"; 9,850 kg means nothing.
- A couple of rows like "Records" give me something to explore without a wall of charts.
- Last 5 always shows something, never a telling-off.
SKETCH: counts / chart card ("Squat top set, 3 weeks: 40 → 47.5 kg", "Change lift") / Records (firsts: "First time 50 kg squat") + Exercise progress rows / Recent 5 / All sessions. 0-2 sessions: "Log 3 sessions with the same lift to see your trend." All sessions: month headers, search, filter by exercise.
RISK: default most-logged lift may be a warm-up.
VETO: Q2 (a) volume number with no explanation.
IDEA: one sentence under the chart: "Your heaviest set on squat, each session. Up = stronger."

### Veteran lifter
VOTE: Q1 (a), Q2 (b), Q3 (d)
- "Is my bench going up?" answered in a second. Rows to unbuilt screens are dead ends; add later.
- Top set is real; weekly tonnage mixes leg press with curls, rewards junk volume.
- Count, not time: 7-day window is empty after a deload.
- Strong buries charts, Hevy paywalls them; trend on the tab beats them.
SKETCH: counts / chart card (lift picker chip, "Show as table") / Recent 5 (top set) / All sessions. All sessions: sticky month headers with count, filter by exercise shows that lift's sets inline, search by name.
RISK: new user gets no chart; honest empty state needed.
VETO: Q2 (a) vanity number.
IDEA: tap a chart point to open that session.

## Synthesis
| Member | Q1 | Q2 | Q3 |
|---|---|---|---|
| Design (impeccable) | a | b | d |
| Product owner | a | a | d |
| Android architect | a | a | d |
| Accessibility | a | b | d |
| Beginner | c | b | d |
| Veteran | a | b | d |

Q1: 5 a (one chart card that opens a growing Stats screen), 1 c. Entry rows belong inside Stats until their features exist.
Q2: 4 b (one lift's top set), 2 a (weekly volume, on cost/scope). Vetoes: volume ×2 (beginner, veteran), swipe charts ×2 (impeccable, a11y). Architect: lift chart needs a new per-exercise query in Plan 4 (today's load is N+1).
Q3: unanimous d: last 5 sessions + "All sessions". Time windows fail 1x/week lifters and deload weeks.
All sessions: sticky month headers with counts, filter by exercise; search split (yes: impeccable, a11y, beginner, veteran; later: PO, architect).
Shared: honest threshold, no chart (and no placeholder) until a lift has 3+ sessions over 2+ weeks.
Ideas: plain sentence under the chart (beginner); tap a point opens the session (veteran); drop sparkline at 175%+ font (a11y); one reusable chart component (PO); one summary use case feeds counts/chart/table (architect).
Recommendation: Q1 (a); Q2 (b) lift top set (impeccable + users, owner liked it; Plan 4 adds the query); Q3 (d).
Owner only: Q2 lift vs volume; search on All sessions in the first build.

Owner (2026-11-01): Q1 (a) one chart card. Q2 (b) best set over time. Q3 (d) last 5 + All sessions. Search on All sessions in the first build. What a tap on the chart opens goes to the next council (2026-11-01-chart-tap).

# Council 2026-11-01: Progress tab layout

## Brief
```
COUNCIL BRIEF: layout of the Progress tab (GymBuddy, native Android, Soft Dark redesign)
Decided: 5 bottom tabs Home, Workouts, Exercises, Progress, You. Progress replaces v1's Sessions list (history) and the empty Stats tab. Owner wants it to GROW: v2 adds simple stats, v3/v4 graphs, PRs, suggestions ("add 2.5 kg"), maybe body weight. Design it so later features slot in without a redesign.
Q1. Overall structure of Progress:
 (a) Category standard: one scroll. Small summary on top (this week), session history list below grouped by week/month.
 (b) Top tabs inside Progress: History | Stats (later | Records | Body...).
 (c) Modular card feed: "This week", "Recent sessions", later "PRs", "Bench trend", "Suggestion" cards; each opens a full screen. History = "All sessions" card/screen.
 (d) Calendar first: month calendar (days with workouts marked) on top, tap a day for its sessions, list under it.
 (e) Hub: short list of sections (Sessions, Exercises, Records, Body) each opening its own screen.
Q2. What the first build (Plan 4, v1 data only = finished sessions with sets) shows:
 (a) session list only + reserved empty slot for stats
 (b) list + 2-3 counts derived from existing data (sessions this week, streak-free, volume)
 (c) calendar + list
Q3. Where per-exercise progress ("my bench over time") lives later:
 (a) exercise detail only (b) Progress only (c) both, Progress links to exercise detail (d) a long-press quick sheet anywhere an exercise appears
Constraints: Operate mode, Material 3 nav and system Back, offline-first (all from local Room data), WCAG AA, 48dp, font scale 200%. Lime #D4FF24 only on pressable things, cobalt #4D86F0 = where you are. No fake motivation, no streak guilt (PRODUCT.md). No API or Room change in this plan; this is design only. Active workout is locked. We will mock 3-5 layouts in Figma for the owner next, so propose layouts with concrete content.
Read: PRODUCT.md, docs/plans/figma-foundation/design-brief.md, docs/plans/figma-foundation/00-overview.md, docs/council/2026-10-31-history-and-next-flow.md (previous council). Repo: /Users/karlo/Dev/GymBuddy. Do not edit any files.
```

## Answers

### Design director (impeccable)
VOTE: Q1 (a), Q2 (b), Q3 (c)
- Operate mode: "a working product of its category" first; Strong/Hevy pattern is one scroll, summary over history. One nav level, no tabs inside a tab (android.md, last council).
- craft-floor bans identical icon+heading+text cards as page structure; stat tiles are that slop. Counts are one typeset line; later modules each get their own shape (chart, list, single hint).
- PRODUCT principle 3: v1 has real data for counts, so no empty slot, no invented charts. Volume only counts weighted sets.
- Q3: bench over time belongs to the exercise; Progress links into it. One chart.
SKETCH v1: top bar "Progress" / "This week: 3 sessions · 3 h 40 m · 9,850 kg" / list grouped by week, sticky headers / rows (name, date, duration, top set) / honest empty state, lime "Start a workout". v4: same top / one top-set chart for a pinned lift / "Records" row, 3 newest PRs / suggestion row ("Bench: try 82.5 kg") / same history.
RISK: modules push history below the fold. Rule: history starts in the first viewport.
VETO: (c) same-size card feed is a craft-floor ban; "All sessions" behind a card costs a tap every visit.
IDEA: stats in the sticky week headers ("Week of 26 Oct · 3 sessions · 9,850 kg").

### Product owner
VOTE: Q1 (a), Q2 (a), Q3 (c)
- v1 scope frozen; session list is real v1 content. Counts are a feature, so v2.
- Build (a) with an empty stack area above the list that becomes (c)-like cards in v2+; never ship an empty feed.
- Q3 (c): per-lift data belongs to the exercise; chart built once.
SKETCH v1: top bar / no cards / sessions by month / empty state "Finish a workout to see it here" + Start. v4: range filter, cards (This week, PRs, Bench trend, "Try +2.5 kg", Body weight), "All sessions".
RISK: v1 is just a list; tab name promises more until v2.
VETO: (b) top tabs: empty Stats tab is the dead tab we just removed.
IDEA: show v1 and v4 frames side by side per layout in Figma.

### Android architect
VOTE: Q1 (c), Q2 (b), Q3 (c)
- SessionsScreen.kt is a stub; real list is in WorkoutScreen.kt fed by getAllWorkoutSessions() (whole Flow<List>). Card feed = one LazyColumn, one item{} per card; new cards need no nav change. (b) puts a pager in a tab: nested scroll + Back pain.
- "All sessions" as its own route can move to Paging 3 later without touching the feed; (a) with years of data needs that change in place.
- Q2 (b): this-week count/volume from getWorkoutSessionsByDateRange + sets, cheap. No all-time totals in v1.
- Q3 (c): performed_exercises indexed on exerciseId; one screen, two entry points.
SKETCH v1: This week card → Recent sessions (last 5, "All sessions") → All sessions screen, sticky month headers. v4: This week → Suggestion → PRs → lift trend → body weight → Recent.
RISK: every card's query runs on tab open; each needs its own small limited query.
VETO: (d) calendar: grid at 200%/48dp, little v1 value, pushes history down.
IDEA: plan an index on workout_sessions.startedAt and SQL aggregates (weekly volume, best set) for later.

### Accessibility / gym ergonomics
VOTE: Q1 (a), Q2 (b), Q3 (c)
- One scroll with real headings is best for TalkBack heading jumps and 200% reflow; card feed grows huge at 200%, top tabs out of thumb reach.
- Each future feature = a new headed section above history; numbers text-first.
- Q2 counts as plain text, no streak.
- Q3: exercise detail is one tap from the active workout; Progress is the look-back; same screen.
SKETCH v1: "Progress" heading / "3 sessions · 2h 40m · 9,450 kg" on a lime-free surface / sticky "November 2026" headers, rows ≥72dp (name, date, duration, "5 exercises, 18 sets", chevron), whole row one target. v4: + "vs last week +1 session" / Records (3 rows) / Trends with sparkline + text "Bench top set 80 → 87.5 kg, 8 wk" / one suggestion row / History last.
RISK: by v4 history is far down; needs a "History" jump.
VETO: (d) calendar: ~46dp cells at 360dp, dot-only state, ~35 TalkBack stops before any session.
IDEA: every chart has "Show as table" with the same data.

### Beginner lifter
VOTE: Q1 (c), Q2 (b), Q3 (c)
- Cards I read in 5 s ("3 sessions this week") beat tabs or a dotty calendar.
- I want 2-3 plain counts now; an empty "stats coming soon" slot feels sad.
- Later "Bench: 30 → 37.5 kg since you started" on Progress, opening the same exercise screen.
- New cards just add; nothing moves.
SKETCH now: This week (3 sessions, 4,210 kg) / Total: 9 sessions since 12 Oct / Recent sessions (last 3) / All sessions. In a year: This week / one Suggestion / New records / 1-2 pinned trends (start and now) / Recent / All. Body weight opt-in.
RISK: feed fills into a scary dashboard; cap or hide cards.
VETO: (b) tabs look like homework; empty Stats says I have nothing.
IDEA: "Since you started" line: "First squat: 40 kg × 5. Last: 55 kg × 5."

### Veteran lifter
VOTE: Q1 (a) built to become (c), Q2 (b), Q3 (c)
- 90% of visits I check last week's session; history must be the main thing, not a card (Hevy feed, Strong tabs hide it).
- (b) splits; Stats stays empty, then forgotten.
- Counts are cheap and honest: sessions, volume, sets per muscle group.
- Trends on exercise detail; Progress shows top 3 lifts linking there.
SKETCH now: This week strip / sessions by week, sticky headers; rows: date, template, duration, top set per exercise ("Bench 100×5"), PR badge. v4: strip + e1RM chip / one horizontal row of cards (PRs, Bench trend, "add 2.5 kg") / history unchanged.
RISK: card row pushes history down; cap at one row.
VETO: (d) calendar: half a screen, says nothing about what I lifted, breaks at 200%.
IDEA: filter history by exercise ("only sessions with Squat").

## Synthesis
| Member | Q1 | Q2 | Q3 |
|---|---|---|---|
| Design (impeccable) | a | b | c |
| Product owner | a | a | c |
| Android architect | c | b | c |
| Accessibility | a | b | c |
| Beginner | c | b | c |
| Veteran | a (→c) | b | c |

Q1: 4 a, 2 c. Nobody for (b), (d), (e). Vetoes: (d) calendar ×3, (b) tabs ×2, (c) card feed ×1 (impeccable).
Q2: 5 b (counts from real data), PO alone for list only (scope).
Q3: unanimous (c): chart lives on exercise detail, Progress links to it.
Agreed rule: history must start in the first viewport, whatever grows above it.
Split: one scroll with headed sections (a) vs card feed with a separate All sessions screen (c). Middle ground several reached: (a) whose top area grows by modules, each module its own shape, never a tile grid.
Ideas to keep: stats in sticky week headers (impeccable); v1 + v4 frames side by side (PO); "Show as table" for charts (a11y); "since you started" line (beginner); filter history by exercise (veteran); index on startedAt later (architect).
Recommendation: Q1 (a) one scroll, growing top area, history in first viewport; Q2 (b) one text line of this-week counts (PO scope concern noted; owner call); Q3 (c).
Figma round: 4 layouts, each v1 + v4 side by side: A1 one scroll + summary line; A2 week headers carry the stats; C card feed + All sessions screen; H hybrid (a) with one capped module row.

Owner (2026-11-01): counts line in the first build (yes). Week strip moves to Home. Progress leans A1 v4 (chart breaks the list). Progress must be more than history; contents and history depth go to the next council (2026-11-01-progress-contents).

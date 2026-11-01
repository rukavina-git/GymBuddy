# Council 2026-10-31: History/Stats placement, next flow

## Brief
Q1. Where do History (past sessions) and Stats live?
(a) 5 tabs: Home, Workouts, Exercises, History, You; Stats inside History.
(b) 5 tabs with "Progress": stats first, history below.
(c) 4 tabs: History inside You; stats glance on Home.
(d) 4 tabs: Home holds Recent + stats glance; no tab.
(e) Category standard / v1: Workouts tab with Templates | History top tabs, Stats tab separate.
Q2. Next flow: (a) Exercises → Workouts → Home; (b) Home + Workouts → Exercises; (c) Workouts first.
Constraints: active workout locked; v1 nav Home, Workouts (Templates | Sessions), Exercises, Stats (placeholder), Settings; stats content is a future plan; M3 nav 3–5 items; no social.

## Answers (verbatim)

### Design director (impeccable)
VOTE: Q1 (a), Q2 (b)
- android.md: nav bar holds 3–5 destinations, each a frequent top-level job; PRODUCT.md says users "look back at history" outside the gym, so past sessions earn a tab. Five sits at critique.md's "≤5 top-level items" limit, within it.
- Stats content is undecided: making it the lead (b) means the first thing on the tab is empty or invented, which breaks principle 3 ("show progress honestly") and repeats today's placeholder Stats tab. History works now; stats can be added at its top later without changing the nav.
- Q2: Operate mode says to design "the product's shell first": nav, work areas, where sessions start. Q1 changes the shell, and Home hosts the live bar and the start action. Settle Home and Workouts, then fit Exercises into it.
RISK: five tabs crowd the bar; each label has to fit one line at 1.3× font scale.
VETO: (e) — templates (plans) and history (past sessions) are different jobs; one tab for both is v1's confusion, plus a dead Stats tab ("inconsistent patterns").
IDEA: put per-lift progress on the exercise detail page (bench over time, last time's numbers); it may mean a Stats tab is never needed.

### Product owner
VOTE: Q1 (a), Q2 (a)
- The core loop ends with "session lands in history", and the session list already exists in v1, so a History tab is a real place today. Stats belongs to a future plan: a reserved slot inside History, not its own tab.
- Five tabs fit M3, and the future History plan gets room without moving the nav again. (a) reverses N1 ("Stats hidden for now"); record it in the brief.
- Exercises first has the least sequencing risk: the most stable part of v1 with nothing to decide; its rows, chips and thumbnails get reused in the template editor and the active workout.
- Home last because what it shows depends on History/Stats, still open; designing it first means redoing it.
RISK: Exercises is a weak first flow to show off; little visible momentum until Workouts and Home are done.
VETO: (b) — leading with stats nobody has defined means an empty hero or invented features past the scope freeze.
IDEA: before the flow rounds, one cheap "shell" frame: all 5 tabs, top bars, live bar, an empty state per tab. Locks navigation once.

### Android architect
VOTE: Q1 (a), Q2 (a)
- History has real data today (SessionsScreen, sessions in Room), so the tab is never empty; stats can be added later as a header or segment. (b) leads with stats that don't exist; (e) keeps a placeholder. M3: every destination should have real content.
- Cheap in code: lift SessionsScreen out of CombinedWorkoutsScreen's TabRow into its own route, swap Statistics for History in BottomNavItem, rename Settings to You. About a day of work.
- Exercises first builds shared pieces bottom-up (exercise row, C1 thumbnail, C4 detail), reused by the template picker, active workout and Home; ExerciseScreen stands alone, so it's a low-risk first code step.
RISK: nav and You designed late; Exercises frames could assume a shell that Home/Workouts later breaks — lock one shell frame up front.
VETO: (e) — top tabs inside a bottom tab plus an empty stats tab is what's broken in v1; history split across two places.
IDEA: per-exercise history (last time, top set, small chart) on exercise detail; leave a slot in the C4 layout now.

### Accessibility and ergonomics
VOTE: Q1 (a), Q2 (b)
- A dedicated History tab is one thumb tap away and easy to find; (c) hides it in You, (e) uses top tabs out of one-handed reach plus a second nav level.
- History has real content today; stats can become a segment or header later without renaming the tab.
- Short one-word labels cope better at 200% font than "Progress"; with 5 tabs each slot is ~72dp on a 360dp phone.
- Q2: Home and Workouts are where sessions start, on the gym floor; get that path's targets, reach and TalkBack order right first. Exercises is mostly used at rest.
RISK: "Exercises"/"Workouts" may cut off or wrap at 200% in a 5-slot bar, worse with the live bar above.
VETO: (e) — top tabs far from the thumb, two-level nav, empty Stats tab.
IDEA: every round includes a 200% font, 360dp frame with live bar + nav and a TalkBack focus-order annotation; if labels fail, show only the selected tab's label.

### Beginner lifter
VOTE: Q1 (b), Q2 (b)
- I open the app for "what do I do today?" Home has to answer that in one tap ("Today: Pull Day, 5 exercises, Start").
- "Am I getting anywhere" is my other question, so I want a tab called Progress; I'd never look under "History" or "You". Good news first ("Bench up 5 kg since week 1"), old sessions under it.
- I don't care much about building plans; help me get mine in once.
- The how-to matters, but in the moment, while doing the exercise, not from browsing a library.
RISK: Progress is mostly empty for the first weeks; a blank chart makes me feel I'm failing. Needs a friendly empty state.
VETO: (e) — tabs inside tabs plus split Stats is the confusing app I quit.
IDEA: a "How do I do this?" link on every exercise during the workout and on its template card: photo + 3 steps, straight back to my set.

### Veteran lifter
VOTE: Q1 (a), Q2 (b)
- I check History all the time ("what did I hit last Tuesday?"); Strong got this right.
- Stats content isn't decided; a Progress tab leading with charts you don't have is an empty showcase. Put stats in History and grow it later.
- Every session starts at Home/Workouts; if starting my planned day takes more than one tap, I'm gone. Fix what I open 4×/week before the library I open monthly.
- Logging is done, so Home + Workouts complete the core loop: start, log, done.
RISK: History fills up with stats and the session list gets slow to scan; the list must stay the default view.
VETO: (d) — past sessions behind "See all" on a feed-style Home; an extra tap every time.
IDEA: long-press any exercise anywhere to open a quick sheet: last 3 sessions, top set, e1RM PR. Offline, no navigation.

## Synthesis (chair)
Votes Q1: (a) 5 — design, PO, architect, a11y, veteran; (b) 1 — beginner. (e) vetoed by 4, (b) by PO, (d) by veteran.
Votes Q2: (b) 4 — design, a11y, beginner, veteran; (a) 2 — PO, architect (both: bottom-up reuse, lower redo risk).
Shared ideas: lock a shell frame first (PO, architect; matches impeccable's "shell first"); per-exercise progress on exercise detail (design, architect, veteran); 200% font + TalkBack frame every round (a11y); friendly empty states (beginner).
Recommendation: Q1 (a) History tab, list as default view, stats slot at its top for the future plan, per-exercise progress slot on exercise detail. Q2: shell frame first (5 tabs, top bars, live bar, empty state per tab, 200% font check), then Home + Workouts, then Exercises. The shell answers the PO/architect redo risk; impeccable and the users favour starting where sessions start.
Owner only: tab name History vs Progress (beginner's point), and whether to accept the shell-first hybrid.

Owner (2026-11-01): Q1 (a), tab named **Progress**, built to grow (stats, graphs, suggestions in v3+); layouts go to a new council + Figma round. Q2: shell frame, then Home + Workouts, then Exercises.

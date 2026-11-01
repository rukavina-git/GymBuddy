# Plan 3: Figma foundation (redesign)

## Why
The v1 UI is a functional skeleton. On 2026-10-31 Plan 3 became a full
UI/UX redesign: direction "Soft Dark" (design-brief.md). Every screen
is designed in Figma first; Plan 4 builds it in Compose. Plan 4 also
needs a Login "session expired" frame (4.2a).

## Roles
- **Claude, product owner and architect.** Writes each surface brief
  from PRODUCT.md (task, data, states, constraints), guards scope and
  invariants, checks Compose/M3 feasibility, and builds frames in
  Figma. Doesn't decide the look alone.
- **impeccable, the UI team.** Decides how things look and work:
  directions, layout, type, colour, copy, motion. Its critique, audit
  and finish review gate every round. When it and Claude disagree on
  design, impeccable wins unless it breaks PRODUCT.md or an invariant.
- **Owner, final taste call.** Picks between options, kills anything
  that looks like AI-made UI, and says "more ideas" to reopen a round.

## Round loop (every surface, steps 04–07)
1. **Brief.** Claude writes the surface brief. impeccable `shape`
   turns it into UX questions and a plan.
2. **Wide.** impeccable new-work (`concept-seed`) deals 3+
   materially different options plus one plain "category standard".
   Claude builds them on a Figma board. Owner picks one or more.
3. **Refine.** impeccable `critique` (two subagents) on Figma
   screenshots, then the fixes it names (`layout`, `typeset`,
   `clarify`, `bolder`/`quieter`). 2–3 variants with real data and
   edge cases. Owner picks.
4. **Harden.** impeccable `harden` + `onboard` for empty, loading,
   error and offline states; `audit` (native) for contrast, 48dp
   targets and font scaling.
5. **Polish.** impeccable `polish`, then the
   `impeccable-finish-reviewer` agent. Owner signs off.

impeccable checks every round, not just the last: accessibility,
ease of use, style, and motion where it helps (`animate`).

Gates: no next round without the owner's pick. No critique P0/P1 left
open when the owner sees a round. Read `craft-floor.md` before any
Figma edit. "More ideas" sends the surface back to 2.

## Steps
1. (done, roadmap 3.1) Figma file + MCP. Figma Console MCP since
   2026-10-31 (no call limit; needs Desktop + Bridge plugin).
2. World: a banned list,
   impeccable's direction round with Soft Dark as the pinned brief,
   direction contract. PRODUCT.md updated to the chosen palette.
3. Tokens: colour, type, space, radius, motion as Figma variables
   (Dark mode, Light slot later) + `design/tokens.json`.
4. Brand and icons: app icon, wordmark, 12 gym glyphs.
5. Components: M3-based library on the tokens (buttons, set row,
   keypad sheet, rest card, live bar, cards, nav bar, sheets).
6. Screens, one sub-step per flow: 6a onboarding + auth (incl.
   session expired), 6b home + templates, 6c active workout, 6d
   exercise library + detail, 6e You + settings, 6f cross-flow
   states (empty, error, offline, sync).
7. Motion and haptics spec (impeccable `animate`).
8. DESIGN.md (impeccable documenter) and the Plan 4 handoff: code
   steps that build the redesign, each linked to its frames.

## Invariants
- No app code changes. Code is Plan 4.
- No `api/openapi.yaml` or Room schema change.
- v1 features stay frozen. New interaction designs from the brief
  (keypad, live bar, lock-screen timer, keep screen on, swipe + undo,
  onboarding) are in scope as design only. History features (last-time
  numbers, summaries, stats) are a later plan. Exception (owner,
  2026-11-01): the Progress tab replaces Sessions + Stats, with a counts
  line, one lift chart and All sessions search (design-brief "Round
  log: Progress tab"). A Stats hub is v2+.
- Material 3 governs structure, navigation and system Back; the brand
  shows through colour roles, type, shape and motion.
- WCAG AA contrast, 48dp+ in-workout targets, system font scaling.
- Figma is the source; `design/tokens.json` is its committed copy.
- Every round uses real content (real exercise names, weights,
  long names, 0 sets), never lorem ipsum.
- Impeccable working files (`.impeccable/`) stay out of git, except
  what a step names.

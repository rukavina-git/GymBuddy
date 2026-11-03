# Plan 7: redesign build (Compose)

## Why
The v1 UI is a functional skeleton. Plan 3 locked a full Soft Dark
redesign in Figma; this plan builds it in Compose on top of the v1
features plus what Plan 6 added (roadmap-details.md, Plan 7).

## Read first
`design/DESIGN.md` (handoff), then the design/ file the step names.
Source of truth, highest first: Figma "01 System" + "03 Locked
designs" → design/ files → design-brief.md round logs. Never build
from "02 Rounds".

## Scope
1. Theme: tokens.json → colour, type (tnum), shape, motion, haptics.
2. Icons + component library (01 System).
3. App frame: 5-tab nav, top bars, predictive back.
4. Onboarding + auth (UI for 6.2, 6.3).
5. Home + Templates (Workouts tab, plans from 6.6).
6. Active workout (keypad, live bar, swipe + undo, rest).
7. Exercise page + create.
8. Progress tab (replaces Sessions + Stats).
9. Session detail.
10. You + Settings (UI for 6.4, 6.5, 6.7).
11. Cross-flow states: error, offline, empty, sync.
12. (me) Device pass on the current backend, CI green, PR develop → main.

1–3 come first, in order. Step files are written one at a time, each
after the previous step lands (the first /step on a missing file drafts
it for approval).

Out of scope: light mode (v2), history features (last-time numbers,
summaries, weekly schedule), staging/prod (Plan 5), CI gates (8.1).

## Invariants
- Every UI step reads its linked Figma frame through the MCP before
  code. No link or no MCP → stop.
- Old screens are replaced, not restyled. A step deletes the screens it
  replaces; no dead UI left behind.
- Each step leaves the app usable end to end (old screens may look off
  under the new theme until replaced; they must not crash or lose data).
- No data, sync or domain changes unless a step says so. Sync rules in
  android/CLAUDE.md and Plan 6 owner isolation (ADR 0014) hold.
- `api/openapi.yaml` and the Room schema: no changes expected. If one
  is needed, say it out loud and stop for approval.
- Colours, type, shapes, spacing and motion come from `ui/theme/`
  tokens. No raw hex or dp in screens, except DESIGN.md §4 raw sizes.
- Lime is only "you can press this"; cobalt only "you are here".
- Dark only. No dynamic colour.
- Every new dependency, font or icon: commercial-friendly licence,
  recorded in DESIGN.md §licences (and 9.1 later).
- Tests: ViewModel/unit tests for new logic; Compose UI tests where a
  step adds interaction logic (swipe/undo, keypad). No manual device
  checks until 7.12 (owner).
- Branch: develop (PR #6 merged 2026-11-03).

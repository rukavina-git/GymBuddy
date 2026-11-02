# 08 DESIGN.md + Plan 4 handoff

## Problem
The redesign is spread across Figma ("01 System", "03 Locked
designs"), `design/` (colors, do-dont, tokens.json, icons, components,
motion) and design-brief.md round logs. Plan 4 in `docs/roadmap.md`
has no steps that build the redesign at all, only release and data
work. Open leftovers from 03–07 (raw sizes, stray layers, the "No ads"
copy, crossfade wording, reviewer notes from 07) have no home.

## Change
1. **DESIGN.md.** The `impeccable-documenter` agent writes
   `design/DESIGN.md` (+ its sidecar, if it makes one, under
   `design/`) from what shipped: Figma "01 System" and "03 Locked
   designs", plus the `design/` files. Not from intentions. It links
   out instead of copying: colors.md, do-dont.md, tokens.json,
   icons.md, components.md, motion.md. One page a Compose dev reads
   first. Where sources disagree, Figma wins and the file is fixed.
2. **Cleanup of known drift.** Fix wording only, no frame redesigns:
   - design-brief.md (l.91, 141, 206-208): crossfade -> instant cut.
     tokens.json `short4` description stays (generated); note it in
     DESIGN.md instead.
   - Delete the two stray layers at 01 System root (151:3220,
     151:4134).
   - "No ads" on R16-6b Ready: owner decides (keep, reword, cut).
   - Raw sizes from 05 (72/40 buttons, 52 InfoRow, 68 ListRow, 64
     stepper, 356 dialog): list in DESIGN.md as "use as drawn" or
     add tokens. Adding tokens means re-export tokens.json.
3. **Plan 4 handoff.** Add "build the redesign" steps to roadmap.md
   Plan 4 and roadmap-details.md (no plan folder yet), one per flow,
   each linked to its
   Figma section (node link) and design/ files. Proposed order:
   theme + tokens in Compose (Color, Type, Shape, motion, tnum),
   icons + components, app frame + nav, onboarding + auth (ties to
   4.2a/4.2c), Home + Templates, Active workout, Exercise page +
   create, Session detail, Progress, You + Settings, cross-flow
   states. Each says which existing screens it replaces
   (e.g. SessionsScreen + StatisticsScreen -> Progress) and carries
   the matching 07 reviewer notes and 03-05 gotchas.
4. **Owner check.** Owner reads DESIGN.md and the new Plan 4 steps,
   and picks the order.

## Tests
None (no code). Claude checks: every locked section on "03 Locked
designs" is linked by exactly one Plan 4 step; every Gotcha from
progress.md marked "Fix in 08 or Plan 4" is fixed here or carried
into a step; DESIGN.md has no value that isn't in Figma or design/.

## Out of scope
Compose code, Plan 4 step files (written when Plan 4 starts), new
screens or rounds, light mode, iOS, `api/openapi.yaml` or Room
changes, re-ordering non-design Plan 4 steps (4.0-4.6).

## Done when
- `design/DESIGN.md` committed.
- Drift fixes from Change 2 done; owner's "No ads" call applied.
- roadmap.md + roadmap-details.md Plan 4 have the redesign steps,
  each with a Figma link; owner okayed the order.
- No finish-reviewer P0/P1 open.

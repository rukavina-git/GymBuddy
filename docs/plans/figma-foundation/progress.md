# Progress

- [x] 01 Figma file + MCP connected (roadmap 3.1, done before this plan)
- [ ] 02 World, direction round (steps/02-world.md)
- [ ] 03 Tokens as Figma variables + design/tokens.json
- [ ] 04 Brand and icons
- [ ] 05 Components
- [ ] 06a Screens: onboarding + auth (incl. session expired)
- [ ] 06b Screens: home + templates
- [ ] 06c Screens: active workout
- [ ] 06d Screens: exercise library + detail
- [ ] 06e Screens: You + settings
- [ ] 06f Screens: cross-flow states (empty, error, offline, sync)
- [ ] 07 Motion and haptics spec
- [ ] 08 DESIGN.md + Plan 4 handoff

## Deviations
- 2026-10-31: plan rewritten from "mirror v1 UI" to a full redesign
  (Soft Dark). Old step 02 (mirror v1 tokens) dropped.
- 2026-10-31: owner skipped outside references and went straight to
  rounds on the core screen. Active workout (06c) was designed and
  locked during step 02 (5 rounds, page "03 Active workout (locked)").
  Decisions in design-brief.md "Round log".

## Gotchas
- From phase5-data-safety: debug builds use the hosted backend; use
  `-Pgymbuddy.apiBaseUrl=http://10.0.2.2:8080/` for a local one.
- From phase5-data-safety: Plan 4 has two new steps, 4.2a (auth gate,
  needs a Login "session expired" frame from 06a) and 4.2b
  (durable push).
- Figma Console MCP needs Figma Desktop open with the Desktop Bridge
  plugin running. Figma Starter may allow only 1 variable mode per
  collection; check before step 03 adds a Light slot.

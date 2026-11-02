# Progress

- [x] 01 Figma file + MCP connected (roadmap 3.1, done before this plan)
- [x] 02 Direction: Soft Dark locked 2026-10-31. Owner: lime = press,
      cobalt = you are here, #121418 ground; dark is the default and
      only mode for v1 (2026-11-02). No new direction round.
- [ ] 02b Banned list in design-brief.md + PRODUCT.md palette
- [ ] 03 Tokens as Figma variables + design/tokens.json (dark only)
- [ ] 04 Brand and icons (incl. new animal logo, owner 2026-11-02)
- [ ] 05 Components
- [x] 06 App frame (locked 2026-11-01)
- [x] 06a Onboarding + auth incl. session expired (locked 2026-11-02)
- [x] 06b Home (2026-11-01) + Templates (2026-11-01)
- [x] 06c Active workout (locked 2026-10-31)
- [x] 06d Exercise page + create (locked 2026-11-02)
- [x] 06e You + Settings (locked 2026-11-02)
- [x] 06g Progress tab (locked 2026-11-01)
- [x] 06h Session detail (locked 2026-11-02)
- [ ] 06f Cross-flow states: error, offline, empty, sync (next)
- [ ] 07 Motion and haptics spec
- [ ] 08 DESIGN.md + Plan 4 handoff

## Deviations
- 2026-11-02: screens (06) were designed before tokens (03), icons
  (04) and components (05); those now follow from the locked frames.
- 2026-10-31: plan rewritten from "mirror v1 UI" to a full redesign
  (Soft Dark). Old step 02 (mirror v1 tokens) dropped.
- 2026-10-31: owner skipped outside references and went straight to
  rounds on the core screen. Active workout (06c) was designed and
  locked during step 02 (5 rounds, page "03 Active workout (locked)").
  Decisions in design-brief.md "Round log".

## Gotchas
- 2026-11-02: screens were designed in rounds outside /step, so this
  file and roadmap.md fell behind. Tick both whenever a flow locks.
  Final frames: Figma "03 Locked designs"; decisions: design-brief.md
  round logs.
- From phase5-data-safety: debug builds use the hosted backend; use
  `-Pgymbuddy.apiBaseUrl=http://10.0.2.2:8080/` for a local one.
- From phase5-data-safety: Plan 4 has two new steps, 4.2a (auth gate,
  needs a Login "session expired" frame from 06a) and 4.2b
  (durable push).
- Figma Console MCP needs Figma Desktop open with the Desktop Bridge
  plugin running. Figma Starter may allow only 1 variable mode per
  collection; check before step 03 adds a Light slot.

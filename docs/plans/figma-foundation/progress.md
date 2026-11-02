# Progress

- [x] 01 Figma file + MCP connected (roadmap 3.1, done before this plan)
- [x] 02 Direction: Soft Dark locked 2026-10-31. Owner: lime = press,
      cobalt = you are here, #121418 ground; dark is the default and
      only mode for v1 (2026-11-02). No new direction round.
- [x] 02b Colours file (design/colors.md) + Do/Don't list
      (design/do-dont.md) + PRODUCT.md palette (7c1a3b6)
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
- [x] 06f Cross-flow states: error, offline, empty, sync (locked 2026-11-02)
- [ ] 07 Motion and haptics spec
- [ ] 08 DESIGN.md + Plan 4 handoff

## Deviations
- 2026-11-02 (02b): owner renamed "banned list" to a living Do/Don't
  list in design/do-dont.md (not design-brief.md "## Banned"). Added
  design/colors.md (every colour, pulled from Figma "03 Locked
  designs") instead of a direction-contract summary. Owner picked all
  six colour merges in colors.md "Decided" (impeccable agrees).
- 2026-11-02: screens (06) were designed before tokens (03), icons
  (04) and components (05); those now follow from the locked frames.
- 2026-10-31: plan rewritten from "mirror v1 UI" to a full redesign
  (Soft Dark). Old step 02 (mirror v1 tokens) dropped.
- 2026-10-31: owner skipped outside references and went straight to
  rounds on the core screen. Active workout (06c) was designed and
  locked during step 02 (5 rounds, page "03 Active workout (locked)").
  Decisions in design-brief.md "Round log".

## Gotchas
- From 02b: Figma frames still use the old values for the six colour
  decisions in design/colors.md (#FF8A80, cobalt 16%/30%, scrims
  55%/35%, #3A3F4B, outline and disabled greys as text). Step 03 must
  apply them while binding frames to variables. Google button,
  Android system UI and board chrome colours are not tokens.
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

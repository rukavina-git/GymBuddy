# Progress

- [x] 01 Figma file + MCP connected (roadmap 3.1, done before this plan)
- [x] 02 Direction: Soft Dark locked 2026-10-31. Owner: lime = press,
      cobalt = you are here, #121418 ground; dark is the default and
      only mode for v1 (2026-11-02). No new direction round.
- [x] 02b Colours file (design/colors.md) + Do/Don't list
      (design/do-dont.md) + PRODUCT.md palette (0f469ac)
- [x] 03 Tokens as Figma variables + design/tokens.json (dark only) (dca0550)
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
- 2026-11-02 (03): scales came from impeccable typeset + layout
  (two subagents); owner accepted all and every visible change:
  titles 26/30 -> 28, dialog titles 24 SemiBold, workout buttons
  17 Bold -> 16 SemiBold, card gaps 20 -> 24 (17 frames; skipped
  R16-7 form fields and R11-4 list), radius 22->20, 18->16, 14->12,
  6->8, keypad keys 52/48 -> 56 (sheet grows upward).
- 03: two custom type roles beside M3: stat-value (22 Bold) and tag
  (12 Bold); M3 has no fit for card numbers or PR tags.
- 03: extra collections beyond the step file: Size (tap target,
  control, key, row, chip, icon) and Stroke. Type is also a variable
  collection (size, weight, line height, tracking per role) so it
  lands in tokens.json; 16 text styles bind to it.
- 03: no space tokens are bound on frames (no auto layout there);
  space is used by the Tokens card and by components from 05.
- 03: the "Tokens" card is a Section below Rules on 01 System;
  Light-mode note there now says Starter allows one mode.
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
- From 03: Starter allows 1 variable mode (tested: "Limited to 1
  modes only"). Light needs a paid plan or a separate collection.
- From 03: not bound on purpose: status-bar "9:41", drawn keyboard,
  wordmark texts, 200% demo frames, R5-A motion frames, Lock screen,
  Live bar spec frame, Google/Android/board colours, icon geometry
  radii 1-3 and strokes 1.33/1.67/2.5/2.75 (scaled or chevron icons).
  Step 05 icons/components should snap these to stroke.icon 2.
- From 03: tabular figures are in text-style descriptions only
  (display-*, title-small, body-small, label-small, stat-value);
  Figma can't store tnum. Plan 4 sets fontFeatureSettings "tnum".
- From 03: tokens.json is generated (figma_export_tokens, DTCG,
  hex8). Don't hand-edit it; motion.press-scale reads 0.97000003
  (Figma float32). Durations are plain ms numbers.
- From 03: radius.full is 999 in Figma; in Compose use CircleShape.
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
  plugin running.

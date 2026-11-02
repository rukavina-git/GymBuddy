# Progress

- [x] 01 Figma file + MCP connected (roadmap 3.1, done before this plan)
- [x] 02 Direction: Soft Dark locked 2026-10-31. Owner: lime = press,
      cobalt = you are here, #121418 ground; dark is the default and
      only mode for v1 (2026-11-02). No new direction round.
- [x] 02b Colours file (design/colors.md) + Do/Don't list
      (design/do-dont.md) + PRODUCT.md palette (0f469ac)
- [x] 03 Tokens as Figma variables + design/tokens.json (dark only) (dca0550)
- [x] 04 Brand and icons: own ECG logo, 12 glyphs, tab + Material UI icons (2f366fe)
- [x] 05 Components: 35 on 01 System, 627 instances on locked frames (711fb05)
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
- 2026-11-02 (05): 14 merges, not 8. D1-D9 on the Round 19 board;
  D10-D14 (text field fill, keypad footer 60->56, snackbar 52->56,
  upcoming set row 52->48, nav icons) found while building; owner
  approved "if impeccable approves", impeccable approved all. D3
  amended: sheets keep surface fill (colors.md wording is wrong).
- 05: not every repeated part is an instance. Swapped: nav bar, live
  bar, buttons, chips, switch, search, set row, stepper, keypad key,
  session/template/info rows, stat tile, snackbar (627). Containers
  with per-screen content stay drawn but token-bound, merges applied
  by hand: sheets, dialogs, menus, set card, keypad sheet, week strip,
  top bars, text fields, list rows. Skipped on purpose: filter chip
  with x, mid-swipe set, rows with extra texts, PR glyph row, 200% frames.
- 05: library is 35 components, more than the step's start list
  (ValuePanel, WeekDay, KeypadKey, MenuItem, RestLine split out).
- 05: logo leftovers removed: "Round 18 · logo" section deleted from
  03 Locked designs; Round 19 board deleted after the merges.
- 05: impeccable critique/audit ran inline (native: Assessment A only).
  Fix applied: fixed heights -> min heights so rows grow at 200%.
- 2026-11-02 (04): logo is not an animal. Owner rejected the duck,
  meerkat, raccoon and 17 symbols; picked an ECG line, then asked for
  our own drawing (logo E, stroke 5/48, impeccable picked medium over
  thin). No licence needed. Old G plate replaced on 5 sign-in screens.
- 04: glyph "muscle" swapped for jump-rope (hand-drawn arm didn't
  read). Cable redrawn (first draft looked like a gallows).
- 04: library has more than the step file: 35 Material Rounded UI icons
  (Apache 2.0) and 10 tab icons (own, outline + filled). 1,199 ad hoc
  icon frames on "03 Locked designs" are now instances.
- 04: visible changes on locked frames beyond the logo: UI icons are
  Material Rounded shapes (slightly different from the hand-drawn
  ones), the wordmark dot is now lime everywhere (W2), drift merged
  (3 chevrons, 3 checks, 4 "more", 2 persons, 2 alerts -> one each).
- 04: impeccable critique ran on the logo only; glyphs got no separate
  critique/audit round (owner reviewed them on the boards).
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
- From 05: a paint bound to a variable must also carry the resolved
  colour (setBoundVariableForPaint on black renders dark until
  refreshed). Resolve aliases first.
- From 05: swapComponent / icon INSTANCE_SWAP resets the icon tint;
  re-tint vectors to the label colour after.
- From 05: filled tab icons have cut-out detail; never tint them, use
  fresh instances.
- From 05: matching flat frames by bounds must respect z-order: only
  take layers above the anchor. Early runs (Home, App frame) deleted
  hidden layers behind bars (invisible) and 3 sheet buttons over nav
  bars (restored from version "Before step 05 swap").
- From 05: tokens.json not re-exported; components use some raw sizes
  (72/40 buttons, 52 InfoRow, 68 ListRow, 64 stepper vs size/stepper-width
  60, dialog 356). Reconcile in 08 or Plan 4.
- From 05: Figma versions "Before step 05 swap" and "Step 05 components".
- From 04: NEVER call figma.triggerUndo() from the plugin. The Desktop
  Bridge session is one undo group: one undo reverted every plugin
  edit since 2026-10-31. Recovery was owner Shift+Cmd+Z, then restoring
  version "5:49 pm" and re-running the step's scripts.
- From 04: before replacing a node, check its parent. The "G" text sat
  directly on the screen frame; "replace parent" replaced 5 screens.
- From 04: icon frames are matched by geometry signature (bbox + path);
  new ad hoc icons drawn on locked frames won't be instances. Step 05
  components must use the library instances only.
- From 04: "Lock screen" ring (53:3478) and the Google G stay raw.
- From 04: R16-6b Ready says "No streaks, no feed, no ads." "No ads"
  conflicts with the monetization rule; owner to decide (not changed).
- From 04: Material icons are fills; custom ones are strokes. To tint
  an instance, set fills on ui/* and strokes on glyph/* and tab/*.
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

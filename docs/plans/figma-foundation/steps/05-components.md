# 05 Components

## Problem
Every locked screen (06–06h) is drawn by hand: buttons, set rows,
cards, sheets and the nav bar are loose frames copied between
screens. They drift (sizes, paddings, states) and Plan 4 has no
single list of Compose components to build. Tokens (03) and icons
(04) exist; nothing is built on them yet.

## Change
1. **Inventory.** Claude reads "03 Locked designs" (Figma Console
   MCP) and lists every repeated UI part: name, where used, variants
   seen (size, state, with/without icon), and drift between copies.
   Start list: buttons (primary lime, secondary, text, danger, icon),
   set row, keypad sheet, rest card, live bar, cards (workout,
   template, exercise, stat), list row, chip, text field, top bar,
   nav bar, bottom sheet, dialog, snackbar (undo), banners
   (offline/sync/error), empty state. Each maps to its M3 component
   or is marked custom. Owner okays the list and any drift merges
   before building.
2. **Build on "01 System".** One component set per part, auto layout,
   bound to tokens only (colour, space, radius, size, type styles),
   icons as library instances (step 04 Gotchas). Variants via
   properties: state (enabled, pressed, disabled, focused, error),
   size, icon on/off, text. Names match the planned Compose names
   (e.g. `Button/Primary`, `SetRow`).
3. **Check.** impeccable `critique` + `audit` (native) on the
   library: 48dp in-workout targets, AA contrast in every state,
   200% font scale doesn't break layouts. Fix P0/P1 before the owner
   sees it.
4. **Swap into "03 Locked designs".** Replace the hand-drawn copies
   with instances. Screenshot each flow before and after; only the
   owner-okayed drift merges may show.
5. **Write-up.** Short `design/components.md`: component → M3 base or
   custom, variants, where used. Plan 4 builds from it.

## Tests
None (no code). Claude checks: no raw hex/size on components; every
repeated part on "03 Locked designs" is an instance; every state has
AA contrast; in-workout targets are 48dp+.

## Out of scope
Motion and haptics (07; components get static pressed states only),
new screens or features, light mode, Compose code (Plan 4),
DESIGN.md (08).

## Done when
- Owner okayed the inventory and drift merges.
- Component sets exist on "01 System", bound to tokens.
- Locked frames use instances; before/after screenshots match except
  the okayed merges.
- No critique/audit P0/P1 open.
- `design/components.md` committed.

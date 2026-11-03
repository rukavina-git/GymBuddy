# 02 Icons + components

## Problem
Screens from 7.3 on are built from the "01 System" library, but none of
it exists in Compose: no own icons (only `material-icons-extended`),
and `ui/components/` holds v1 parts (wheels, old snackbar, header).
Roadmap 7.2. **No spec change, no Room schema change.**
Figma: Components https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=151-3214,
Icons https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=137-26976.
Specs: design/components.md, icons.md, DESIGN.md §4–6, §9.

## Owner decisions: 1A, 2A, 3A (2026-11-03)
1. **Icons into Android**
   - **A (recommended): VectorDrawable XML** in `res/drawable`
     (`ic_ui_*`, `ic_glyph_*`, `ic_tab_*`, `brand_*`), converted once
     from design/icons + design/brand with `svg2vectordrawable` (MIT,
     run via npx, not a build dependency). Typed access through an
     `AppIcons` object. A unit test checks every SVG has a drawable.
   - **B: ImageVector Kotlin files** (one per icon). No XML, but long
     generated path code in the source tree.
2. **Size of the step**
   - **A (recommended): split in two.** 02a = icons + general parts
     (buttons, icon button, chip, switch, segmented, text/search
     field, list/info/template rows, session card, banner, empty
     state, snackbar, dialog, sheet, menu, top bar, nav bar).
     02b = workout and data parts (set row, rest line, value panel,
     stepper key, set card, keypad key/keypad, live bar, stat tile,
     week strip). Both land before 7.3.
   - **B: one step,** all 35 components. One very large diff and review.
3. **Old ui/components/**
   - **A (recommended): delete only unused parts now**
     (`AppSnackBar`, `SearchDialog`). The rest go with the screen step
     that replaces their last caller (invariant: no dead UI).
     `material-icons-extended` is removed when its last use goes (7.11).
   - **B: migrate old screens** to the new parts now. Touches ~30 v1
     files that later steps delete anyway.

## Change
- 02a:
  - `res/drawable/`: 35 UI, 12 glyph, 10 tab, brand mark + wordmark.
    Material UI icons tint the fill; glyphs and tab outlines the
    stroke; filled tab icons are never tinted (`Color.Unspecified`).
  - `ui/theme/AppIcons.kt`: `AppIcons.Ui.Back` etc. → `@DrawableRes`.
  - `ui/design/` (new package, one file per family): `Buttons.kt`
    (Primary 72/56/40, Secondary 56/40, Outline 56/40, Text, Danger,
    IconButton Lime/Raised/High/Plain), `Chip.kt`, `Switch.kt` (cobalt
    on), `Segmented.kt`, `TextFields.kt` (TextField + SearchField),
    `Rows.kt` (ListRow 68, InfoRow 52, TemplateRow, SessionCard),
    `Feedback.kt` (Banner, EmptyState, Snackbar 56), `Overlays.kt`
    (Dialog 356, BottomSheet with M3 motion, Menu/MenuItem),
    `TopBar.kt` (Back / Large / Workout), `NavBar.kt` (5 tabs, cobalt
    pill, filled icon when selected).
  - Heights are minimums, 48dp tap targets, 2dp cobalt focus ring,
    colours only from `ui/theme/`. `@Preview` per component.
  - Delete `components/AppSnackBar.kt`, `components/SearchDialog.kt`.
  - DESIGN.md §10: icons row notes the conversion tool.
- 02b: written after 02a lands.

## Tests
- Icon parity: every SVG in design/icons (+ mark, wordmark) has a
  drawable, no extra `ic_*` drawables.
- Compose UI tests (Robolectric) for parts with logic: NavBar
  selection + filled icon, SearchField clear ×, Chip selected state
  semantics, Button disabled not clickable, rows reach 48dp targets.
- Existing unit tests stay green.

## Out of scope
Using the parts in screens (7.3+). Swipe-to-delete and keypad input
logic (7.6). Light mode. Drag reorder library.

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

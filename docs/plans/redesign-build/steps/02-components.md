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

---

# 02b Workout + data components

## Problem
02a landed the general parts. The workout and data parts from
"01 System" are still missing, and 7.5, 7.6, 7.8 and 7.9 build from
them. Roadmap 7.2 (ticked when 02b lands). **No spec change, no Room
schema change.**
Figma: Components https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=151-3214
(SetRow, RestLine, ValuePanel, StepperKey, SetCard, KeypadKey, Keypad,
LiveBar, StatTile, WeekDay/WeekStrip). Active workout frame for
context: node 67:6561.
Specs: design/components.md, DESIGN.md §4 (StepperKey 64×56), §5 rest,
motion.md rows 1, 5, 6 and haptics table.

## Change
- `ui/design/Workout.kt`:
  - `SetRow` Done / Upcoming (48 min, D13) · set label · value text;
    radius 12 (D8). Sets/reps/kg biggest; no swipe yet (7.6).
  - `RestLine` value only, quieter type/colour than SetRow.
  - `ValuePanel` Default / Editing (lime line) · number (tnum) · unit;
    clickable, `onClick`.
  - `StepperKey` Minus / Plus · enabled · 64×56, `pressScale`,
    `KEYBOARD_TAP`-style haptic via `rememberHaptics()`.
  - `SetCard` title + two ValuePanel slots + StepperKeys, cobalt ring.
- `ui/design/Keypad.kt`:
  - `KeypadKey` Digit / Decimal / Backspace · 56 high · press scale,
    keyboard-tap haptic.
  - `Keypad` content (label, value, unit, 4×3 grid, Cancel + Next 56,
    D11) as a stateless composable emitting `KeypadInput` events
    (Digit(n), Decimal, Backspace, Next, Cancel). Sheet wrapping uses
    02a `AppBottomSheet`. Parsing/validation stays in 7.6.
- `ui/design/Data.kt`:
  - `LiveBar` title · subtitle · lime progress line (0..1), clickable.
  - `StatTile` value (`statValue`) · label · 72 high (D7).
  - `WeekDay` Rest / Done / Today / TodayDone + `WeekStrip` (7 days).
- Previews for each in `DesignPreviews.kt`. Tokens only; raw sizes only
  from DESIGN.md §4.
- No old `ui/components/` deleted here (NumberPicker goes with 7.6).

## Tests
- Compose UI tests (Robolectric), added to DesignComponentsTest or a
  new WorkoutComponentsTest:
  - Keypad: each key emits the right `KeypadInput`; Next/Cancel too.
  - StepperKey: click calls onClick; disabled not clickable; 48dp+.
  - ValuePanel: editing state exposed in semantics (selected).
  - WeekDay: each state has a distinct content description.
  - LiveBar: progress semantics (ProgressBarRangeInfo) and click.
- Existing tests stay green.

## Out of scope
Using the parts in screens (7.3+). Swipe-to-delete, undo, keypad value
parsing, the "set lands" animation (7.6). Charts (7.8).

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

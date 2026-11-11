# 01 Theme

## Problem
`ui/theme/` is the old light blue theme with `FontFamily.Default`.
Every later step needs the Soft Dark tokens in Compose first.
Roadmap 7.1. **No spec change, no Room schema change.**
Figma: Tokens https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=124-26794,
Motion https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=153-8784.
Specs: design/DESIGN.md §3, colors.md, motion.md.

## Owner decisions: 1A, 2A, 3A (2026-11-03)
1. **How tokens get into Kotlin**
   - **A (recommended): hand-copied Kotlin + a unit test** that parses
     `design/tokens.json` and asserts every colour, type size, radius,
     space and motion value matches the Kotlin constants. Readable
     code, drift fails CI.
   - **B: Gradle task generating Kotlin** from tokens.json at build
     time. No drift possible, but generated code and one more build
     step to maintain.
2. **Figtree**
   - **A (recommended): bundle the static TTFs** (Regular, Medium,
     SemiBold, Bold) in `res/font`, from the Google Fonts GitHub repo,
     OFL 1.1 text alongside. Offline-first, no Play Services font
     provider.
   - **B: downloadable fonts** (GMS provider). Smaller APK, but falls
     back to Roboto offline/first launch.
3. **Old screens under the new theme**
   - **A (recommended): switch the app to the dark theme now.** Old
     screens go dark and may look off until replaced; fix only
     unreadable text (hardcoded colours, ~13 spots).
   - **B: keep the old theme live,** new theme behind a flag until 7.3.

## Change
- **Color.kt**: token constants (`colour.*`) + `darkColorScheme` per
  the DESIGN.md §3 colour map. `GymBuddyColors` (CompositionLocal) for
  cobalt, soft text, disabled line, lime pressed/tint, danger swipe,
  shadow, ripple. Old light colours removed.
- **Type.kt**: Figtree family, 15 M3 roles from `type.*` (no
  displaySmall), custom `statValue` and `tag`; `tnum` on display-*,
  titleSmall, bodySmall, labelSmall, statValue. Float noise rounded.
- **Shape.kt**: extraSmall 8 … extraLarge 28; `radius.full` → CircleShape.
- **Dimens.kt**: `space.*`, `size.*`, `stroke.*` in dp.
- **Motion.kt**: durations + `CubicBezierEasing`; rows without easing
  use `std`. Reduced motion (animator scale 0) → instant (motion.md rule 4).
- **Haptics.kt**: the motion.md haptics map over
  `View.performHapticFeedback` with API fallbacks (minSdk 28). Rest
  buzz (Vibrator) is not here; it comes with 7.6.
- **Theme.kt**: dark only, light system-bar icons off, exposes
  `GymBuddyTheme.colors/dimens/motion`.
- DESIGN.md licences row for Figtree: source + OFL.

## Tests
- Token parity: tokens.json vs Kotlin constants (if 1A).
- Typography: tnum present exactly on the listed styles.
- Haptics: each action maps to the constant in motion.md, fallback
  below its API level (Robolectric or pure mapping function).
- Existing unit tests stay green.

## Out of scope
Icons, components, nav, any screen redesign (7.2+). Light mode.

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

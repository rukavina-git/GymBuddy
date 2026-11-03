# Progress

- [x] 01 Theme (steps/01-theme.md) — b8a0806
- [x] 02a Icons + general components (steps/02-components.md) — d8049bc
- [x] 02b Workout + data components (steps/02-components.md) — HASH
- [ ] 03 App frame
- [ ] 04 Onboarding + auth
- [ ] 05 Home + Templates
- [ ] 06 Active workout
- [ ] 07 Exercise page + create
- [ ] 08 Progress tab
- [ ] 09 Session detail
- [ ] 10 You + Settings
- [ ] 11 Cross-flow states
- [ ] 12 Device pass, CI green, PR develop → main (me)

## Deviations
- 01: Figtree TTFs from the upstream repo (erikdkennedy/figtree, commit
  032dfa7), not google/fonts (which ships only the variable font).
  OFL text in assets/licenses/figtree-OFL.txt.
- 01: Reduced motion needs no code: Compose scales durations by the
  animator scale (motion.md rule 4). Motion.kt only documents it.
- 01: Haptics go through `View.performHapticFeedback` (motion.md rule 6),
  not Compose `HapticFeedbackType`. API: `rememberHaptics()(Haptic.X)`.
- 01: No `GymBuddyTheme.motion` accessor; motion is the top-level
  `Durations` / `Easings` / `PRESS_SCALE` / `motionTween`.
- 01: Old `colors.xml` entries were unused; it now holds only `ground`
  (window background). `enableEdgeToEdge` forced to dark bar styles.
- 01: ~10 hardcoded colours fixed (Register red/gray, Settings white on
  danger, Home green day → lime, filter sheet scrim). NumberPicker's
  Color.Black is a DstIn mask, left.

- 02a: Old `AppSnackBar.kt` was still used by Login/Register (function
  `AppSnackbar`, file `AppSnackBar`); both now use
  `SnackbarHost { ui.design.AppSnackbar(it) }` instead of keeping it.
- 02a: Files grouped by family: `Selection.kt` (chip, switch,
  segmented), `Bars.kt` (top bars, nav bar), plus `Modifiers.kt`
  (`focusRing`, `pressScale`) and `DesignPreviews.kt`.
- 02a: `ui/design/**` excluded from coverage (jacoco + Sonar action),
  like `ui/components/**`; logic is covered by DesignComponentsTest.
- 02a: `LargeTitleTopBar` is static; scroll collapse comes in 7.3.
- 02a: Chip gets `minimumInteractiveComponentSize()` (selectable
  Surface skips it), so it lays out 48 high, drawn 36.
- 02a: Roadmap 7.2 stays unticked until 02b lands.

- 02b: Steppers live inside `ValuePanel` (Figma), not on `SetCard`;
  `SetCard` takes a `panels` RowScope slot. Editing panel shows `step`.
- 02b: No Decimal key type in Figma; `KeypadKey(input)` renders
  Digit/Decimal as text (locale separator), Backspace as icon.
- 02b: RestLine is muted `bodySmall` + timer icon (DESIGN.md "quiet
  muted line" over Figma's text-colour title-small; owner OK).
- 02b: StatTile uses `stat-value` (DESIGN.md), Figma had title-large.
- 02b: LiveBar has the Figma lime open pill (not in the step file);
  progress line drawn along the top edge.
- 02b: Keypad is sheet content only (no handle/title). AppBottomSheet
  needs a title, so 7.6 needs a titleless variant or uses the label.

## Gotchas
- From 02b (reviewer, not fixed):
  - ValuePanel steppers on a 360dp phone: two 64dp keys need 128dp but
    each panel gets ~120dp in SetCard. Fix in 7.6 (smaller gap/keys or
    stacked panels) before building the workout screen.
  - ValuePanel steppers have no per-panel content description (TalkBack
    says "Decrease" twice); add e.g. "Decrease weight" in 7.6.
  - Raw dp (Keypad 76/20/40, SetMark 28, Rest icon 16, LiveBar 60/44/3,
    StatTile 14/2, day dot 36) aren't in DESIGN.md §4 yet; add rows.
    LiveBar 3dp = `dimens.strokeMark`.
  - Decimal key uses JVM default locale, not LocalConfiguration; the
    7.6 parser must accept "." and ",".
  - LiveBar subtitle lacks ellipsis; WeekStrip 7-day require untested.
- Flaky: ui/workout ViewModel tests sometimes fail with "Dispatchers.Main
  is used concurrently with setting it" (MainDispatcherRule.resetMain).
  Reproduces without 02b; likely Robolectric Compose tests leaking a
  Main reader. Rerun passes; fix before CI gates.
- From 02a (reviewer): all five fixed after the step (nav icon test via
  `navIcon()`, text field label merged into the field for TalkBack,
  snackbar dismiss ×, focus ring on banner/snackbar actions, previews
  for snackbar/dialog/sheet/menu/back bar, DESIGN.md typo).
- `AppBottomSheet` callers need `@OptIn(ExperimentalMaterial3Api::class)`
  (its default `rememberModalBottomSheetState()` is experimental).
- Use: `ui/design/*` parts, icons via `AppIcons.Ui/Glyph/Tab/Brand`
  (`painterResource`). Tabs: `NavTab` enum + `AppNavBar`. Re-convert
  icons with svg2vectordrawable if design/icons changes;
  IconParityTest fails otherwise.
- From 01 (reviewer, not fixed):
  - `Haptic.Reject` has no fallback below API 30, but motion.md wants
    "set couldn't save" to fall back to LONG_PRESS. Split it (e.g.
    `SaveFailed`) when 7.6/7.11 wire it, with a HapticsTest case.
  - TokenParityTest reads tokens.json by path; it isn't a Gradle task
    input, so a tokens-only change leaves the test UP-TO-DATE locally,
    and android CI's push path filter skips `design/**` (PRs still run).
  - M3 `secondary` = cobalt with `onSecondary` = text is ~3.3:1. Not
    mapped in DESIGN.md; don't put body text on `secondary`.
  - RegisterScreen uses `ColorTokens.Danger` directly in one place
    (same colour as `colorScheme.error`); the screen goes in 7.4.
- Use: `MaterialTheme.colorScheme/typography/shapes` for M3 slots;
  `GymBuddyTheme.colors` (cobalt, softText, disabled, lime tints,
  dangerSwipe), `.type` (statValue, tag), `.dimens` for the rest.
  `SheetShape`, `FullShape` in Shape.kt.
- From Plan 6: the expired-session banner (03) and the export sheet
  (07) are plain M3 placeholders; 7.4 and 7.10 replace them.
- From Plan 6 06: `nextInPlan` and derived-template sessions: decide
  before 7.4/7.5.

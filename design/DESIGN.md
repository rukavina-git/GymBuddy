---
name: GymBuddy
description: Soft Dark redesign of the GymBuddy Android app. Dark only in v1. Tokens live in design/tokens.json.
---

# GymBuddy design (Soft Dark)

Read this page before you build Plan 7. It tells you where each answer
lives and how the Figma tokens map to Compose. It does not copy the
tables: follow the links.

## 1. What this is

The one-page handoff for the Android redesign, written after every flow
locked (2026-11-02). Source of truth, highest first:

1. **Figma** [GymBuddy](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy).
   "01 System" (Rules, Light mode (later), Tokens, Icons, Components,
   Motion), "03 Locked designs" (final frames). "02 Rounds" is work in
   progress only, never build from it.
2. **design/ files**: `tokens.json` (committed copy of the Figma
   variables, generated, never hand-edit), `colors.md`, `do-dont.md`,
   `icons.md`, `components.md`, `motion.md`, `brand/`, `icons/`.
3. **Round logs** in `docs/plans/figma-foundation/design-brief.md`: the
   why behind each decision. Where they disagree with design/, design/
   wins.

Product context (users, scope, principles): `PRODUCT.md`.

## 2. Principles

- **Calm workout screen.** Only what's crucial while logging; extras
  one tap away. Nothing new goes on it unless a set could be lost.
- **Lime #D4FF24 = "you can press this."** Start, log, done, save, and
  done sets. Never icons, headings or decoration (except app icon and
  wordmark dot).
- **Cobalt #4D86F0 = "you are here."** Current set, active tab,
  selected row, switch on. Never a button.
- **Ground #121418** is the screen background. Depth comes from
  lighter surfaces, not shadows (only sheets cast one).
- **Dark only in v1.** Light is v2 (Figma Starter allows one variable
  mode).
- **M3 for structure, brand for feel.** Large collapsing title, bottom
  bar, sheets, dialogs, system Back. Brand comes through colour, type
  (Figtree), shape and motion. Don't invent navigation.
- **Sets, reps and kg are the biggest thing on a row.** Rest is one
  quiet muted line.
- **Honest and plain.** The user's own numbers, no streaks or cheers;
  plain words ("On this phone"), no "sync" or "server". Offline is
  normal, not an error.

Full list: `design/do-dont.md`. Every colour: `design/colors.md`.

## 3. Tokens to Compose

`design/tokens.json` is DTCG, hex8, dark only. Read it with a small
generator or copy values by hand into `ui/theme/`; either way, the JSON
wins.

| tokens.json group | Compose target | Notes |
|---|---|---|
| `colour.*` | `darkColorScheme(...)` + an extended colours object (CompositionLocal) | See colour map below. |
| `type.*` (+ `type.family` Figtree) | `Typography(...)` + two custom styles | Sizes in sp. |
| `radius.*` | `Shapes(...)` | `radius.full` → `CircleShape`. |
| `space.*` | own `Dimens` object (dp) | Compose has no spacing slot. |
| `size.*` | own `Dimens` object (dp) | `size.target` 48 = M3 `minimumInteractiveComponentSize`. |
| `stroke.*` | `BorderStroke` widths, icon/chart strokes (dp) | |
| `motion.*` | `tween(ms, CubicBezierEasing(...))` | Durations are plain ms. |

**Colour map (suggested).** M3 slots that match a token:
`background` / `surface` = `surface.ground`; `surfaceContainer` =
`surface.surface` (cards, rows, nav bar); `surfaceContainerHigh` =
`surface.raised` (keys, inputs, chips); `surfaceContainerHighest` =
`surface.high`; `primary` = `brand.lime`, `onPrimary` = `text.on-lime`;
`secondaryContainer` = `tint.cobalt-tint`, `onSecondaryContainer` =
`text.text`; `onSurface` = `text.text`; `onSurfaceVariant` =
`text.muted`; `outline` = `line.outline`; `error` = `danger.danger`;
`errorContainer` = `danger.container`; `scrim` = `overlay.scrim`.
No slot: `brand.cobalt`, `text.soft`, `line.disabled`,
`tint.lime-pressed`, `tint.lime-tint`, `danger.swipe`, `overlay.shadow`,
`overlay.ripple`; put them in the extended object.
Watch out: M3 colours Switch, focused TextField and some indicators
with `primary` (lime). Pass cobalt explicitly (`SwitchDefaults.colors`
etc.); cobalt is never a button.

**Typography.** 15 M3 roles in tokens (no `display-small`; don't use
it) plus two custom:
- `stat-value` 22 Bold, line 28: numbers on cards and stat tiles.
- `tag` 12 Bold, line 16, tracking 0.4: PR tags.
- Tabular figures: set `fontFeatureSettings = "tnum"` on
  `display-*`, `title-small`, `body-small`, `label-small`,
  `stat-value`. Figma can't store tnum; it's only in the text-style
  descriptions.
- The current `ui/theme/Type.kt` still uses `FontFamily.Default`;
  Figtree has to be bundled.

**Shapes.** `extraSmall` 8, `small` 12, `medium` 16, `large` 20,
`extraLarge` 28; `radius.full` (999 in Figma) → `CircleShape`. Sheets
round the top corners only.

**Float noise.** Figma stores float32. Round on import:
`motion.press-scale` reads 0.97000003 (use 0.97); tracking reads
0.10000000149 (0.1), 0.15000000596 (0.15), 0.40000000596 (0.4).

**Motion text drift.** The `short4` description in tokens.json still
says "reduced-motion crossfade". Ignore it: `motion.md` rule 4 wins
(Remove animations = instant cut). tokens.json was not re-exported
after step 07.

## 4. Raw sizes: use as drawn

No token yet; the Figma component is right. Reconcile in Plan 7.

| Part | Size (dp) | Note |
|---|---|---|
| Button/Primary large | 72 high | Done, workout only |
| Buttons small | 40 high | Top bar Save, small Secondary/Outline |
| InfoRow | 52 min height | |
| ListRow | 68 min height | `size.row` says 72; component wins |
| StepperKey | 64 wide × 56 | `size.stepper-width` says 60; component wins |
| Dialog | 356 wide | |
| Rest buzz | 80 on · 120 off · 80 on (ms) | `RestBuzz.PULSE_MS` / `GAP_MS` (Plan 7 06c) |
| Notification ring | 24 (18 drawn) | `ic_notification_workout`, white, tinted lime by the system |

All heights are minimums: rows and buttons grow with font scale.

**Deliberately unbound in Figma** (don't make tokens from them):
status bar "9:41", the drawn keyboard, wordmark texts, 200% demo
frames, R5-A motion frames, Lock screen (incl. ring 53:3478), Live bar
spec frame, the Google G, Google / Android / board colours, icon
geometry radii 1–3 and strokes 1.33 / 1.67 / 2.5 / 2.75 (scaled or
chevron icons). Icons in code use `stroke.icon` 2; the one exception
is the Exercises active tab (2.75, see icons.md).

## 5. Components

Index: `design/components.md` (35 components, Compose base for each,
variants, where used, merges D1–D14). Visuals: Figma "01 System" →
Components.

On the locked frames, not every part is an instance:
- **Instances** (627): nav bar, live bar, buttons, chips, switch,
  search, set row, stepper, keypad key, session / template / info
  rows, stat tile, snackbar.
- **Drawn but token-bound** (content differs per screen; build them
  from the components): sheets, dialogs, menus, set card, keypad
  sheet, week strip, top bars, text fields, list rows.
- **Skipped on purpose**: filter chip with ×, mid-swipe set, rows with
  extra texts, PR glyph row, 200% frames.

Sheets use the `surface` fill (D3 amended), not `surface-raised` as
colors.md and the tokens.json description say.

## 6. Icons and brand

Index and licences: `design/icons.md`. Files: `design/icons/ui`,
`design/icons/glyphs`, `design/icons/tabs`, `design/brand`.

Brand files: `mark.svg`, `wordmark.svg`, `lockup.svg`, `app-icon.svg`,
`app-icon-foreground.svg`, `app-icon-background.svg`,
`app-icon-monochrome.svg`. The mark is our own ECG line; scale it,
never redraw.

Tinting:
- Material UI icons (`ui/*`) are **fills**: tint the fill.
- Custom glyphs and tab outlines are **strokes**: tint the stroke.
- **Filled tab icons are never tinted** (they have cut-out detail).
- Default colour `text.text`, secondary `text.muted`, cobalt only for
  "you are here". Never lime.
- 24dp grid, 48dp tap area even when the icon is 20–24.

## 7. Motion and haptics

Spec: `design/motion.md` (7 rules, 27-row inventory, haptics map).
Figma: "01 System" → Motion. Locked frames have no prototype
animation.

Plan 7 must act on these reviewer notes (step 07, not yet fixed in
motion.md):
- Use Compose `HapticFeedbackType` (own fallbacks for constants newer than minSdk 28)
  instead of hand-rolled calls.
- "The set lands" (row 2) likely needs `sharedBounds` or one node that
  swaps content, not `animateBounds`.
- Drag reorder (row 20) needs a library or custom code. Check the
  library licence first (MIT / Apache / BSD only).
- Navigation Compose 2.8+ already does predictive back: drop
  `PredictiveBackHandler` from row 15.
- `ModalBottomSheet` motion can't take our tokens: keep M3 defaults.
- Rest buzz: done in Plan 7 06c (`VIBRATE`, `USAGE_NOTIFICATION`, skips
  silent and Do Not Disturb). Notification channel "Workout" is silent.
- Rows without an easing default to `std` (`easing.standard`).
- Done followed by "Couldn't save" fires two pulses (CONFIRM then
  REJECT); decide if that's wanted.
- The 1 s outline hold and the 80 / 120 / 80 ms buzz are raw ms, not
  tokens.
- No motion is defined for the first-3-workouts hint.

## 8. Screens

Links go to "03 Locked designs". Current code is under
`android/app/src/main/java/com/rukavina/gymbuddy/ui/`.

| Flow | Figma | Replaces today |
|---|---|---|
| App frame (tabs, live bar) | [85:9890](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=85-9890) | `MainScreen.kt` nav bar, `theme/Color.kt`, `Type.kt`, `Theme.kt` |
| Home | [87:11340](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=87-11340) | `HomeScreen.kt` |
| Templates | [92:12488](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=92-12488) | `CombinedWorkoutsScreen.kt` (Templates tab), `template/WorkoutTemplateScreen.kt`, `ExercisePickerDialog.kt`, `ExerciseConfigDialog.kt`; `WorkoutsScreen.kt` is a placeholder |
| Active workout | [67:6561](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=67-6561) | `workout/ActiveWorkoutScreen.kt`, `SetTrackingFields.kt`, `components/NumberPicker.kt` (wheels go; keypad + − / +) |
| Exercise page + create | [97:14553](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=97-14553) | `exercise/ExerciseScreen.kt`, `ExerciseDetailsScreen.kt`, `ExerciseCreationBottomSheet.kt`, `ExerciseWizardDialog.kt` |
| Progress tab | [62:5195](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=62-5195) | `SessionsScreen.kt` + `StatisticsScreen.kt` (placeholders), session list in `workout/WorkoutScreen.kt` (Sessions tab) |
| Session detail | [107:15763](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=107-15763) | No screen today; closest is the session dialog in `workout/WorkoutScreen.kt` |
| You + Settings | [110:17469](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=110-17469) | `profile/ProfileScreen.kt`, `profile/edit/*`, `settings/SettingsScreen.kt`, `AppPreferencesScreen.kt`, `HiddenExercisesScreen.kt`, `HiddenTemplatesScreen.kt`, `AboutScreen.kt` |
| Onboarding + auth | [114:20520](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=114-20520) | `SplashScreen.kt`, `auth/LoginScreen.kt`, `RegisterScreen.kt`, `PasswordTextField.kt`; onboarding is new. Includes the "session expired" frame for roadmap 6.3 (auth gate) |
| Error / offline states | [124:26620](https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=124-26620) | `components/EmptyState.kt`, `LoadingState.kt` (local lists show nothing while loading, no skeletons), `AppSnackBar.kt`, `settings/UnsyncedChanges.kt` |

Nav: 5 tabs, Home, Workouts, Exercises, Progress, You. Progress
replaces v1 Sessions + Statistics.

## 9. Accessibility floor

- WCAG AA: text 4.5:1, icons, outlines and large text 3:1. Contrast
  per colour is in colors.md. Placeholders are text (use `muted`).
- Every tap area 48dp, in the workout above all, even when the drawn
  part is smaller (chip 36, small button 40, switch 32).
- Follow system font scale up to 200%. Rows and buttons use min
  heights, never fixed heights. At 175%+ drop extras, never cut the
  set or the number.
- State is never colour alone: pair it with a check, ring or text.
- Focus: 2dp cobalt ring outside the control.
- Charts: one TalkBack stop with a summary sentence.
- Remove animations: instant cut, haptics stay (motion.md rule 4).

## 10. Licences

Owner rule: only MIT, Apache 2.0, BSD, OFL, CC0 or CC BY (with credit).
Never NC, ND, SA or GPL/AGPL. Record every third-party asset here.

Libraries are not listed here: the build collects them and fails on a
licence outside the allow-list (ADR 0018). Users see them, and the
assets below, in You › About › Open-source licences.

| Asset | Source | Licence | Note |
|---|---|---|---|
| UI icons | Material Symbols Rounded, weight 400, `@material-symbols/svg-400` 0.27.0 | Apache 2.0 | List in the app's open-source licences screen. Android drawables converted 2026-11-03 with `svg2vectordrawable` 2.9.1 (MIT, run once via npx, not a build dependency) |
| Font | Figtree | OFL 1.1 | Bundled 2026-11-03: Regular, Medium, SemiBold, Bold static TTFs from github.com/erikdkennedy/figtree (upstream, commit 032dfa7) in android/app/src/main/res/font; licence text in assets/licenses/figtree-OFL.txt |
| Profile animals (20) | Microsoft Fluent Emoji, 3D style, github.com/microsoft/fluentui-emoji (commit 1ffb34c) | MIT | Bundled 2026-11-07: 256 px PNGs scaled to 144 px with `sips`, in android/app/src/main/res/drawable-nodpi/avatar_<id>.png; licence text in assets/licenses/fluent-emoji-MIT.txt. Stored in the profile as an id (e.g. `fox`) |
| Google sign-in button + G logo | Google | Google's brand rules | Use Google's own dark button; never recolour or tokenise |
| Mark, wordmark, app icon, gym glyphs, tab icons, notification ring | Own drawing | Ours | Wordmark is Figtree Bold outlined (OFL) |

| Built-in exercise photos (exercise page, 7.7) | Were hotlinked from github.com/yuhonas/free-exercise-db | Repo Unlicense, photos' origin unknown | **Removed 2026-11-13** (owner): backend V8, exercise library v10; the page shows its stand-in. Own photos planned (Plan 9 step 01b) |
| Exercise images (exercise page) | Made by the owner with Google Gemini (free tier) from `design/exercise-images/prompts.html`, from 2026-11-13; originals in `design/exercise-images/raw/` | AI-generated; Google's terms let the user use generated content (check the current Gemini terms before release); not copyrightable, so others could copy them | Bundled in `res/drawable-nodpi/ex_*.jpg` (506 px, JPEG 85), mapped by exercise id in `ExerciseImages.kt`; shown as a centred square at 60% width |

Still open: the licence of the stock reference exercise photos in
PRODUCT.md "Evidence on Hand" (the design references, not the app's photos).

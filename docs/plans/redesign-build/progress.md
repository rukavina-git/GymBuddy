# Progress

- [x] 01 Theme (steps/01-theme.md) — b8a0806
- [x] 02a Icons + general components (steps/02-components.md) — d8049bc
- [x] 02b Workout + data components (steps/02-components.md) — 8263a47
- [x] 03 App frame (steps/03-app-frame.md) — d69ddbd
- [x] 04 Onboarding + auth (steps/04-onboarding-auth.md) — 6781df5
- [x] 05a Home + Workouts tab + template page (steps/05-home-templates.md) — f609339
- [x] 05b Template editor + exercise picker (steps/05-home-templates.md) — 803691f
- [x] 06a Active workout: logging (steps/06-active-workout.md) — 0d33cb5
- [ ] 06b Active workout: around the workout (steps/06-active-workout.md)
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

- 03: Collapsed title uses `titleLarge` (20/600); Figma has 22 Bold, no
  matching token. Collapsed bar is `surfaceContainerHigh` (surface/raised).
- 03: Own `LargeTitleState` (nestedScroll) instead of M3 `LargeTopAppBar`
  (its expanded style can't take headline-large + actions row on top).
- 03: Live bar enter is expand-from-bottom `long2` `emph`, a stand-in
  for the ⌄ container transform (motion.md #10) until 7.6. Exit is
  motion.md #12. Only text crossfades; the clock ticks without fading
  (`LiveBar(subtitleKey)`).
- 03: Icons-only nav bar from fontScale 1.3 (`NAV_ICONS_ONLY_FONT_SCALE`,
  estimate); tooltip on long-press via M3 `TooltipBox`.
- 03: Live bar text from the old workout state: "Workout · exercise",
  "Set x of y · mm:ss", "All sets done" when finished.
- 03: Workout complete/discard pops the workout, then switches to the
  Workouts tab (old code could stack Workouts on top of the workout).
- 03: Manifest sets `enableOnBackInvokedCallback="true"` (default on
  target 36+, needed for API 33–35).

- 04: Google "no account found" runs on a second FirebaseApp
  ("sign-in-probe"), so AuthGate never sees an account the user didn't
  agree to. "Create account" from that dialog deletes the probe account
  and opens Create account (consent first; one more Google tap).
- 04: Account collision link dialog (old LoginViewModel) dropped: Google
  is a trusted provider, Firebase links it itself.
- 04: Other-account check (R16-13) compares emails (owner vs typed or
  Google email), not uids; counts = owner file's outbox.
- 04: Consent parked in DataStore (`pendingConsentAt`) by SignInViewModel,
  written by `ConsentRecorder.applyTo` from AuthGate.afterSignIn (unless
  merge is Asking) and `GuestMergeCoordinator.afterMerge`; Cancel clears.
  Version string `2026-11-03` until the privacy pass (4.6).
- 04: `GuestMergeDialogs` restyled in place (AppChoiceDialog + snackbar
  receipt via new `AppFrame(snackbarHost)`), not deleted.
- 04: `PasswordTextField` kept: SettingsScreen's password dialogs use it
  (goes in 7.10).
- 04: Plan rows show "<days> · N workouts" from `suggestedFor` + template
  count, then the bundled description (Figma copy isn't in the data).
- 04: New design parts: `AppLinkButton` (lime text), `AppChoiceDialog`
  (lime/plain/danger actions, stacked option). Transitions moved to
  `ui/theme/Transitions.kt`; root NavHost uses shared axis.
- 04: Settings guest entries: "Back up your workouts" (create) and
  "Sign in"; logout/delete/erase go to Welcome. Splash has no delay.
- 04: Strings in `res/values/strings_onboarding.xml`.
- 04 (fix after owner check): logo was invisible: `brand_mark` is the
  dark ECG line only, meant for lime. Added `brand_app_icon` (lime tile,
  converted from design/brand/app-icon.svg) as `AppIcons.Brand.AppIcon`
  for Splash, Welcome and sign-in.
- 04 (fix after owner device check): spacing to Figma. Wordmark 41/31dp
  (was 32/24); sign-in header gaps 64/16/24/28; 32dp around "or";
  Welcome and Ready buttons 40dp above the system bar (Figma had ~3dp
  over a 3-button bar). Welcome line breaks per sentence, not by width
  (Figma's break gave an orphan on a 384dp phone).

- 04 Amendment 1 (account first, owner 2026-11-03): Figma R18-1…R18-7
  locked (R16-1/4/7/8 removed). Welcome = Create account / I have an
  account / Continue without account. Create account is its own route
  (`create_account?from=`) with three steps in one ViewModel
  (METHOD → EMAIL → CONSENT); Google's token waits for Agree. Setup step
  5 is that route with from=SETUP, answer passed back via savedStateHandle;
  an existing Google account from step 5 goes straight to Main (setup
  would overwrite its plan). SignInViewModel is sign-in only, with a
  busy `SignInAction` for spinners. `ButtonSize.Form` (48dp) and a
  `loading` spinner on Primary/Outline buttons. No repeat-password field.
- 04 Amendment 1: profile edits from setup and ConsentRecorder go through
  `UserProfileRepository.updateProfile` (read + write in one transaction),
  so neither overwrites the other's fields. Setup's finish waits up to 5s
  for the account's file; on timeout it saves nothing and Let's go works
  again.
- 04 Amendment 1 (owner): "Not now" before setup counts for setup too:
  `setup?offerAccount=false` skips step 5 (4-step bar).
- 04 Amendment 1: Settings guest entries are "Create account" and
  "Sign in"; ADR 0014 got an amendment note.

- 05a: Start sheet has no "+ Empty workout": the workout can't add
  exercises until 7.6. Add it there.
- 05a: Workouts tab ⋯ has one item, "Hidden templates" (Figma doesn't
  say what ⋯ holds). Search is a plain title filter (typo search is 05b).
- 05a: Start sheet "Your templates" = templates you made or have done;
  the Workouts tab splits by source (Figma shows a done starter under
  Your templates on the sheet, under Starter plans on the tab).
- 05a: "about N min" = average of finished sessions, rounded to 5 min.
  Week strip is Monday first.
- 05a: Delete waits for the snackbar: `PendingTemplateDeletes` hides
  the id, `TemplateDeleteViewModel` writes the tombstone when the
  snackbar is dismissed without Undo (the repository has no undelete).
- 05a: The pencil and "+ New template" still open the old dialog, now in
  `WorkoutTemplateFormDialog.kt` (renamed from WorkoutTemplateScreen.kt).
  A starter's pencil makes "<name> (copy)" first and opens it with
  `edit=true`.
- 05a: Progress tab shows the old `WorkoutScreen` under a large title
  until 7.8. StatisticsScreen deleted; SessionsScreen (unused) left for 7.8.
- 05a: `UserProfileDao.observeUserProfile` + `UserProfileRepository.
  observeProfile()` added (query only, no schema change).
- 05a (test-only): the cross-test Main flake was the test DataStore on
  Dispatchers.IO. `testPreferencesDataStore()` now runs on an Unconfined
  scope; ActiveWorkoutViewModelTest also cancels its VMs in @After.
  Full suite green 3 runs in a row.

- 05b: Figma read via the Console MCP (official Figma MCP hit the Starter
  call limit).
- 05b: `WorkoutTemplateViewModel` deleted; `HiddenTemplatesScreen` (7.10)
  now uses a small `HiddenTemplatesViewModel` (unhide only). The template
  use cases GetAll/Search/GetById/Delete are now unused (domain, left).
- 05b: Editor and picker are own routes (`template_editor?templateId=`,
  `exercise_picker?inTemplate=`), full screen. Picks come back through the
  editor entry's savedStateHandle (`picked_exercises`). TemplatePage lost
  its `edit` arg: a starter's pencil copy replaces the page, then pushes
  the editor. New template Save pops back to Workouts (no page opened).
- 05b: Picker R12-13 'Create "bnch"' row left out (exercise create is 7.7).
  Muscle chips: All, Chest, Back, Legs, Shoulders, Arms, Core (no Other).
  Row subtitle = first primary muscle · first equipment.
- 05b: Save is disabled with no exercises (use case needs ≥ 1); empty name
  shows "Give it a name" on Save. Discard dialog body for a new template:
  "This template won't be saved."
- 05b: Keypad Next walks sets → reps/time/distance → kg → rest → next
  card; last field says "Done". Rest and time are typed m:ss from the
  right ("130" = 1:30). 0 sets keeps the old value; empty reps/kg/rest
  clear it. `KeypadValue` holds the editing + parsing ("." and ",");
  Decimal key now uses LocalConfiguration's locale.
- 05b: Titleless sheet = `AppBottomSheet(title = null)`; `Keypad` got
  `nextLabel`; `BackTopBar(close = true)` shows ×.
- 05b: Notes edit inline (AppTextField in the card), not a dialog. Undo
  re-inserts the card with a new list key (the swiped composition kept its
  dismissed state otherwise).
- 05b: Muscle/equipment display names in `ui/exercise/ExerciseText.kt`;
  strings in `res/values/strings_editor.xml`.

- 06a: Figma read via the Console MCP. All 8 owner decisions as recommended.
- 06a: `NumberPicker.kt` kept (profile edit screens, 7.10) and
  `SetTrackingFields.kt` kept (WorkoutScreen manual set entry, 7.8/7.9).
  WorkoutScreen.kt had no active part left to delete.
- 06a: A set counts only after Done (`isCompleted`), not when filled.
  Sets start from the template targets (reps, kg, time, distance); Done
  copies the set's values to the sets after it.
- 06a: Steps ± 2.5 kg / 5 lb, ± 1 rep, ± 5 s, ± 100 m (distance step not in
  Figma). Floors 0 kg, 1 rep, 5 s, 0 m. Rules: ≥ 1 rep, ≥ 1 s, > 0 m; weight
  may be empty. Times show m:ss everywhere ("1:00", Figma also has "60 s").
- 06a: Rest runs on the workout clock (`RestState.startedAtElapsed`), in
  the draft JSON with the current exercise (Moshi defaults; no Room change).
  Template without rest uses `DEFAULT_REST_SECONDS` (2:00, from 05b).
- 06a: ⌄ and Back pop the workout with a scale-to-bottom + fade (`long2`
  `emph`), stand-in for the container transform: the live bar is outside
  the NavHost. Set lands = in-place content swap (fade + size,
  `SET_LANDS`), no lime sweep or digit roll. Odometer rolls the whole
  number, not per digit.
- 06a: Swipe right on the set card = Done (motion.md #7); swipe left =
  delete. Long-press → Delete on rows only (on the card it fired under a
  held − / +, caught by a UI test).
- 06a: `SetCard(panels = List<(Modifier) -> Unit>)`, stacks below 328dp
  inner width. `StepperKey` holds to repeat (raw 400/150→50 ms).
  `Keypad(message)` for rules. `Haptic.SaveFailed` (REJECT, LONG_PRESS
  below 30). `UnitConverter.formatTrimmed` uses Locale.ROOT (05b gotcha).
- 06a: Summary is its own route `workout_summary`; Done goes to the Workouts
  tab. Backup "Create account" uses `createAccount(CreateFrom.SETTINGS)`
  (no `signIn(CREATE)` route exists).
- 06a: Left for 06b: Home "Workout in progress" card (53:3414), live bar
  rest variant, "3 / 15" pill (All exercises). "How to" hidden until 7.7.
- 06a: Roadmap 7.6 stays unticked until 06b lands.

## Gotchas
- From 06a (reviewer, not fixed):
  - Summary after process death: `workout_summary` comes back with no
    summary in the new VM and renders blank (only Back leaves). Pop or
    fall back to Done when null.
  - Rest isn't shown between exercises: the rest line needs a set left in
    the shown exercise, and Done jumps to the next exercise. Show it on the
    next exercise (or keep the old one on screen).
  - Undo after a newer set restores the old rest over the new one
    (`rest = d.rest ?: it.rest`).
  - Hold to the floor/ceiling never plays Reject: disabling cancels the
    press before the check.
  - `restText` (counting vs "+0:45") has no test.
  - Swipe right on the card logs a set; a sideways slip on − / + could too.
  - Pausing the workout clock freezes rest too (no pause UI today).
  - Keypad accepts weight/distance over the stepper ceiling (999 kg).
  - Summary `lifted` reads all exercises, sets from kept ones (differs
    only for rule-breaking done sets from old drafts).
- Use (06a, for 06b): `ActiveWorkoutViewModel.selectExercise`,
  `deleteSet/undoDelete`, `WorkoutEvent`, `SetFields`, `WorkoutText.kt`
  helpers; `ActiveWorkoutUiTest` sets up a real VM (qualifier
  w412dp-h915dp, timer paused).
- From 05b (reviewer, not fixed):
  - Picker search text round-trips through `combine(...).stateIn`
    (async): fast typing can jump the cursor, and text typed before the
    exercise list loads is wiped. Keep the query as plain sync state.
  - Editor draft and picker picks aren't in SavedStateHandle: process
    death loses unsaved edits; a restored `picked` result can race
    `load()` and be overwritten.
  - `UnitConverter.weightToDisplayUnit` only trims after "."; comma
    locales show "100,00 kg" (chips, keypad start text). Fix the helper.
  - Distance is always metres (no imperial display).
- Use (05b, for 7.6): `ExercisePickerScreen` + `NavRoutes.exercisePicker
  (ids)` and `ExercisePickerViewModel.RESULT_PICKED`; `KeypadValue`;
  `TypoMatcher` (also 7.7 Exercises search); `muscleLabel`/`equipmentLabel`.
- From 05a (reviewer, not fixed):
  - `TemplateDeleteViewModel.commit` has no catch: a DB/outbox error
    crashes the app. Catch, put the template back, show a message.
  - The Undo wait lives in MainContent's `rememberCoroutineScope`; a
    rotation during the snackbar cancels it and the delete is undone
    (nothing lost, but the delete is dropped). Move it to a VM.
  - `workoutRunning` (exercises not empty) differs from
    `hasActiveWorkout()`; matters once 7.6 adds empty workouts.
  - Template page "gone → back" can race the Deleted event; if it wins,
    the id stays pending (hidden, never deleted) until restart.
  - Copy names are checked against visible templates only.
  - Old edit dialog errors (`WorkoutTemplateViewModel.errorMessage`)
    are never shown; 05b replaces it.
  - Home's `today` updates only when a source emits (stale after
    midnight until something changes).
  - No tests for commit/copy failure paths.
- Use (05b): `TemplateSources` (fake in tests: `FakeTemplateSources`,
  `TemplateFixtures`), `TemplateLine(s)`, text helpers in
  `ui/template/TemplateText.kt` (`targetText`, `clock`, `relativeDay`,
  `joinDot`). Route `NavRoutes.templatePage(id, edit)`; 05b's editor
  should take over the `edit=true` path and the Workouts "+" action.
  Strings in `res/values/strings_templates.xml`.
- From 04 Amendment 1 (reviewer, not fixed):
  - Consent "Not now" (and back) stay tappable while Agree is running;
    leaving cancels the VM and `pendingConsentAt` stays parked (also on
    process death). Same family as the stale-consent note below: ignore
    Not now/back while busy and clear the pending value unless created.
  - `CreateAccountViewModel`: `setLastSignInEmail` after createWithEmail
    is outside the catch; a DataStore IOException would crash.
  - New account from Welcome: `popUpTo(Welcome)` isn't inclusive, so back
    from the first setup question shows Welcome to a signed-in user.
  - `afterAccount`'s `insideMain` checks the whole back stack.
  - Google Password Manager offers to save the password on the email
    step's Continue, before the account exists.
- From 04 (reviewer, not fixed):
  - Pending consent can go stale (Add fails after Asking, or process
    death before AuthGate) and later land on another account at a
    SIGN_IN sign-in. Near-privacy issue: tie it to the email/uid or
    clear it on SIGN_IN. Fix before 7.12.
  - Probe account left behind if the app dies while "No account for
    this email yet" is open; next Google sign-in then skips the
    question. Fix: on probe init, delete any signed-in probe user.
  - `AuthClient.mapped` maps CancellationException to OTHER; rethrow it.
  - `SignInEvent.Done` / `OnboardingEvent.Done` have no replay; lost if
    emitted during recreation.
  - Create + Google on an existing account whose profile isn't pulled
    yet: ConsentRecorder writes a bare rev-0 profile, server conflict
    drops the consent.
  - `OnboardingViewModel.finish()` has no try/finally: a failed save
    leaves Let's go disabled.
  - Google button doesn't set busy; double tap can open two sheets.
  - Older builds can't parse `GET_STRONGER` (fine before release).
- Every full-screen root needs a Surface or Scaffold (not
  `Modifier.background`): Text without a colour falls back to black.
  Setup and Ready titles were black on black until fixed after 04.
- Use: `AppChoiceDialog` for account questions; `navController.openMain()`
  / `openWelcome()` (StartNavGraph.kt) instead of raw navigate.
  `NavRoutes.signIn(SignInMode.CREATE)` is the guest backup entry for
  7.10's You tab and the 7.6 backup card.
- Raw dp added (DESIGN.md §4 rows still owed): brand mark 80/64,
  wordmark 32/24, Google logo 20, choice row 68, suggested tag 24.
- From 03 (reviewer, not fixed):
  - Tab fade-through only when both ends are tab roots; switching tabs
    from a pushed screen (or to a tab with a saved pushed screen)
    slides instead. Decide by `selectedTab` of both back stacks.
  - MainContent wiring (live bar opens workout, leaveWorkout pops
    before tab switch) has no test; AppFrameTest covers the parts.
  - `workoutState` read in MainContent recomposes the frame every
    second; `AppFrame` LiveBarSlot writes `last` in composition.
  - `LargeTitleState` collapses even when content can't scroll, and
    isn't saveable (title re-expands after tab restore/rotation).
  - " · " separators are hard-coded, not string resources.
  - Icons-only tooltip anchors on the icon, not the whole item.
- Use: tab roots from 7.5 on take `rememberLargeTitleState()` and
  `Modifier.nestedScroll(state.nestedScrollConnection)` on their list.
  Switch tabs with `navController.navigateToTab(tab)`; never
  `navigate(tabRoute)` (breaks per-tab stacks). Routes `progress`
  (StatisticsScreen) and `you` (SettingsScreen) until 7.8 / 7.10.
- Old ActiveWorkoutScreen has no ⌄; leaving is system Back until 7.6.
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
- 04: SignInViewModelTest cancels its ViewModels' scopes in @After (init's
  DataStore read leaked onto Main after resetMain). The workout flake
  below is separate: a withContext(IO) inside ActiveWorkoutViewModelTest.
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

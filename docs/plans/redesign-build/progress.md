# Progress

- [x] 01 Theme (steps/01-theme.md) — b8a0806
- [x] 02a Icons + general components (steps/02-components.md) — d8049bc
- [x] 02b Workout + data components (steps/02-components.md) — 8263a47
- [x] 03 App frame (steps/03-app-frame.md) — d69ddbd
- [x] 04 Onboarding + auth (steps/04-onboarding-auth.md) — 6781df5
- [x] 05a Home + Workouts tab + template page (steps/05-home-templates.md) — f609339
- [x] 05b Template editor + exercise picker (steps/05-home-templates.md) — 803691f
- [x] 06a Active workout: logging (steps/06-active-workout.md) — 0d33cb5
- [x] 06b Active workout: around the workout (steps/06-active-workout.md) — bd61611
- [x] 06c Active workout: rest buzz + lock screen (steps/06-active-workout.md) — 2d9b952
- [x] 07 Exercise page + create (steps/07-exercise-page.md) — 0245dcc
- [x] 08 Progress tab + All sessions (steps/08-progress.md) — c5dc7c7
- [x] 09a Session detail (steps/09-session-detail.md) — 453d555
- [x] 09b Edit workout (steps/09-session-detail.md) — 5135a08
- [x] 10a You page (steps/10-you-settings.md) — 86974f2
- [x] 10b Profile, picture, body weight (steps/10-you-settings.md) — dab5844
- [x] 11a Cross-flow: workout safety (steps/11-cross-flow-states.md) — 5e145e6
- [x] 11b Cross-flow: backup, re-download, small states (steps/11-cross-flow-states.md) — 5c4299b
- [x] 11c Device feedback A1: profile + sheets + settings bugs (F1, F3a, F7, F10, F12) — d3b7634
- [x] 11d Device feedback A2: workout bugs (F17, F22, F19, F21a-c) — 3694d71
- [x] 11e Device feedback B: quick cuts (F2, F4, F8, F14, F9, F11, F16) — dfeaf5d
- [x] 11f Feedback: F20, F23, F15, F24a, F21a, F21d (grown: owner asked for bigger feedback steps) — aa75b70
- [x] 11g Feedback: F1 + F12 avatar revert, backup "not possible" (stale Render build; steps/11g-profile-sync.md) — 2576fc9
- [ ] 11i Feedback: F24b save as template after an empty workout
- [ ] 11j Feedback: F6 height and body weight wheels
- [ ] 11k Feedback: F13 delete account screen
- [ ] 11l Feedback: F3b typing off sheets (notes, quick create, name/units inline)
- [ ] 11m Feedback: F25 stale workouts (2 h rule, auto-finish, estimates)
- [ ] 11n Feedback: F18 prefill sets from last session or template
- [ ] 11o Feedback: F5 edge swipe back (only with gesture nav; last)
- [ ] 12 Device pass, CI green, PR develop → main (me)

## Deviations
- 11g: No code change. Cause was a stale Render build (pre-Plan 6; Render tracks develop with
  autoDeploy off), not a sync conflict as the 11c leads guessed. Owner redeployed develop.
  Phone check via adb: avatar survives sync and restart (fox, rev 13, SYNCED), offline change
  uploads once online, sync_log empty, reference `plans` error gone. "Back up and sign out"
  not tapped (would sign the owner out); it runs the same flushOutbox push that now succeeds.
  Roadmap has no 11g line; nothing to tick there.
- 11f: Spec is device-feedback.md "Round C consult"; device feel over Figma, no Figma read.
  Scope grew from F20+F23 to also F15, F24a, F21a, F21d (owner: bigger steps for feedback);
  11h folded in, 11i keeps only F24b.
- 11f: F21d "lighter surface" not done: no lighter surface token exists (surfaceBright ==
  surfaceContainerHighest). Snackbar keeps SurfaceHigh + outline. Needs a token decision.
- 11f: F21d timer runs inside AppSnackbar for any snackbar with an action and
  SnackbarDuration.Indefinite (M3's default when actionLabel is set). The 6 Undo callers
  moved from Short to Indefinite. Timeout = max(8 s, a11y recommended).
- 11f: F21a replaces SwipeToDismissBox with AnchoredDraggable (Settled / Revealed 88 dp /
  Deleted / Done). The swipe background paints only the exposed strip (set rows are
  see-through). F23 distance tile shows "5.2" over "km".
- 11f: F20 phone check: 4.5 s hold now reaches 82.5 kg (was 167.5), no clipped digits.
  Verified on the phone via adb: F15, F23, F24a, F20, F21a reveal + Delete + Undo, F21d line.
- 11e: Spec is device-feedback.md Round B; device feel over Figma, so no
  Figma read. Figma still shows the letter row, licence line, BMI footer,
  phone tip and raised collapsed bar; update it after.
- 11e: F4 random animal is set in `UserProfileRepository.updateProfile`'s
  create path only (new local rows). Existing or server-pulled profiles
  without one keep the letter until the owner picks. Ids live in
  `domain/model/AvatarIds` (test keeps them equal to `ui/you/Avatars`).
  "Or pick one" label went with the letter row.
- 11e: F9 keeps the dark snackbar (an inverse one breaks the lime action
  and close icon): adds a 1 dp outline + 6 dp shadow on every snackbar;
  export message is Long with a close button. Full redesign stays F21d.
- 11e: F11 adds icon `ui/show` (Material Symbols Rounded `visibility`,
  Apache 2.0, design/icons.md); one eye toggles all three fields. The
  delete-account password field has no eye (not in F11).
- 11e: F16 drops the raised box; the bar stays page coloured and draws a
  16 dp fade below itself (zIndex over the list).
- 11e: roadmap has no 11c–11e rows (7.11 already ticked); nothing ticked.
- 11d: Spec is device-feedback.md; bug fixes, no Figma read. F21a
  diagnosed, not fixed: set rows have no trash button; the only trash is
  the swipe background icon, which can't be tapped. Owner to confirm.
- 11d: F17 changes the 06a rule "weight may be empty (bodyweight)": Done
  and the keypad now need weight > 0 on weight types (bodyweight lifts
  use Reps only). Saving skips the weight rule (`SetFields.savable`), and
  Edit workout keeps weight optional (`weightRequired = false`), so sets
  logged before F17 are never dropped or blocked.
- 11d: Edit workout change was the reviewer's SHOULD (a regression), fixed
  under the "app stays usable" invariant.
- 11c: Spec is device-feedback.md (no step file); bug fixes only, so no
  Figma read (device feel over Figma). F1 and F12 diagnosed, not fixed:
  sync changes are outside the plan invariants. Both stay open.
- 11c: F3a pads the sheet surface (not just content) for the nav bar, so
  the sheet floats above a strip of scrim. Unverified on device.
- 11b: Not split (owner). Figma read via the Console MCP (C1b, R1, E3, C2a,
  R2, R3, D3, E4, E5, E6, notes).
- 11b: Resync card replaces the week card (Figma C2a/R2 show no week
  strip; the step said "templates card"). Rows become one muted line,
  "All templates" hides while it runs or failed.
- 11b: Home's 3-day rule is its own `stuckDays` (HomeSync.kt) on the
  oldest outbox `enqueuedAt`, not 10a's `backupLine` (that one is 24 h and
  shows counts). Guests never see the line.
- 11b: `SyncEngine.resync` is scoped to the database owner (`holder.ownerFlow`):
  sign out, guest or another account reads Idle (reviewer blocker).
  Failed clears on the next successful pull, else on process death.
- 11b: E5 body says "A chart appears after 2 entries" (10b chart needs 2;
  Figma says 3). Hidden templates empty copy mirrors E6 (no frame).
- 11b: Start sheet keeps Empty workout on top (R11-3), R3 has it last.
- 11b: D3 also hides "All N sessions" for a lift done once. Progress
  rule unchanged (ProgressStats: 3 sessions, 14 days).
- 11b: `ui/components/**` deleted; coverage globs for it left in place.
- 11a: Split 11a/11b (owner). Figma read via the Console MCP (R17-B1, E2).
- 11a: E2 "Unfinished workout" is a Home card under the week strip
  (Figma), not a dialog. Discard confirms with an AppDialog; buttons
  40dp (`ButtonSize.Small`).
- 11a: `Haptic.SaveFailed` already existed (01 gotcha was done); no split.
- 11a: "Last set" = draft `savedAt` (decision 3), so any edit counts.
- 11a: Unfinished also hides the live bar and the workout notification
  until Resume. Resume always starts the timer.
- 11a: Banner title falls back to "Your last change isn't saved yet"
  when no set was marked done.
- 11a: roadmap 7.11 stays unticked until 11b.
- 10b: Not split (owner). Figma read via the Console MCP (R15-3..6, notes).
- 10b: Avatars are Fluent Emoji 3D PNGs (Flat exists only as SVG; Figma
  shows 3D), scaled to 144 px with sips (no webp tool), 448 KB in
  `res/drawable-nodpi/avatar_<id>.png`. Ids: fox cat dog panda bear koala
  tiger lion frog monkey pig rabbit hamster owl penguin unicorn dragon
  wolf sloth otter. Licence row + assets/licenses/fluent-emoji-MIT.txt.
- 10b: Body weight page lists the entries in the chosen range (no "All
  entries" link/screen). Chart needs 2 points; fewer shows a line of text.
  Change line only once an entry is 30+ days older than the latest.
- 10b: Experience uses New / Some / Experienced (onboarding words), not
  Figma's "Intermediate". Footer says "Used later for BMI…" (no "(v2)").
- 10b: Height and target weight edit in a Keypad sheet (Save, Clear);
  birth date in an M3 DatePickerDialog (Clear, Cancel, OK), stored as UTC
  midnight. Gender/activity/goal sheets have "Not set". Bio and the old
  Units/Weight edit screens are gone (units live on the You page).
- 10b: Log date other than today saves at noon that day; future days off.
  Undo on delete re-saves the same entry (upsert clears the tombstone).
- 10b: `Group`, `RowDivider`, `ValueRow`, `ChoiceSheet` moved to
  `ui/you/YouRows.kt`; `Avatar` in `ui/you/Avatars.kt` (also on the You
  card). `SignInInfo` (open, Firebase) gives the email line. Both new VMs
  use `SharingStarted.Eagerly` (actions read `uiState.value`).
- 10a: Decisions 1–6 as recommended (owner). Figma read via the Console MCP.
  Rows left out: Distance (one Units row), Default rest, Weight step, Keep
  screen on, Week starts on, Starter plans, Show tips again.
- 10a: Delete account has no keep/erase radio; body says the phone is wiped
  too. Re-auth: Google-only → Google at once; else a password dialog.
- 10a: Sign out asks (R15-8) only when changes are waiting; nothing waiting
  signs out at once. Offline (R15-13) disables "Back up and sign out". A
  failed backup re-asks with "Couldn't back up just now".
- 10a: Backup line from the outbox (count + oldest `enqueuedAt` > 24 h =
  failed) and `SyncScheduler.status.lastSyncAt` (in memory, so "Backed up"
  without a time after a restart). No new DAO query.
- 10a: Card shows a letter avatar; animals come in 10b. Export sub reads
  "A file you keep (JSON)" (CSV dropped). About = version, feedback mail,
  Sync debug (debug builds); no licences link yet.
- 10a: Quote-of-the-day pref deleted (nothing showed it). `AppChoiceDialog`
  got a content slot and `DialogAction.enabled`. `OnlineCheck` added.
- 10a: Hidden lists: new page, "Unhide all" only for exercises (no repo call
  for templates). VMs moved to ui/you. `SettingsSection` moved into the old
  ProfileScreen until 10b.
- 09b: Owner OK: an exercise whose sets are all swiped away is dropped on
  save (exercises can't be removed otherwise).
- 09b: Save goes through `UpdateWorkoutSessionUseCase` (validates via
  `ValidateWorkoutSessionSetsUseCase`, then `updateWorkoutSession`).
- 09b: Unchanged sets are saved as stored (no lb → kg drift); edited sets
  keep their id, set type and rest taken.
- 09b: Pickers are M3 `DatePickerDialog` and `TimePicker` in a
  `BasicAlertDialog` (no design-system picker exists). An end time before
  the start rolls to the next day. A changed date moves start and end
  together.
- 09b: `NameField` from the template editor made internal and reused.
  Card line in Edit is computed from the edited sets and hidden while
  the card is open (Figma R14-4).
- 09: Split into 09a/09b (owner, decisions 1–5 as proposed). Figma read
  via the Console MCP. Roadmap 7.9 stays unticked until 09b lands.
- 09a: ⋯ has no "Edit workout" until 09b; past sessions can't be edited
  for one step. Delete pops the screen, then tombstones at once; Undo =
  new `restoreWorkoutSession` (DAO UPDATE, outbox UPSERT; no schema or
  spec change). A Mutex keeps Undo behind its delete.
- 09a: Also deleted `SetTrackingFields.kt` (only WorkoutScreen used it)
  and `WorkoutSessionViewModelTest`.
- 09a: Distance tile in km/mi (one decimal), pace per km/mi; set rows in
  the open card use bodyMedium, not a tabular style (none exists).
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
- 06a (fix after owner device check, Figma not binding): Done is 56dp
  (`ButtonSize.Large` removed; summary Done too). ValuePanel: number
  `displayMedium` (44) only when the panel is ≥ 176dp wide, else
  `headlineLarge` + tnum; padding 12, − / + keys share the panel width
  (48 high). Panels stack only below 264dp card width (was 328).
  Number box at least "00.0" wide, right-aligned, so the unit stays put;
  unit baseline lined up by measuring (`baselineShift`), since
  AnimatedContent passes no baseline. "Up next" hidden after the last
  exercise; progress pill hidden (`onProgress = null`) until 06b.

- 06b: Split: rest buzz + lock screen notification moved to 06c (owner
  said "go" to the plan that proposed it). Roadmap 7.6 stays unticked
  until 06c lands.
- 06b: Figma read via the Console MCP. The live bar's rest text comes from
  85:10033 ("Set 3 of 4 done · rest 1:12", then "rest +0:45"). It counts
  down, while the in-workout rest line counts up as before. No rest text
  once every set is done.
- 06b: All exercises sheet lists every exercise (scrolls); Figma's
  "+ 5 more" left out. Reorder = the sheet in reorder mode with Move
  up / Move down buttons per row (chevrons), not Figma's drag dots. ⋯ →
  Reorder exercises opens that mode. Reorder hidden with < 2 exercises.
- 06b: Added exercises get the editor defaults: 3 sets, 10 reps where reps
  count, no weight, rest = default (2:00). The picker marks the workout's
  exercises as "In template" (reused `ARG_IN_TEMPLATE`).
- 06b: Rest timer = titleless Keypad sheet (m:ss from the right, like 05b),
  changes only this workout's exercise (template unchanged) and a rest
  running after one of its sets.
- 06b: Workout note = sheet with a multi-line AppTextField + "Save note";
  saved trimmed as `WorkoutSession.notes` (blank → null). Draft JSON got
  `note` (default ""); no Room or spec change.
- 06b: `ActiveWorkoutUiState.running` = (exercises or start time) and not
  saved/discarded; `hasActiveWorkout()`, live bar, Home card and draft
  persist/restore all use it (05a gotcha closed). Empty workout title
  "Workout", no templateId.
- 06b: Home card "Workout in progress · title · Started 18:24 · N of M
  exercises done" (locale short time), tap opens the workout. Start sheet
  "+ Empty workout" is the first row.
- 06b: "Add set" = lime text link under the set list, copies the last
  set's values.

- 06c: Figma read via the Console MCP. Notification text follows 53:3470,
  not the step file: title "Bench Press · set 4 of 4", text "82.5 kg × 8",
  subtext "Push A" ("· rest" / "· paused"). One system chronometer
  counting up: the rest while resting, else the workout clock. No
  countdown, no per-second posts. Empty workout: "No exercises yet".
- 06c: Buzz timer lives in `WorkoutServiceController` (@Singleton, binds
  `WorkoutServiceStarter` + `RestBuzzScheduler`), not in the Service; the
  service only keeps the process in the foreground. Holds a PARTIAL wake
  lock until the buzz (new `WAKE_LOCK` permission), or a locked phone
  could sleep through the rest.
- 06c: The VM doesn't call schedule/cancel per action: it derives them from
  its state (`buzzTarget()`, `toNotice()` + distinctUntilChanged). Covers
  start, change, delete/undo, pause, finish, discard, buzz off. The service
  follow skips leading nulls, so a new VM doesn't stop a restored workout.
- 06c: "Open" (and tap) brings the app task back like the launcher; no
  deep link to the workout screen (live bar is one tap).
- 06c: Notification icon `ic_notification_workout` is our own drawing of
  ring 53:3478 (two circles), white, tinted lime. DESIGN.md §4 rows for
  the buzz ms and the ring; licence table updated.
- 06c: `liveExercise()` / `liveRest()` pulled out of LiveBarState for the
  notice. `setValueText` / `unitText` got `Resources` variants.
- 06c: Rest timer sheet has the switch on top; state follows the
  DataStore flow (no optimistic update, avoids a flip back on first read).

- 07: One step, not split (owner). Figma read via the Console MCP.
  Decisions 2–6 as recommended.
- 07: Templates keep a deleted exercise. `StampTemplateExerciseSnapshots`
  and `ValidateWorkoutSessionSets` now look exercises up including deleted
  ones (`getExercisesAnyState`), or templates/workouts with one couldn't
  save (reviewer BLOCKER, fixed). The workout starts a deleted exercise from
  the template snapshot (name, tracking type).
- 07: Restore = `ExerciseDao.restoreExercise` (deletedAt null) + a plain
  UPSERT outbox push; the server's update clears deleted_at. No spec change.
- 07: New queries only (no Room schema change): `observeExercise`,
  `getExercisesAnyState`, `restoreExercise`, user state `observeAll`,
  `observeState`, `setDefaultRest`. Domain `ExerciseUserState`.
- 07: "Progress shows after 3 sessions." left out with the chart (7.8).
  History rows and "All N sessions" not tappable/shown until 7.8/7.9.
- 07: Tracking chips are Weight + reps, Reps only, Time, then "More" on
  both create screens (android/CLAUDE.md: not six equal choices); Figma
  Create A shows all six. Equipment: 7 chips + More.
- 07: Hidden fields: Difficulty and Kind (compound/isolation) editable under
  More details; Category isn't shown (Distance + time → Cardio, else
  Strength; an edit keeps its category). No main muscle → Other.
- 07: Create A has no discard dialog (× just leaves). New exercise Save
  opens its page in the editor's place; Edit pops back to the page.
- 07: Your rest per exercise is used when it's added (editor add/swap,
  workout add, ⋯ Add to template); template rest still wins on start.
- 07: Add to template = sheet of your own templates (no starters), appended
  with editor defaults.
- 07: Hide from lists pops the page with a snackbar (no Undo; Settings ›
  Hidden exercises brings it back). Hidden screen got its own small
  `HiddenExercisesViewModel`; old WorkoutScreen now uses `ExercisesViewModel`.
- 07: Workout one-note line: plan note first, your note behind the note
  icon (cobalt dot when set), only yours → the line opens it. Template note
  is in ActiveExercise + draft JSON (`templateNote`, default null). "How to"
  in the workout still hidden.
- 07: Swap on a deleted row opens the picker; the first pick replaces the
  card (same tracking type keeps the targets).
- 07: Photos load from free-exercise-db URLs with Coil, nothing shown on
  failure. DESIGN.md licence row: Unlicense, but the photos' original
  source is unknown; confirm or replace before release.
- 07: Strings in `res/values/strings_exercises.xml` (quotes must be `\"`).

- 08: One step, not split (owner). Figma read via the Console MCP.
  Decisions 2–6 as recommended.
- 08: Rows open the old `WorkoutSessionFormDialog` through
  `SessionEditBridge` (WorkoutScreen.kt), state in MainScreen
  (`editingSession`). WorkoutScreen's list, sort menu, FAB, delete dialog
  and pull-to-refresh are gone; manual past-session logging and manual
  sync from this screen go with them. `WorkoutSessionViewModel` not
  trimmed (step file said it would be); 09 deletes it with the dialog.
- 08: "Start a workout" (R7-3) goes to the Workouts tab, not the start
  sheet: that sheet is internal to HomeScreen.
- 08: "N weeks ago" is min 1 week, not 2 as decision 5 said (a thin window
  can span 1 week); weeks = whole weeks between first and last point.
- 08: Chart points are evenly spaced (Figma), not by date. Sparkline on
  the card, full `LiftChart` (3 y ticks, 3 dates, drag readout) on the
  exercise page only. At font scale ≥ 2 the card drops the line and
  "Change lift" becomes a button; the page drops the chart.
- 08: Exercise page: chartable lifts show the Progress block (sentence,
  chart, Show on Progress, window rows newest first with Latest tag)
  instead of History; others keep History (3 rows). Rows open the
  session; "All N sessions" link opens All sessions filtered. Route
  `exercise/{id}?atProgress=` scrolls to the block once.
- 08: Show on Progress off = the most-logged other lift is stored; the
  switch is disabled when this lift is the only one.
- 08: All sessions exercise filter = a sheet of exercises you've logged
  (most sessions first), not the exercise picker; ✕ is an icon button
  beside the cobalt chip. Filter with no matches: "No sessions with X"
  + Clear exercise filter.
- 08: Progress uses `ZoneId.systemDefault()` for days; `Clock` is UTC.
- 08: Chosen lift = DataStore `progress_lift_id` (device pref, like
  units). `ui/SessionsScreen.kt` deleted. Strings in
  `res/values/strings_progress.xml`.

## Gotchas
- From 11g: Render never deploys by itself. After any backend change on develop, ask the owner
  to Manual Deploy before a phone check. Quick stale-server sign: logcat SyncEngine
  "reference data check failed ... missing". Step 12: redeploy before the device pass.
- From 11g: a scratch adb tap-by-text helper (uiautomator dump + bounds) works well for phone
  checks; the debug DB copies out with `run-as ... cat databases/<file>` (+ -wal, -shm).
- From 11f (reviewer, not fixed):
  - Unused strings now: summary_time, summary_lifted, session_km, session_mi,
    session_distance. Delete in the next step.
  - No tests for snackbar pause-on-touch, line only with action, or F21a reveal/tap/long swipe.
  - DeleteRevealWidth = 88.dp is a raw dp in ActiveWorkoutScreen; move to a token/ui/design.
  - Tapping the Delete strip plays SwipeThreshold haptic twice.
  - Positional threshold is 50% of the distance to the next anchor: Done needs ~half the row
    width (was 56 dp) unless flung. Check feel.
  - A revealed row only closes by swiping back, not by tapping it or opening another row.
  - Compose UI tests with an Undo snackbar must pause mainClock (autoAdvance = false) before
    the action, or idling runs the 8 s timer out (see ActiveWorkoutUiTest swipe test).
  - (done 990fcb7) android/CLAUDE.md Verification now allows phone checks; was:
    "no adb driving".
- From 11e (reviewer, not fixed):
  - F11 eye has no Compose test.
  - Bars.kt fade uses `colorScheme.surface`; tab pages paint
    `background`. Same token today (Ground).
  - The fade draws outside the bar's bounds: needs an unclipped parent
    with the list as a sibling (true at all 4 tab roots; not in a
    Scaffold topBar).
  - F2: the Fluent Emoji MIT credit is no longer shown in the app (the
    text still ships in assets/licenses/). Plan 9's licences page must
    list it.
- From 11d (reviewer, not fixed):
  - The rule snackbar (F22) dismisses a showing Undo snackbar: delete a
    set, then Done on an invalid set, and Undo is gone.
  - Weight − still floors at 0 kg, a value Done rejects (same as distance).
  - F19 scroll and F21c Undo have no Compose tests; check on device.
- From 11d: the F21c cause was LazyColumn restoring saveable item state by
  key. Any `remember*Saveable` state inside a keyed lazy item comes back
  when that key returns.
- From 11c, F1/F12 (avatar reverts, backup "not possible"): server and
  client both store avatarId fine. Leads: the profile push comes back
  CONFLICT (local revision behind server, e.g. after guest merge or a
  locally created row) and SyncEngine.sync's full pull then overwrites
  the row, or INVALID (blank name/email, ProfileSyncService.validate).
  Owner: read the reason in the debug Sync screen or Render logs
  (SyncAudit, entityType=USER_PROFILE). Render sleep is ruled out
  (UptimeRobot 5-min ping); /v1/health doesn't touch Neon, which can
  still suspend. Hosting move stays after v1 (roadmap Plan 5).
- From 11c (reviewer, not fixed):
  - Overlays.kt comment on insets is wrong: M3's own imePadding eats the
    keyboard inset; the nav padding drops to 0 while the keyboard is up.
  - Sheets now open full height: long ones (avatar picker, add-to-template
    ExerciseSheets.kt:165, start sheet HomeScreen.kt:432) need their own
    scroll if taller than the screen.
  - F10 changes the fill only (SurfaceRaised on SurfaceHigh); unfocused
    fields still have no border. Check contrast on device.
- From 11b (reviewer, not fixed):
  - `catch (e: Throwable)` in `SyncEngine.pullLocked` also marks a
    cancelled re-download Failed.
  - The day count only refreshes when the outbox or Retry emits; Home
    left open never ticks over to 3 days.
  - BackupFooter isn't merged for TalkBack (text and Retry are two stops;
    notes want one).
  - No Compose test for the resync card, rows or Start sheet states.
- From 11a (reviewer, not fixed):
  - Finish on Home fails silently: with 0 sets, or if the save fails,
    `rejectSave` sends `SaveFailed` on the workout events Channel, which
    only the workout screen reads. The event fires later (on Resume) and
    `finishingFromHome` stays true (the next finish navigates twice,
    harmless with launchSingleTop). Fix: hide Finish at 0 sets, reset
    the flag, and show the error on Home.
  - No SyncEngine-level test that 410 keeps the draft (DAO-level only);
    no test for the finishUnfinished failure path.
  - A failed `DraftWrite.Clear` is now swallowed (it used to crash the
    app); a stale draft could come back after a save. Add a log line or
    a retry.
  - The banner's set number is read when a write fails, not when it
    was made (quick taps can name a later set).
- Use (11b): `SyncDao.wipeForResync()` / `SyncLocalStore.wipeForResync()`
  is the 410 wipe (keeps the draft); `wipeLocalUserData()` = it + draft.
  `ActiveWorkoutUiState.unfinishedSince` marks a stale restore.
- From 10b (reviewer, not fixed):
  - In lbs, holding + reaches unrounded kgToLbs(700) ≈ 1543.24, which
    converts back over 700 kg: the repo `require` fails and Save shows
    "Couldn't save". Round the lbs bounds.
  - No Compose test for body weight swipe-delete + Undo; no test for the
    SaveFailed path.
  - Raw dp 96 (page avatar), 32 (badge), 56 (picker cell), 80 (letter
    size switch) aren't in DESIGN.md §4.
  - Events use tryEmit with buffer 1: quick repeat deletes during a
    snackbar can drop an Undo. A failed delete leaves the row swiped away.
  - Birth date picker allows future dates.
- From 10a (reviewer, not fixed):
  - `busy` in DeleteAccountFlow/PasswordDialog is rememberSaveable: rotate
    mid re-auth → dialog stuck disabled. Use remember or move to the VM.
  - No Compose test for DeleteAccountFlow (password vs Google, error).
  - No licences link in About (CLAUDE.md licence rule): add or track.
  - Guest erase failure is only logged; Export first closes the erase
    dialog for good; loadCounts/loadDeletionWarning have no try/catch.
- 10a, for 10b: Profile row still opens the old ProfileScreen (+ EditUnits
  route). YouUiState has `avatarId`; swap `LetterAvatar` in YouScreen for
  the picker's avatar. Delete ui/profile/** and the Edit* routes.
- From 09b (reviewer, not fixed):
  - Every save sets durationSeconds = end − start, even for a rename
    (WorkoutSession says duration isn't derived). Keep the stored one
    unless times changed.
  - Save re-stamps exercise snapshots from today's exercise (use case
    behaviour): a renamed exercise renames history; a changed tracking
    type makes old sets fail validation ("Couldn't save").
  - No test for the save-failure path; no Compose UI test for Edit.
  - Deleting every set saves an empty workout. No future-date limit.
    Edits aren't in SavedStateHandle (lost on process death).
- From 09a (reviewer, not fixed):
  - `IndexColumn = 20.dp` raw dp in SessionDetailScreen (not a token).
  - No UI test for the history highlight (open + scroll + 1.5 s outline).
- 09a, for 09b: add "Edit workout" back to the ⋯ menu in
  `SessionDetailContent`; route `session/{id}/edit`. Update/validate
  through `ValidateWorkoutSessionSetsUseCase` + `updateWorkoutSession`.
  Detail cards are keyed by PerformedExercise id; reuse `exerciseLine`.
- From 08 (reviewer, not fixed):
  - No tests for the sentence variants (more/same/less), units in the
    week line, `inlineSets` or the session subtitle (Compose text).
  - Chart drag: if `pointerInput` restarts mid-drag (data change), `active`
    isn't reset; reset it in try/finally.
  - `SessionEditBridge` with a missing session leaves `editingSession` set.
  - All sessions exercise filter isn't in SavedStateHandle (lost on
    process death).
  - Week line buckets by `doneAt`, months by `startedAt`.
  - AppPreferencesRepository: progressLiftId KDoc sits between
    `notificationsAsked`'s KDoc and its val.
- 08: Home's `HomeViewModel` uses `clock.zone` (UTC) for the week strip,
  so Home and Progress can disagree near midnight.
- 08, for 09: delete `SessionEditBridge`, `WorkoutSessionFormDialog`,
  `WorkoutSessionViewModel` and WorkoutScreen.kt; point `openSession` in
  MainScreen at the new session route.
- From 07 (reviewer, not fixed):
  - No Room/outbox tests for the new repository calls (restore over a
    pending DELETE, setExerciseRest, observeUserState(s), any-state read).
  - Workout note line isn't limited to one line (no maxLines/ellipsis).
  - Note dot is cobalt; cobalt is meant only for "you are here". Check
    against R13-13 or switch it.
  - With neither a plan note nor your note, a workout can't add your note.
  - `ExerciseHistory.SetOrder` ranks time before distance: for Distance +
    time, Best is the longest, not the farthest set.
  - Swap with several picks keeps the first only; `swapKey` is lost on
    process death (the pick is appended instead).
  - Editing an exercise's tracking type leaves old template snapshots on the
    old type until each template is saved again.
  - DESIGN.md §10: the new photo row sits after a blank line, so it renders
    outside the table. Photos are a runtime call to GitHub.
- 07, for 7.8: put the Progress block (chart + "Show on Progress") into
  `ExercisePageContent` after the note; history entries are
  `ExerciseHistory.of(id, sessions)`. "All N sessions" → All sessions
  filtered by exercise. For 7.9: make `HistoryRow` open the session.
- 07: in Robolectric, click buttons inside a ModalBottomSheet through
  `performSemanticsAction(SemanticsActions.OnClick)`; performClick can miss.
- 07: workout VM tests must not use `runTest` (it drives the endless clock
  loop and hangs); use plain tests or runBlocking.
- From 06c (reviewer, not fixed):
  - The workout clock ticks with `delay(1000)` and stops while the phone
    sleeps, so the notification chronometer base (`now - elapsed`) jumps
    back after a locked spell. Derive elapsed from the wall clock
    (start + pauses) instead of counting ticks.
  - Wake lock race: an old buzz job past its delay can release the lock a
    new `schedule()` just took (single non-counted lock). Release only if
    still the current job.
  - Chronometer base drops part-seconds: may shift up to 1 s per notice
    change (e.g. stepper taps).
  - If the VM is cleared mid-workout and the new one finds no draft,
    nothing sends null; the notification stays until process death.
  - `askNotificationsOnce` stores "asked" before launching; if the screen
    leaves in between the one ask is lost. No test for it.
  - `specialUse` FGS needs a Play Console justification at release (5.x).
- From 06b (reviewer, not fixed):
  - No Home tests for "+ Empty workout", the in-progress card or
    `toRunningWorkout` (0-set exercise not counted as done).
  - Picker says "In template" for exercises already in the workout.
  - Reordering the shown exercise changes `currentExerciseIndex`, so the
    same exercise slides in sideways (AnimatedContent keyed by index).
  - Empty workout shows a disabled "Next exercise" under "+ Add exercise".
- 06c: rest state is `RestState(afterSetId, startedAtElapsed, seconds)` on
  the workout clock; `LiveBarRest` / `liveBarRest()` already compute the
  remaining time. Pausing the clock freezes rest (06a gotcha).
- Robolectric fakes text metrics (every text ~4px wide, same height):
  baselines and text widths can't be asserted in Compose UI tests. Test
  the maths as plain functions; layout tests by dp widths are fine.
- 06b: give `WorkoutTopBar(onProgress = …)` back when the sheet lands.
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

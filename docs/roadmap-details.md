# Roadmap: details

Re-plan of 2026-10-29, re-ordered 2026-11-02. The checklist is
[roadmap.md](roadmap.md); this file explains each item. It replaces
§13 (execution plan) and §17 (next actions) of
`architecture-and-execution-plan-v2.md`. The rest of that document
(decisions, data model, sync protocol, scope freeze §14) still holds.
iOS is out of scope until Android v1 is live.

## 1. Where we are

| Phase | State |
|---|---|
| 0 Monorepo | Done |
| 1 Schema hardening | Done. Group K favourites and rest timer are v2 (§14.2, Q-07) |
| 2 OpenAPI contract | Done. All 8 endpoints in the spec |
| 3 Backend | Done, deployed on Render + Neon at api.rukavina.app |
| 4 Android sync | Done (Plan 1). Device checks passed 2026-10-31 |
| Data safety | Done (Plan 2): migrations, Crashlytics, JSON logs |
| Redesign in Figma | Done (Plan 3): Soft Dark, every screen locked, DESIGN.md |
| Android v1 | Plans 4–10 below |
| iOS | Later |

Last merge to main: 4349324 (PR #4, 2026-10-31). develop is 44
commits ahead (Plan 3 docs only).

### Why this order

1. **Code health (4)** first: known bugs and CVEs, before new code
   lands on top of them.
2. **Environments (5)** before features: guest mode, account switching
   and the redesign are tested against staging, never against prod data.
3. **Features (6)** before the redesign: the new screens need their
   data (per-account data, guest, body weight, profile, plans, export).
   Each feature step leaves the old UI working, with at most a plain
   entry point.
4. **Redesign (7)** builds every screen once, on top of 6.
5. **Delivery (8)**: CI/CD and fastlane once there is something worth
   shipping; testers get staging builds from here on.
6. **Legal (9)** describes the finished app: the policy and Data safety
   form must match what the shipped build collects.
7. **Release (10)** last: internal → closed → production.

## 2. How we work

| Piece | Where | Role |
|---|---|---|
| Rules | `CLAUDE.md`, `android/CLAUDE.md` | Branching, contract, conventions |
| Roadmap | `docs/roadmap.md` + this file | All plans and steps, done and to do |
| Plans | `docs/plans/<name>/`, `ACTIVE` | Overview, one file per step, progress.md |
| `/step` | `.claude/skills/step` | Runs one step: plan, "go", build, test, review, commit, push |
| `reviewer` | `.claude/agents/reviewer.md` | One read-only review per step, BLOCKER/SHOULD-FIX/NIT |
| Guard hook | `.claude/hooks/guard-bash.sh` | Blocks force push, push to main, deletes, secrets, deploys |
| Permissions | `.claude/settings.json` | Gradle/git allowlist, secret files denied |
| Figma | Figma Console MCP (Desktop + Bridge plugin) | Source of truth for all UI |
| Remote | claude.ai/code from the phone | Same flow; "go" is approved from the phone |

### The loop

1. A plan folder is written when its plan starts (overview, step
   files, progress.md), reviewed by me, then `ACTIVE` points at it.
2. `/step` runs the next step that isn't ticked or deferred.
3. After each step, `/step` ticks it in both `progress.md` and
   `docs/roadmap.md`, in the same commit.
4. When a plan is done, `/step` says which plan is next. The last step
   of each plan is a PR develop → main once CI is green.

### Step size

- One commit, roughly up to 600 changed lines including tests.
- May cross backend and Android when the feature does, but at most
  one Room version bump.
- Has a "Done when" with exact commands, and a manual check list for
  what tests can't prove.
- Manual-only work is its own "(me)" step. One I can't do yet is
  marked "(deferred)" and `/step` skips it.

### Rules carried forward

- Out-of-scope list in every step file (v2 plan §1.2).
- Spec change, Room schema change, Flyway migration: called out in the
  step file and in the commit message.
- The hook blocks deploys, so deploying by hand is always a "(me)"
  step. CI deploying on push (8.4) is fine.
- UI steps link a Figma frame. No frame, no UI change.

## 3. The plans

### Plans 1–3 (done)

- **Plan 1, phase4-closeout:** account deletion end to end, sync debug
  screen, Render + Neon deploy with nightly backup, api.rukavina.app,
  device checks, PR to main.
- **Plan 2, phase5-data-safety:** one DB builder, v26 baseline with
  migration tests, no destructive fallback, Crashlytics, JSON logs,
  client version headers (spec change), PR to main.
- **Plan 3, figma-foundation:** full redesign (Soft Dark) instead of
  the planned "mirror v1": tokens, brand + icons, 35 components, every
  screen locked, motion spec, `design/DESIGN.md`. Details in its
  progress.md.

### Old ids → new ids (2026-11-02)

| Old | New | Old | New |
|---|---|---|---|
| 4.0 | 4.1 | 4.D1–4.D11 | 7.1–7.11 |
| 4.1 | 4.3 | 4.3 | 8.2 |
| 4.2 | 4.2 | 4.4 | 10.1 |
| 4.2a | 6.3 | 4.5 | 9.2, 9.5 |
| 4.2b | 4.4 | 4.6 | 9.3, 9.4 |
| 4.2c | 6.2 | old 5.1 | 10.3 |
| 4.2d | 6.7 | old 5.2 | 10.4 |
| 4.2e | 6.4 | old 5.3 | 10.5 |
| 4.2f | 6.5 | | |
| 4.2g | 6.1 | | |
| 4.2h | 6.6 | | |

Plan 3 docs, council notes and older progress files keep the old ids.

### Plan 4: phase5-code-health

4.1 **OWASP.** Dependabot shows 35 alerts, all fixed (2026-11-02).
    Run OWASP dependency-check for backend and Android, triage each
    finding (bump, replace, or suppress with a written reason), then
    restore OWASP's push/PR triggers (manual-only since 2026-10-30).
    Stays non-blocking (ADR 0013) unless I say otherwise.
4.2 **ImageStorageUtil.** Write the new file first, then delete the
    old one; close streams with `use`.
4.3 **ViewModel tests.** Audit the existing ViewModel tests, then
    cover the active workout state machine's failure paths (rejected
    save, resume after process death). The ViewModel survives the
    redesign; only its screen is replaced.
4.4 **Durable push.** Swiping the app away right after an offline edit
    kills the network callback, so the edit waits for the next app
    open. When the outbox becomes non-empty, enqueue a unique one-off
    WorkManager job with a CONNECTED constraint that pushes; it
    survives process death. Found in Plan 2 T10.
4.5 **(me)** CI green, PR develop → main.

### Plan 5: environments (test + prod)

Today there is one backend, one database and one Firebase project, and
debug builds talk to it. From Plan 6 on, nothing under test touches
prod data.

5.1 **ADR 0016.** Staging and prod each get their own Render service,
    Neon database (or branch), Firebase project and API host
    (api-staging.rukavina.app, api.rukavina.app). Check free-tier
    limits (Render services, Neon branches) at write time. Debug and
    staging builds point at staging; only prod release builds point at
    prod.
5.2 **Backend.** Staging service in `render.yaml` (same image, own
    env vars), nightly backup stays prod only, `docker/README.md`
    documents both. Spec: add the staging server. **Spec change.**
5.3 **Android flavors.** `staging` and `prod` product flavors:
    `applicationIdSuffix ".staging"` (both installable side by side),
    own Firebase config per flavor, own API URL, app name "GymBuddy
    Staging". The `-Pgymbuddy.apiBaseUrl` override stays. CI builds
    and tests both; secrets per flavor.
5.4 **(me)** Create the staging Firebase project (Auth: Google +
    email), the Neon branch, the Render service, the DNS record, and
    the GitHub secrets. Deploy staging.
5.5 **(me)** CI green, PR develop → main.

### Plan 6: v1 features (data + logic)

Each step leaves the current UI working. New screens come in Plan 7;
until then a feature may have a plain entry point or none.

6.1 **Per-account local data** (ADR 0014). One local store per
    account plus one for the guest; switching accounts never shows or
    uploads another account's data. Room schema change.
6.2 **Guest mode** (ADR 0014). The app works fully without an
    account. Creating an account or signing in merges guest data into
    that account once.
6.3 **Auth gate.** One place watches Firebase auth state and sync 401s
    (`getIdToken(true)` tells expired, deleted and offline apart).
    Expired: the user keeps logging, the outbox pushes after re-sign-in
    as the same account. Deleted elsewhere: keep as guest or erase.
    No forced Login, no wipe. UI in 7.4.
6.4 **Body weight.** New table, sync, spec. **Spec change, Flyway
    migration, Room schema change.**
6.5 **Profile fields.** Experience, avatar (Fluent Emoji animals,
    MIT), consent record, days per week. **Spec change.**
6.6 **Plans.** A plan is a set of templates to rotate through. Rework
    the default templates and add starter plans. Data model change.
6.7 **Data export** (ADR 0015). Versioned JSON + CSV through the
    system share sheet.
6.8 **(me)** CI green, PR develop → main.

### Plan 7: redesign build (Compose)

Read `design/DESIGN.md` first. Each step links its Figma section and
reads it through the MCP before code. 7.1–7.3 come first, in order.
Old screens are replaced, not restyled. Stay on v1 features (Plan 3
invariants) plus what Plan 6 added.

7.1 **Theme.** Color.kt/Type.kt/Theme.kt from `design/tokens.json`
    (dark only), Shapes (radius.full → CircleShape), stat-value and
    tag roles, `fontFeatureSettings "tnum"` on the styles named in
    DESIGN.md, motion tokens (rows without easing use `std`), haptics
    via Compose `HapticFeedbackType` with own API fallbacks. Raw sizes
    used as drawn (DESIGN.md list). Figma: https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=124-26794 (Tokens),
    https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=153-8784 (Motion). Specs: colors.md, motion.md.
7.2 **Icons + components.** design/icons.md + components.md, 35
    components on https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=151-3214, icons https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=137-26976. Material icons tint
    fills, own glyphs/tabs tint strokes, filled tab icons never
    tinted. `ModalBottomSheet` keeps M3 defaults where tokens can't
    be set. Replaces ui/components/* where a library part exists.
7.3 **App frame.** https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=85-9890. 5 tabs (Home, Workouts, Exercises,
    Progress, You), top bars, title collapse, tab fade-through.
    Navigation Compose 2.8+ predictive back (no
    `PredictiveBackHandler`). Replaces MainScreen.kt nav.
7.4 **Onboarding + auth.** https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=114-20520. Welcome, setup (no Next
    buttons), sign-in, create account with consent, session expired
    banner, account deleted elsewhere, Ready. UI for 6.2 and 6.3.
    Replaces SplashScreen, LoginScreen, RegisterScreen.
7.5 **Home + Templates.** https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=87-11340, https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=92-12488. Week strip on
    Home, plans from 6.6. Replaces HomeScreen, WorkoutsScreen,
    CombinedWorkoutsScreen, WorkoutTemplateScreen + dialogs.
7.6 **Active workout.** https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=67-6561. Keypad sheet, − / + stepper,
    live bar from ⌄, swipe done/delete + Undo, rest card. Set lands:
    `sharedBounds` or one node swapping content, not `animateBounds`.
    Drag reorder needs a library (check licence) or own code. Rest
    buzz: VIBRATE permission, a notification channel for DND. Done +
    couldn't save = two pulses (decide). 1 s outline hold and
    80/120/80 buzz are raw ms (keep). Replaces ActiveWorkoutScreen,
    SetTrackingFields, NumberPicker, and WorkoutScreen's active part.
    WorkoutScreen's Sessions tab and session dialog stay until 7.8/7.9.
7.7 **Exercise page + create.** https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=97-14553. Replaces
    ExerciseScreen, ExerciseDetailsScreen, ExerciseCreationBottomSheet,
    ExerciseWizardDialog.
7.8 **Progress tab.** https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=62-5195. Counts line, one lift chart, All
    sessions search. Replaces SessionsScreen + StatisticsScreen.
7.9 **Session detail.** https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=107-15763. Replaces WorkoutScreen's
    session dialog; WorkoutScreen goes here.
7.10 **You + Settings.** https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=110-17469. One settings page, backup
    line, delete account (keep or erase), export (6.7), body weight
    (6.4), profile fields + animal avatars (6.5). Replaces
    ProfileScreen, SettingsScreen, AppPreferencesScreen,
    Hidden*Screen, AboutScreen.
7.11 **Cross-flow states.** https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=124-26620. Empty, error, offline,
    sync. Sets logged during a re-download (410) must survive the
    wipe. Replaces EmptyState, LoadingState, AppSnackBar,
    UnsyncedChanges where they differ.
7.12 **(me)** Device pass on a staging build, CI green, PR.

### Plan 8: delivery (CI/CD + fastlane)

8.1 **CI gates.** Run Android Lint in CI (baseline the current
    warnings, fail on new errors). Revisit the SonarQube gate (Q-E).
8.2 **Release workflow.** On a `v*` tag: build a signed, minified prod
    AAB, versionCode from the tag, Crashlytics mapping upload on.
    Signing secrets in GitHub secrets.
8.3 **fastlane** (MIT). Lanes: `staging` (staging APK to testers via
    Firebase App Distribution), `internal` (prod AAB to the Play
    internal track), `promote` (internal → closed). Store listing text
    and screenshots in `android/fastlane/metadata`. CI runs the lanes;
    I run them by hand only as a fallback.
8.4 **Backend CD.** Push to develop deploys staging, a `v*` tag
    deploys prod (Render deploy hooks from CI). Flyway runs on start,
    as today.
8.5 **(me)** Play Console developer account and app, Play App Signing
    with an upload key, a Google Cloud service account for fastlane,
    all secrets in GitHub.
8.6 **(me)** CI green, PR develop → main.

### Plan 9: legal + privacy

9.1 **Licences.** Every third-party asset and library with source and
    licence (CLAUDE.md rule), including the stock exercise photos
    (not recorded yet; DESIGN.md §10). An in-app open-source licences
    page, reached from About.
9.2 **Privacy policy + terms** (Q-11). Written in the repo, published
    at a public URL on rukavina.app, linked in-app and in Play.
9.3 **Web account deletion.** Play requires a URL where users can ask
    for deletion without the app.
9.4 **Compliance pass.** Health-data consent, 16+ audience,
    Crashlytics payloads (no PII), timer/notification rules. Checklist:
    docs/council/2026-11-02-onboarding.md.
9.5 **(me)** Play Console: Data safety (Firebase Auth + Crashlytics),
    content rating, target audience, store listing, privacy URL.
9.6 **(me)** CI green, PR develop → main.

### Plan 10: release

10.1 **(me)** Release build manual pass, staging then prod: R8,
     sign-in, guest, sync, logout, account deletion.
10.2 **(me)** First prod AAB on the internal track via fastlane.
10.3 **(me)** Closed testing. Check the current tester rule (it was 12
     testers for 14 days on new personal accounts) before setting a date.
10.4 Fix what testers find, one step per batch of issues.
10.5 **(me)** Full manual pass, then production release. Android v1
     is done; iOS planning starts, with designs already in Figma.

## 4. Hosting

Goal for now: free, easy to set up, easy to leave. Later: paid
hosting of my own (e.g. DigitalOcean).

**Choice: Render free web service (Docker) + Neon free Postgres.**
Check both free tiers at setup time, since they change. Staging (Plan
5) uses the same pair.

- Render builds `backend/Dockerfile` as is. No buildpack, so nothing
  ties us to Render (§12.3).
- Neon is plain Postgres: `DATABASE_URL` + `DB_PASSWORD`, nothing
  else. `pg_dump` works. EU region for both.
- The sync lock is `pg_advisory_xact_lock`, which works through Neon's
  pooled connection.
- Costs of free: the service sleeps when idle, so the first request
  after a pause can take ~30–60 s (JVM start). Fine for testing, not
  for real users. 512 MB RAM, so the JVM needs `-Xmx` set.

**Moving later** (by design, under an hour):
1. Run the existing `docker/compose.yaml` on the new box, plus Caddy.
2. `pg_dump` from Neon, `docker/restore.sh` into the new Postgres.
3. Repoint the `api.rukavina.app` DNS record. No app update needed.

## 5. Decisions

| # | Question | Answer |
|---|---|---|
| Q-A | Sign in with Apple on Android? | **No.** Built with iOS. Enable the Firebase provider then |
| Q-B | Hosting (old Q-02) | **Free + portable for now:** Render + Neon (§4). Paid VPS later |
| Q-C | Firebase Analytics in v1? | **No.** Crashlytics only, so no consent gate |
| Q-D | Client version headers (§8.3) | Yes, done in 2.4 |
| Q-E | SonarQube new-code gate | ADR 0013 as is; revisit in 8.1 |
| Q-F | Can a cloud session run the tests? | Open. Needs JDK 17, Android SDK, a Firebase config stand-in, Docker. Check on first remote step |
| Q-G | Own domain for the API | **api.rukavina.app** (live). Staging: api-staging.rukavina.app (Plan 5) |
| Q-H | Can the Figma MCP draw frames? | **Yes.** Figma Console MCP since 2026-10-31 (Desktop + Bridge plugin). Starter plan: 3 pages, 1 variable mode |
| Q-I | Test + prod environments? | **Yes,** Plan 5, before features (owner 2026-11-02) |
| Q-J | Deployment tool | **fastlane** (MIT) for Play; Render deploy hooks for the backend (Plan 8) |

Figma file: https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy

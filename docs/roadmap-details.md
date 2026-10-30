# Roadmap: details

Re-plan of 2026-10-29. The checklist is [roadmap.md](roadmap.md); this
file explains each item. It replaces §13 (execution plan) and §17 (next
actions) of `architecture-and-execution-plan-v2.md`. The rest of that
document (decisions, data model, sync protocol, scope freeze §14) still
holds. iOS is out of scope until Android v1 is in closed testing.

## 1. Where we are

| Phase | State |
|---|---|
| 0 Monorepo | Done |
| 1 Schema hardening | Done. Group K favourites and rest timer are v2 (§14.2, Q-07), not open work |
| 2 OpenAPI contract | Done. All 8 endpoints in the spec |
| 3 Backend | Done. Sync, reference, /me, account deletion, tombstone job, rate limiter, drift test, Testcontainers suite |
| 4 Android sync | Mostly done. Hardening plan done except device checks. Gaps below |
| 5 Android v1 | Not started |
| 6 iOS | Later |
| 7 Launch | Later |

main is 30 commits behind develop. Last recorded green CI: b7334dd.

### Gaps found in this audit

Checked against the code on 2026-10-29, not against older status docs.

1. **Account deletion never reaches the backend.** `SettingsViewModel
   .performAccountDeletion` clears Room and deletes the Firebase user
   client-side. It never calls `DELETE /v1/account`, so the server keeps
   all of the user's data and nothing can delete it later. GDPR Art. 17
   and a Play/App Store blocker. It also wipes outside the sync lock
   (same bug class as hardening step 01).
2. **Two database builders.** `di/AppModule.kt` and
   `AppDatabase.getInstance` both build the DB, both with destructive
   fallback. `exportSchema = false`, so there are no schema JSONs to
   test migrations against.
3. **No sync debug screen** (§11.4, planned for Phase 4).
4. **Backend not deployed.** The release `API_BASE_URL` and the spec's
   production server are `api.gymbuddy.app`, which doesn't exist (§8.3).
5. **No Crashlytics.** The §6.8 rule "discarded payloads go to a
   Crashlytics non-fatal" isn't implemented.
6. **Backend observability partial.** traceId is in error responses;
   no structured JSON logs, no sync counters (§11.4).
7. **`X-Client-Version` / `X-Client-Platform` headers** (§8.3) are in
   neither the spec nor the code.
8. Carried known issues (§9.3): `ImageStorageUtil` write-then-delete,
   redundant notes query, unwired filter queries.

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
| Figma | Figma MCP server (from Plan 3) | Source of truth for all UI |
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

Bigger than the hardening steps. One step is one feature-sized change
that the reviewer can still hold in its head:

- One commit, roughly up to 600 changed lines including tests.
- May cross backend and Android when the feature does (e.g. account
  deletion), but at most one Room version bump.
- Has a "Done when" with exact commands, and a manual check list for
  what tests can't prove.
- Manual-only work is its own "(me)" step. One I can't do yet is
  marked "(deferred)" and `/step` skips it.

### Rules carried forward

- Out-of-scope list in every step file (v2 plan §1.2).
- Spec change, Room schema change, Flyway migration: called out in the
  step file and in the commit message.
- The hook blocks deploys, so deploying is always a "(me)" step.
- UI steps (after Plan 3) link a Figma frame. No frame, no UI change.

## 3. The plans

### Plan 1: phase4-closeout

Finish Phase 4 to its exit criteria (§13.3): two devices converge,
offline edits sync, a clean resync on 410.

1.1 **Account deletion end to end.** Android calls `DELETE /v1/account`
    after re-auth. Only on success: wipe under the sync lock
    (`LogoutCoordinator`'s pattern), then delete the Firebase user if
    the backend didn't already. Offline or failure: nothing is deleted,
    the dialog shows a retryable error. Tests: ViewModel + coordinator
    over in-memory Room, fake API. Fixes gap 1.
1.2 **Sync debug screen.** Outbox rows grouped by type, cursor, last
    sync time and outcome, last error with traceId. Reached from
    Settings, debug builds only. No Figma frame needed (dev tool).
1.3 **Deploy config.** See §4 Hosting. A `render.yaml` (or the chosen
    host's equivalent) that runs the existing Docker image, JVM memory
    flags for a 512 MB box, a GitHub Actions nightly `pg_dump` kept as
    an encrypted artifact, and `docker/restore.sh`. Docs in
    `docker/README.md`. The compose file stays the local and future
    VPS setup.
1.4 **(me)** Create the database and host accounts, set the secrets,
    deploy, add a CNAME `api.rukavina.app` → the Render service and
    the custom domain in Render, restore a dump into a local Postgres
    once.
1.5 **Point at the real host.** Release `API_BASE_URL` and the spec's
    production server, both `https://api.rukavina.app`. **Spec change.**
    Because it's my own domain, moving hosts later is a DNS change,
    not an app update.
1.6 **(me, deferred)** Device checks: the phase4-hardening step 05
    list, two devices converging, airplane-mode edits syncing on
    reconnect, 410 resync.
1.7 **(me)** CI green on develop, PR develop → main.

### Plan 2: phase5-data-safety

Nothing ships to testers until a schema change can't wipe their data.
R-11 is the one risk that can't be undone.

2.1 **One DB builder.** Remove the duplicate, `exportSchema = true`,
    commit the v26 schema JSON as the baseline, set up
    `MigrationTestHelper`.
2.2 **Remove `fallbackToDestructiveMigration()`.** Add a test that
    fails if the DB version has no migration path from v26.
2.3 **Crashlytics.** Crashes, sync failures as non-fatals tagged with
    traceId, discarded logout payloads (§6.8). No Analytics (Q-C).
    Collection off in debug builds.
2.4 **Backend observability + client headers.** JSON logs with
    traceId and userId, Micrometer counters for push/pull volume,
    conflicts and 410s. Android sends `X-Client-Version` and
    `X-Client-Platform`; the backend logs them. **Spec change.**
2.5 **(me)** CI green, PR develop → main.

### Plan 3: figma-foundation

All UI lives in Figma first, for Android now and iOS later. v1 scope
stays frozen (§14): this mirrors and tidies the current screens, it is
not the v2 UI overhaul.

3.1 **(me)** Create the GymBuddy Figma file. Connect Figma's official
    MCP server to Claude Code (needs a Figma account; Dev Mode features
    may need a paid seat, check at setup). Then Claude checks it can
    read a frame and reports which write tools exist (Q-H).
3.2 **Design tokens.** Colours, type scale, spacing and shapes from
    `ui/theme/` (Color.kt, Type.kt, Theme.kt) as Figma variables,
    light and dark. Also kept as `design/tokens.json` in the repo.
3.3 **Component library.** One Figma component per file in
    `ui/components/` (ScreenHeader, EmptyState, ThumbnailCard,
    NumberPicker, FilterBottomSheet, …), using the tokens.
3.4 **Screens.** Every v1 screen as a frame, built from the components:
    auth, home, exercises, templates, active workout, history, profile,
    settings. Claude writes the screen inventory and states (empty,
    loading, error); frames drawn in Figma by Claude if the MCP can
    write, otherwise by me.
3.5 **Code follows Figma.** Compose theme updated from the variables.
    From here on, step files for UI work link a Figma frame and the
    step reads it through the MCP before writing code.

### Plan 4: phase5-release

4.0 **Dependency CVEs.** Triage the Dependabot alerts on main (25 on
    2026-10-30, 1 critical, 13 high) and the OWASP findings: bump,
    replace, or suppress with a written reason. Then restore OWASP's
    push/PR triggers (manual-only since 2026-10-30). Before the
    release workflow, so the first AAB ships without known criticals.

4.1 **ViewModel tests.** Audit the 5 existing ViewModel tests, then
    cover the active workout state machine's failure paths (rejected
    save, resume after process death).
4.2 **ImageStorageUtil.** Write the new file first, then delete the
    old one; close streams with `use`.
4.3 **Release workflow.** On a `v*` tag: build a signed, minified AAB,
    versionCode from the tag. Signing secrets in GitHub secrets (me).
4.4 **(me)** Release build manual pass: R8, sign-in, sync, logout,
    account deletion.
4.5 **(me)** Privacy policy at a public URL (Q-11), linked in-app.
    Play Console setup and the Data Safety form (Firebase Auth +
    Crashlytics).

### Plan 5: phase5-closed-testing

5.1 **(me)** Closed testing track. Check the current tester rule (it
    was 12 testers for 14 days) before setting a date.
5.2 Fix what testers find, one step per batch of issues.
5.3 **(me)** Exit: a release build passes a full manual pass and closed
    testing is running. Then iOS planning starts, with designs already
    in Figma.

## 4. Hosting

Goal for now: free, easy to set up, easy to leave. Later: paid
hosting of my own (e.g. DigitalOcean).

**Choice: Render free web service (Docker) + Neon free Postgres.**
Check both free tiers at setup time, since they change.

- Render builds `backend/Dockerfile` as is. No buildpack, so nothing
  ties us to Render (§12.3).
- Neon is plain Postgres: `DATABASE_URL` + `DB_PASSWORD`, nothing
  else. `pg_dump` works. Pick an EU region for both.
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
| Q-D | Client version headers (§8.3) | Yes, in 2.4 |
| Q-E | SonarQube new-code gate | ADR 0013 as is; revisit in Plan 4 |
| Q-F | Can a cloud session run the tests? | Open. Needs JDK 17, Android SDK, a google-services.json stand-in, Docker. Check on first remote step |
| Q-G | Own domain for the API | **api.rukavina.app.** I own rukavina.app; DNS not set up yet (1.4) |
| Q-H | Can the Figma MCP draw frames, or only read? | **Both.** Read checked 2026-10-29; write tools exist (`use_figma`, `generate_figma_design`), first real write in 3.2. Starter plan, Full seat. Write tools are exempt from rate limits; read limits for this seat aren't published, so keep read calls lean |

Figma file: https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy
(MCP server in `.mcp.json`, authenticated on the Mac).

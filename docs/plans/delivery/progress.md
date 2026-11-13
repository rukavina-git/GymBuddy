# Progress

- [x] 00 Deploy secrets into environments, prod gated (0aa3afe, PR #9)
- [x] 01 CI gates: Android Lint, SonarQube (Q-E) (b36a470)
- [x] 02 Signed release AAB on a `v*` tag (322cf68)
- [x] 02a Clean Sonar issues and Dependabot alerts (before any `v*` tag)
      (part A d1e3fc5, part B 926a4a5)
- [x] 03 fastlane: testers, Play internal, store listing (HASH)
- [ ] 04 Backend CD: develop → test, `v*` → prod
- [ ] 05 (me) Play Console, upload key, service account, secrets
- [ ] 06 (me) CI green, PR develop → main

## Deviations

- 00: CI SSH key rotated mid-step: the old `gymbuddy_ci` private key
  was pasted into a Claude chat, so the owner made `~/.ssh/gymbuddy_ci2`
  (comment `gymbuddy-ci2`), added it to deploy's authorized_keys,
  removed the old line, and put it in all 3 environments + the
  repo-level `DEPLOY_SSH_KEY`. Bitwarden: one Secure Note "GymBuddy CI
  deploy key". `DEPLOY_KNOWN_HOSTS` holds all 3 host key types
  (`ssh-keyscan` without `-t`), README changed to match. The stale
  `develop-gymbuddy-backend` environment (Render leftover) was deleted.
  Owner run after PR #9: Backup from main green with the `.gpg`
  artifact; Deploy test green; Deploy prod waited for review, approved,
  green (`sha-f3050fe` over itself, 7 s, container not recreated).
  Repo-level `DEPLOY_*` deleted, Backup green again. Root's
  authorized_keys has no `gymbuddy-ci` line. Branch-refusal check
  (Backup from develop) not run.

- 01: Lint model generation failed (`ui-test-junit4` had no version on
  `androidTestImplementation`): added `platform(compose BOM)` there.
  Fixed pre-existing shellcheck findings in android-build.yml's Build
  Summary (env vars, grouped redirect) so actionlint is clean. CLAUDE.md
  line "CI doesn't run lint" updated. Owner checklist (SonarCloud gate
  + new-code definition, Sonar green on develop, `Lint` required on
  main) not done yet.
- 01 follow-up: the blocking gate broke develop CI: "Organization is
  not allowed to access data from non main branches" (free plan). The
  gate now waits only on pushes to `main`. SonarCloud was also wrong:
  backend's main branch was an empty `master`, real `main` a short
  branch. Owner deleted all 3 projects and recreated
  `GymBuddy-backend`, `-android`, `-ios` (reserved) by hand, public,
  unbound. `sonar-full.yml` deleted. Owner still has to rename each
  main branch to `main` after the first develop scan (Branches page
  appears only then), set new code = 30 days, gate = Sonar way.
  Done 2026-11-13: main branches renamed, `Lint` added to the
  "Main protect rule" ruleset (verified via API). Sonar keeps running
  on develop/PRs although the free plan discards those scans: the
  ruleset requires the Sonar checks on PRs into main.
- 02: the properties file with the signing passwords is written to
  `android/` (Gradle reads it from rootProject), not `$RUNNER_TEMP`;
  only the key itself sits in temp. Both are removed in an `always()`
  step. Mapping upload assumed to need only `google-services.json`;
  owner checks Crashlytics on the first tag run.

- 02a: owner decision mid-step: SonarCloud's main branch is
  `develop` (not `main`), gate warns on develop, release rule is the
  block. Step split into part A (CI, docs, Dependabot) and part B
  (Sonar fixes after the rename + one develop scan). Reviewer
  SHOULD-FIXes (stale Q-E row, 01 gotcha) fixed in part A since they
  contradicted the new docs.
- 02a part B: first develop scan listed 47 issues (backend 16, android
  31), 0 hotspots; gate status NONE (no new-code baseline yet). Owner
  chose "go, all": the 12 S3776 complexity refactors were done here,
  not split out. Done by extracting helpers, behaviour-neutral. SyncEngine
  touched (`prepareBatch`, `applyResult`: line-for-line moves, same
  transactions). Seed scripts: banner helper `section()`; generated SQL
  verified byte-identical to V2/V7 (migrations not regenerated, the
  scripts' version guards refuse). Not fixed, for owner to mark in
  SonarCloud: S107 ×5 (SyncAudit + 4 test builders, accepted),
  S6624 ×2 (backend build.gradle.kts versions, accepted), S2083
  path traversal in generate_reference_seed.py (false positive: fixed
  output path), S5332 + S6358 in src/debug/AndroidManifest.xml
  (debug-only, accepted), S6619 elvis in TemplateEditorViewModel:212
  (false positive: all operands nullable). Backend tests need Docker
  Desktop running (Testcontainers); first local run failed with 91
  context errors until it was started.

- 03: owner decision mid-step: develop is always test, main is
  always prod (ADR 0017). Prod = `v*` tag on main right after each
  merge; release workflow fails on a tag not on main. Test builds use
  a separate qa key in a new `testers` environment (develop only), not
  the upload key. 02's checklist now tags `v0.1.0` on main after 8.6.
  `promote` is a `workflow_dispatch` job in android-release.yml.
  Gemfile.lock not committed and `fastlane lanes` not run: the guard
  hook blocks fastlane/docker commands. Local Ruby is 2.6 (too old).

## Gotchas

- 00 for the owner run: with empty `DEPLOY_*` the backup job exits 0
  with a notice, and environment secrets fall back to repo ones. So
  a green Backup run proves nothing: check the run has the
  `gymbuddy-vps-<ts>.dump.gpg` artifact, especially after deleting
  the repo-level secrets (reviewer SHOULD-FIX; fixed after 8.0 on
  owner request: the empty case now fails red, on every trigger).
- 00 reviewer NITs left: `ssh-keyscan` without `-t` trusts
  host key types not checked against netcup's mailed fingerprint.
  Plan 5 progress still names `gymbuddy_ci` (history, left as is).
- 01: the lint gate only fails on Error severity (ShowToast, a warning,
  passed; MissingPermission failed). Baseline has 105 `<issue>` entries
  vs lint's "42 errors, 62 warnings" (ADR wording, not re-counted). The
  Lint report artifact holds HTML + SARIF (AGP writes both by default).
  Sonar gate now fails the job; if develop goes red on untouched code,
  that's the gate, not this step. Reviewer NITs left: ADR count, the
  gate only blocks main after the owner adds required checks.
- 01: no Sonar gate before merge on the free plan; the gate shows red
  on `main` after merging. [Superseded by 02a: SonarCloud's main
  branch becomes `develop`, gate warns there.] coverage-exclusions still emits `*-root`
  outputs that only sonar-full.yml used (dead, harmless).
- 02: owner checklist open (upload key, `release` environment with
  tag rule `v*`, 4 `ANDROID_UPLOAD_*` secrets, tag `v0.1.0`). The
  workflow fails red if any secret is missing. Reviewer NITs left:
  regex allows leading zeros (`v1.02.3` and `v1.2.3` share a
  versionCode) and `v0.0.0` (versionCode 0, Play rejects); the
  `major > 2000` check is dead (regex caps at 999); a backslash in an
  upload password breaks the Java properties parse (avoid it). 03
  uploads `app/build/outputs/bundle/prodRelease/app-prod-release.aab`.
- 02a part A: until the owner renames the SonarCloud branches, develop
  Sonar jobs show the yellow warning (gate read fails), not red.
  `continue-on-error` also hides real scan failures on develop (token,
  missing JaCoCo) as that warning; read the summary. PRs into main
  still scan report-only so the required checks report. Reviewer NIT
  left: "main was never scanned" wording (main got no push since 8.1).
- 02a: Dependabot alerts track `main` only; #39 stays open until the
  8.6 merge although develop is fixed.
- 02a part B: develop gate is verified only after this push's scan and
  the owner's Sonar marks above. Reviewer NITs left: 3 blank lines
  before `def section` in generate_reference_seed.py (PEP 8 wants 2),
  no blank line before `mainEnter` in MainScreen.kt. 03 can start; the
  release rule (gate green, #39 closed at 8.6) still blocks the first
  `v*` tag.
- 03 owner: generate and commit `android/Gemfile.lock` (Ruby 3.x,
  `bundle lock --add-platform x86_64-linux`) and run
  `bundle exec fastlane lanes` before the first run with Play secrets
  (without a lock, setup-ruby resolves gems inside the secret job).
- 03 reviewer SHOULD-FIX left: (a) the main-only check lives in the
  tagged commit's workflow, so a tag on an old commit skips it; add a
  tag ruleset (only owner creates `v*`) in 8.5 and say so in ADR 0017.
  (b) `promote` promotes the latest internal release, not the tag's;
  pass `version_code` from the tag. (c) `release_status: completed`
  fails while the Play app is unpublished (must be `draft`); 8.5's
  first upload is by hand in Play Console anyway. NITs: pass the
  Firebase `app:` id explicitly; check allows any older main commit.
- 04: prod deploy should use the same rule (v* tag on main).

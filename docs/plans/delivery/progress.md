# Progress

- [x] 00 Deploy secrets into environments, prod gated (0aa3afe, PR #9)
- [x] 01 CI gates: Android Lint, SonarQube (Q-E) (HASH)
- [ ] 02 Signed release AAB on a `v*` tag
- [ ] 03 fastlane: testers, Play internal, store listing
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

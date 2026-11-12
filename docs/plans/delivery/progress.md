# Progress

- [x] 00 Deploy secrets into environments, prod gated (0aa3afe, PR #9)
- [ ] 01 CI gates: Android Lint, SonarQube (Q-E)
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

## Gotchas

- 00 for the owner run: with empty `DEPLOY_*` the backup job exits 0
  with a notice, and environment secrets fall back to repo ones. So
  a green Backup run proves nothing: check the run has the
  `gymbuddy-vps-<ts>.dump.gpg` artifact, especially after deleting
  the repo-level secrets (reviewer SHOULD-FIX, not fixed: make the
  empty case fail on `schedule`).
- 00 reviewer NITs left: `ssh-keyscan` without `-t` trusts
  host key types not checked against netcup's mailed fingerprint.
  Plan 5 progress still names `gymbuddy_ci` (history, left as is).

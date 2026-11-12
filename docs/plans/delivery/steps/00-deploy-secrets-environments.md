# 00 Deploy secrets into environments, prod gated

## Problem
`DEPLOY_HOST`, `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS` are repo-level
secrets. They aren't public, but any workflow on any branch can use
them, and the `prod` environment has no reviewer. Step 04 (auto-deploy)
must not land on top of that. They are repo-level only because the
nightly backup job has no environment (Plan 5, step 04 deviation).

## Change (Claude)
- `db-backup.yml`: job `vps` gets `environment: backup`. A new
  environment with no reviewer (nightly runs can't wait for a click),
  limited to `main`. Skip-notice text names the environment.
- `deploy.yml`: error text says "environment secrets", not "repo
  secrets". Header comment: prod needs approval, test doesn't.
- `docker/README.md` Secrets: `DEPLOY_*` live in environments `test`,
  `prod`, `backup`; none repo-level. Table of the three environments
  (branches, reviewer, why).

## Checklist (owner, GitHub → Settings → Environments)
1. `prod`: add the 3 `DEPLOY_*` secrets. Required reviewer: you
   (owner may self-approve). Deployment branches: `main` + tags `v*`.
2. `test`: add the same 3 secrets. Branches: `develop`, `main`.
   No reviewer.
3. `backup`: add the same 3 secrets. Branches: `main`. No reviewer.
4. Run Backup by hand from `develop`: it must be refused by the
   branch rule (proves the gate). Then the PR in step 5.
5. Small PR develop → main with this change (scheduled backups run
   from main, so the new `environment: backup` only counts there).
   After merge: Backup by hand from main green; Deploy test green;
   Deploy prod waits for approval, approve, green.
6. Only then delete the 3 repo-level `DEPLOY_*` secrets. Run Backup
   from main again: still green.

## Out of scope
A separate SSH key per environment or a forced-command key (the same
`deploy` key can still reach both stacks). Note it as an option in
the README, don't build it.

## Done when
`actionlint` clean on both workflows. No repo-level `DEPLOY_*` secret
left. Backup green from main, Deploy prod needs approval, a run from
another branch is refused. Progress and roadmap ticked.

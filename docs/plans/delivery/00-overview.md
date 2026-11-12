# Plan 8: delivery (CI/CD + fastlane)

## Why
Releases and backend deploys are manual today. Plan 8 makes them
repeatable: lint gates, a signed release AAB on a `v*` tag, fastlane
to testers and the Play internal track, and backend auto-deploy
(develop → test, `v*` tag → prod).

Before any of that, prod must be gated. The `DEPLOY_*` SSH secrets
are repo-level, so any workflow on any branch can reach the box, and
the `prod` environment has no reviewer (Plan 5, step 08 gotcha).
Auto-deploy on top of that would make prod one push away.

## Read first
`.github/workflows/deploy.yml`, `db-backup.yml`, `docker/README.md`
(Secrets), `docs/plans/environments/progress.md` (Gotchas 04, 06, 08).

## Scope
0. Deploy secrets into GitHub environments, prod gated.
1. CI gates: Android Lint, SonarQube (Q-E).
2. Signed release AAB on a `v*` tag, versionCode from the tag.
3. fastlane: test build to testers, prod AAB to the Play internal
   track, store listing in the repo.
4. Backend CD: develop → test, `v*` tag → prod.
5. (me) Play Console app, upload key, service account, secrets.
6. (me) CI green, PR develop → main.

Step files are written one at a time, each after the previous step
lands.

## Invariants
- No secret in the repo. Every secret is listed in `docker/README.md`
  (backend) or `android/README.md` (Android) with where it lives.
- Prod deploy secrets live only in the `prod` environment, usable only
  from `main` and `v*` tags, after step 0. Nothing deploys prod
  without passing that environment.
- Backups keep running every night; no step may leave them skipped.
- Every new tool or action: commercial-friendly licence (fastlane
  MIT), recorded in an ADR.
- `api/openapi.yaml`, Room schema, sync, domain: no changes.
- Branch: develop.

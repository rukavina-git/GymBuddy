# 04 Backend CD: develop → test, `v*` → prod

## Problem
`backend-build.yml` pushes `sha-<7>` images to GHCR on develop/main,
but deploying them is a manual "Run workflow" on `deploy.yml`. Test
lags behind develop; prod has no link to the `v*` release tag that
ADR 0017 makes the prod trigger.

## Rule (ADR 0017)
develop is test, main is prod. Prod deploys only from a `v*` tag on
main, through the `prod` environment (owner approval). A tag doesn't
rebuild: prod runs the exact image develop → main already built.

## Change (Claude)
- `deploy.yml`: add `workflow_call` with the same inputs (`target`,
  `image_tag`), keep `workflow_dispatch` as the manual fallback /
  rollback. Job unchanged (environment = target, deploy.sh rolls back
  on failed health check). Concurrency group stays per target.
- `backend-build.yml`: new job `deploy-test` after `image`, only on
  push to `develop`, calls `deploy.yml` with `target: test`,
  `image_tag: sha-${{ github.sha }}` shortened like the image job
  (expose it as an `image` job output). Runs in env `test` (develop
  allowed). Pushes to main build the image but don't deploy.
- New `.github/workflows/backend-release.yml`: on push of tags `v*`.
  Job `check`: checkout (fetch-depth 0), "Tag must be on main" (same
  check as android-release), then wait for
  `ghcr.io/…/gymbuddy-backend:sha-<7>` to exist (poll
  `docker manifest inspect`, max ~25 min, GHCR login with
  `packages: read`), since the main push build may still be running.
  Job `deploy`: calls `deploy.yml` with `target: prod`; the `prod`
  environment asks the owner to approve.
- Comments in `deploy.yml`/`backend-build.yml` ("Auto-deploy: Plan
  8.4", "Deploying it is manual") updated. `docker/README.md`
  "Deploying to the VPS": auto paths, manual fallback, rollback =
  run Deploy with an older `sha-` tag. ADR 0016 gets a short
  "Update (8.4)" line; ADR 0017 names backend-release.yml.

## Checklist (owner)
1. Push to develop touching `backend/`: Backend Build → Image →
   deploy-test green, test API health shows the new revision.
2. After 8.6: tag `v0.1.0` on main; Backend Release waits for the
   image, asks for prod approval, deploys.
3. (from 03, gotcha a) tag ruleset so only the owner creates `v*`:
   in 8.5.

## Out of scope
Rebuilding or retagging images (`v0.1.0` image tag). Deploying main
pushes without a tag. DB migrations beyond today's Flyway-on-start.
Smoke tests after deploy beyond deploy.sh's health check. Changes on
the VPS or to deploy.sh. iOS, Android.

## Done when
`actionlint` clean on the 3 workflows. Backend Build on develop has a
`deploy-test` job that only runs on develop pushes. Backend Release
fails on a tag not on main (by reading the check, not by tagging).
README + ADR notes written. Progress and roadmap ticked.

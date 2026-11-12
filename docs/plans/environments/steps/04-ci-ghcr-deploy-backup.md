# 04 CI: image to GHCR, SSH deploy, backups

## Problem
ADR 0016: CI builds the backend image and pushes it to GHCR; a manual
workflow deploys a chosen tag over SSH as `deploy`; the nightly backup
reaches Postgres over SSH (it is not public). Today Render builds the
image itself, and the backup dumps Neon through a connection string.
Render + Neon keep serving until step 05, so nothing here may break
them.

## Change
- `backend-build.yml`: new `image` job, `needs: test`, only on `push`
  (develop, main). `docker/login-action` + `docker/build-push-action`
  (both Apache 2.0, pinned to major tags like the rest of CI) with
  `GITHUB_TOKEN` (`packages: write` on that job only). Pushes
  `ghcr.io/rukavina-git/gymbuddy-backend:sha-<7>` and `:<branch>`.
  linux/amd64 only (the VPS is x86). OCI `source` label so the
  package links to the repo.
- `docker/server/deploy.sh <image>` (runs on the server as `deploy`,
  in `/opt/gymbuddy`): checks the ref looks like
  `ghcr.io/rukavina-git/gymbuddy-backend:<tag>`, saves the current
  `BACKEND_IMAGE`, rewrites that line in `.env`, `docker compose pull
  backend`, `up -d --no-deps backend`, then polls
  `https://$API_HOST/v1/health` (`curl --resolve` to 127.0.0.1, up to
  ~90 s). On failure it writes the old image back, `up -d` again and
  exits non-zero. No nginx reload (Gotcha 03).
- `.github/workflows/deploy.yml`: `workflow_dispatch` with input
  `image_tag` (e.g. `sha-abc1234`). GitHub environment `prod` holds
  the secrets, so the owner can add a required reviewer later. Plain
  `ssh` from the runner (no third-party SSH action): key from
  `DEPLOY_SSH_KEY`, host pinned by `DEPLOY_KNOWN_HOSTS`
  (`StrictHostKeyChecking=yes`), runs `./deploy.sh` remotely.
  `concurrency: deploy-prod`.
- `db-backup.yml`: one job per source, each skips itself (with a
  notice) when its secrets are empty. `vps`: `ssh deploy@host 'cd
  /opt/gymbuddy && docker compose exec -T db pg_dump -Fc --no-owner -U
  gymbuddy gymbuddy'` piped into the same gpg command. `neon`: today's
  job, unchanged. Both run during the cutover; step 06 deletes `neon`.
  Artifact names say the source.
- `docker/README.md`: "Hosting" rewritten: GHCR image, deploy workflow,
  backups over SSH, restore onto the VPS (`docker compose exec -T db
  pg_restore`). Render/Neon parts kept, marked "until step 05".
  Secrets table: `DEPLOY_HOST`, `DEPLOY_SSH_KEY`,
  `DEPLOY_KNOWN_HOSTS` (GitHub, environment `prod` + repo-level for
  the backup job), and how to make the GHCR package public.
- The GHCR package is public (the repo is), so the server pulls
  without a login and no registry token sits on the box. Owner flips
  visibility once after the first push (step 05 checklist).

## Tests
- `actionlint` (MIT, run via its Docker image) clean on the three
  workflows; `shellcheck` clean on `deploy.sh`.
- Local, on step 03's smoke stack (self-signed cert, fake account):
  tag the local image as two `ghcr.io/rukavina-git/gymbuddy-backend:`
  tags, deploy A then B → health 200 after each, `.env` updated, nginx
  not restarted (backend swap check). Deploy a tag that doesn't exist
  → non-zero exit, `.env` back to B, health 200. Bad ref format →
  rejected before any change. `down -v` after.
- Backup pipe: run the `compose exec pg_dump | gpg` command against
  the local stack, restore with `docker/restore.sh` into a scratch
  `postgres:17`, row count matches.
- The image job and real SSH can't run locally: checked by the
  owner's first run in step 05.

## Out of scope
Buying/using the VPS, real secrets, first deploy (05); staging tags,
second database dump, deleting the Neon job and `render.yaml` (06);
auto-deploy on push (8.4); a forced-command backup-only SSH key.

## Done when
actionlint, shellcheck, the local deploy and backup runs pass, README
updated, progress and roadmap ticked.

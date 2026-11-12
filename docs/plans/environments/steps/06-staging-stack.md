# 06 Staging stack on the same box (spec change)

## Problem
Prod runs on the VPS since step 05 (`/opt/gymbuddy`, one compose
project: db, backend, nginx, certbot). Debug builds and tests must not
write into it. Staging needs its own backend and database on the same
box, behind the same nginx, at `api-staging.rukavina.app`. Render
removal already happened in 05.

## Change
- **Separate compose project** `docker/server/staging/compose.yaml`
  (`name: gymbuddy-staging`, lives in `/opt/gymbuddy-staging` with its
  own 0600 `.env`): `db` (postgres:17, own volume) and `backend`
  (`JAVA_TOOL_OPTIONS -Xmx192m`, same flags otherwise). No ports. The
  services keep the names `db`/`backend`, so `deploy.sh` works there
  unchanged (copied next to it). `.env.example` for it.
- **Shared network**: prod compose defines `edge` (`name:
  gymbuddy-edge`); nginx joins it. Staging's backend joins it as
  external with alias `backend-staging`. Staging's db stays on its own
  default network: prod and staging can't reach each other's Postgres.
- **nginx**: one cert for both names (`--expand`, cert name stays
  `api.rukavina.app`), so the staging block never points at a missing
  cert and can't take prod nginx down. New 443 block for
  `${STAGING_API_HOST}` proxying to `backend-staging:8080` (same
  resolver trick, 502 while staging is down). Port 80: the default
  server answers `/.well-known/acme-challenge/` for any name, still
  444 otherwise, so certs are issued through running nginx (no prod
  downtime). Prod `.env` gains `STAGING_API_HOST` (required).
- **init-cert.sh**: `WEBROOT=1` mode for a running nginx
  (`docker compose run --rm certbot certonly --webroot ... -d API_HOST
  -d STAGING_API_HOST --expand`). Standalone mode stays for a fresh box.
- **deploy.yml**: input `target` (`prod` | `staging`, default
  `staging`): picks `/opt/gymbuddy` or `/opt/gymbuddy-staging`, the
  GitHub environment of the same name, and `concurrency:
  deploy-<target>`. Same SSH key and secrets.
- **Spec** (`api/openapi.yaml`): add server
  `https://api-staging.rukavina.app`, description "Staging".
  **Explicit spec change**, servers list only.
- **Docs**: `docker/README.md` (Server stack: staging section, cert
  for both names, secrets table rows for the staging env file),
  ADR 0016 note if anything deviates. Backups stay prod-only (staging
  data is disposable).

## Tests
- `actionlint` on deploy.yml, `shellcheck` on init-cert.sh/deploy.sh,
  `npm run lint` in `api/`, backend `OpenApiSpecDriftTest` green.
- Local, on step 03's smoke setup (fake service account, self-signed
  cert for both names): both stacks up, `--resolve` health 200 on
  both hosts, each backend on its own DB (write via one, absent in
  the other's DB), staging down → staging 502 and prod still 200,
  `deploy.sh` in the staging dir swaps staging only, unknown Host
  still 444, acme path answered on port 80.
- Memory: `docker stats` with both stacks idle, recorded in progress.

## Out of scope
Running it on the VPS, the staging Firebase project, DNS, secrets (08).
Android flavors (07). Staging backups. Auto-deploy (8.4).

## Done when
Tests above pass, README + spec updated, progress and roadmap ticked.

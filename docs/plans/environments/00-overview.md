# Plan 5: environments (VPS, then staging + prod)

## Why
Today one Render service + one Neon database is both test and prod, and
debug builds write into it. Render's free hours can't run two
always-on services, so the owner chose a self-hosted VPS (2026-11-11):
netcup VPS nano (2 vCore, 2 GB, 60 GB, €3.88/mo), nginx + certbot,
Postgres on the box. First move the one environment over, then split
it into staging and prod on the same server.

Measured 2026-11-11 (local compose, `-Xmx300m`): backend 403 MB idle,
494 MB after 6k requests; Postgres 86 MB. Estimate for prod + staging
+ nginx + OS: 1.3–1.4 GB of 2 GB, plus 2 GB swap.

## Read first
`docker/README.md` (Hosting, Secrets, Backups), `render.yaml`,
`.github/workflows/db-backup.yml`, `backend-build.yml`.

## Scope
1. ADR 0016: self-hosting on a VPS + staging/prod split.
2. Server setup as code (hardening, Docker, swap).
3. Server stack: compose + nginx + certbot, Postgres not exposed.
4. CI: backend image to GHCR, manual SSH deploy workflow, backups
   pointed at the VPS.
5. (me) Buy the VPS, cut over from Render + Neon.
6. Staging stack on the same box, Render leftovers removed.
7. Android `staging` / `prod` flavors.
8. (me) Staging Firebase project, DNS, secrets, CI green, PR.

The server is bought only at step 5. Steps 1–4 are built and tested
locally. Step files are written one at a time, each after the previous
step lands.

Out of scope: auto-deploy on push/tag (Plan 8.4), Cloudflare proxy
(api records stay DNS-only, grey cloud), monitoring beyond a free
uptime check, a second server.

## Invariants
- The backend image and code stay host-agnostic: config only through
  env vars, as today. No VPS-specific code in `backend/`.
- Postgres and the backend port are never published to the internet;
  only nginx listens on 80/443. SSH by key only.
- No secret in the repo. Secrets live in GitHub secrets and a
  root-only env file on the server; `docker/README.md` lists each one.
- Prod data is never lost: the cutover keeps Neon until the owner
  deletes it; a backup is restored on the VPS as a test first.
- `api/openapi.yaml`: only step 6 adds the staging server. Room schema,
  sync and domain: no changes.
- Every new tool or image: commercial-friendly licence (nginx BSD-2,
  certbot Apache 2.0, Postgres PostgreSQL licence), recorded in the ADR.
- Branch: develop. Render keeps serving until the step 5 cutover.

# 01 ADR 0016: VPS hosting + environment split

## Problem
Hosting was chosen in roadmap-details §4 (Render + Neon), never as an
ADR. The owner replaced it on 2026-11-11 with a netcup VPS. Steps
02–08 build on that choice, so it needs one written decision first:
what runs where, how staging and prod are kept apart, and the
licences of everything new.

## Change
- `docs/adr/0016-vps-hosting-and-environments.md`, Status: Proposed
  (owner accepts on review). Sections as in 0015: Context, Decision,
  Consequences.
- Context: Render free hours can't run two always-on services; debug
  builds write into prod today; memory measured (overview).
- Decision:
  - One netcup VPS nano (Ubuntu LTS, EU). Docker Compose runs
    everything. Setup is a script in the repo (step 02).
  - nginx terminates TLS (certbot, Let's Encrypt, auto-renew) and is
    the only thing on 80/443. Chosen over Caddy: industry standard.
  - One Postgres container, one database + role per environment, not
    published. Backend ports bound to the Docker network only.
  - Two backend containers from the same GHCR image, config by env
    vars: `api.rukavina.app` (prod) and `api-staging.rukavina.app`.
  - Separate Firebase project per environment.
  - Debug and staging builds talk to staging; only prod release
    builds talk to prod.
  - Deploy: manual GitHub Actions workflow over SSH (deploy user,
    key in GitHub secrets) that pulls a tagged image. Auto-deploy is
    Plan 8.4.
  - Backups: nightly `pg_dump` per database, pulled off the box by
    GitHub Actions (as today with Neon).
  - DNS stays at Cloudflare, api records DNS-only (grey cloud).
  - Licence table: nginx (BSD-2), certbot (Apache 2.0), Postgres
    (PostgreSQL), Docker Engine (Apache 2.0), Ubuntu (free, mixed
    OSS), Let's Encrypt certs (no cost, no restriction).
- Consequences: we patch the OS (unattended-upgrades); one box is a
  single point of failure (accepted for v1, backups off-box); moving
  hosts = same compose + restore + DNS; cost €3.88/mo.
- `docs/roadmap-details.md` §4: one line under the heading pointing
  to ADR 0016 as the current choice; the Render text stays as history.

## Tests
None (docs). Check that links resolve and no secret or IP is written.

## Out of scope
Any script, compose file, workflow or app change (steps 02–07).

## Done when
ADR 0016 committed, §4 points to it, progress and roadmap ticked.

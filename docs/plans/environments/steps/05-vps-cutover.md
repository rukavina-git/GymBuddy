# 05 (me) Buy VPS, cut over from Render + Neon

## Problem
The VPS is bought (netcup nano, delivered 2026-11-12) but runs Debian 13
(trixie), not Ubuntu 24.04. `setup.sh` hardcodes Docker's Ubuntu apt
repo. Render + Neon still serve `api.rukavina.app`. This step moves
prod to the VPS without losing data. The owner runs it; Claude helps
live.

## Decision first (owner)
- A: keep Debian 13. Claude first makes a small code change:
  `setup.sh` picks Docker's repo from `/etc/os-release` `ID`
  (`debian`/`ubuntu`, refuses others), README + ADR 0016 say
  "Debian 13 or Ubuntu 24.04", the licence table gets Debian. Tested
  in `debian:13` and `ubuntu:24.04` containers (apt part only).
- B: reinstall Ubuntu 24.04 in the netcup panel. No code change, but
  a new root password and host key (new fingerprint email).

## Checklist
Prep (Render keeps serving):
1. Change the root password netcup mailed. First SSH: compare the
   ED25519 host key with the fingerprint in the netcup email.
2. `ssh-copy-id root@<host>`, then run `setup.sh` (README "Server
   setup"). Keep the root session open until
   `ssh deploy@<host> docker ps` works.
3. In the DNS panel, lower the `api` record TTL to 300 s (a day
   ahead if it was long).
4. Push develop so CI builds the image. Set the GHCR package public.
   Note its `sha-<7>` tag.
5. Add repo secrets `DEPLOY_HOST`, `DEPLOY_SSH_KEY`,
   `DEPLOY_KNOWN_HOSTS` (from `ssh-keyscan -t ed25519`, checked against
   the fingerprint). Create the `prod` environment.
6. As root: `mkdir /opt/gymbuddy && chown deploy:deploy`. Copy the
   stack files (README "Server stack"). Fill `.env` (0600) with
   `BACKEND_IMAGE=…:sha-<7>` (never `:local` or `:develop`), a new
   `DB_PASSWORD` (into the password manager), the Firebase creds.

Dry run (Render still prod):
7. `docker compose up -d db`, restore last night's Neon backup into it
   (README "Restore", onto the VPS). Row counts vs Neon match.
8. Point a test name (e.g. `api-test.rukavina.app`, DNS-only) at the
   VPS, set `API_HOST` to it, `STAGING_CA=1 ./init-cert.sh`, then a
   real cert, `docker compose up -d`, health 200. Run the Deploy
   workflow with the same `sha-` tag: green.
9. Run the backup workflow by hand: `vps` and `neon` jobs both green.

Cutover (short write freeze, a few minutes):
10. Suspend the Render service (no more writes to Neon).
11. Fresh `pg_dump` of Neon (backup workflow `neon` job). Empty the
    VPS database first: step 8's backend may have run newer Flyway
    migrations that `pg_restore --clean` won't drop
    (`docker compose down`, `docker volume rm gymbuddy_db-data`,
    `up -d db`). Restore with the backend stopped. Row counts match.
12. `API_HOST=api.rukavina.app` in `.env`, `init-cert.sh` needs
    port 80, so: point the `api` A record at the VPS first (DNS-only,
    grey cloud), wait for it to resolve, then `docker compose stop
    nginx`, `./init-cert.sh`, `docker compose up -d`.
13. Health 200 on `https://api.rukavina.app/v1/health`. Phone (debug
    build): sign in, sync a workout, it shows up after reinstall.
14. Add the free uptime check on `/v1/health`.

Rollback until step 06: point DNS back at Render and resume it. Neon
stays untouched (writes after step 10 would be lost there: only roll
back within the first hour, before real use).

## Out of scope
Deleting Render, Neon, `render.yaml` or the `neon` backup job (06);
staging (06); Android flavors (07). Neon is kept until the owner
deletes it.

## Done when
The api name serves from the VPS with the restored data, deploy and
both backup jobs ran green, the phone syncs. progress and roadmap
ticked with the hash of the docs commit (plus option A's commit).

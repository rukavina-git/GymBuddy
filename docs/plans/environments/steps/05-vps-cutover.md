# 05 (me) Move prod to the VPS

## Problem
The VPS is bought (netcup nano, delivered 2026-11-12, Debian 13, option A
done in e693e39). Render + Neon still serve `api.rukavina.app`. The
data there is test data only (owner, 2026-11-12), so there is no
migration: the VPS starts with an empty database and Flyway builds the
schema. Render and Neon can be shut off right away. The owner runs it;
Claude helps live.

## Checklist
Server:
1. Change the root password netcup mailed. First SSH: compare the
   ED25519 host key with the fingerprint in the netcup email.
2. `ssh-copy-id root@<host>`, then run `setup.sh` (README "Server
   setup"). Keep the root session open until
   `ssh deploy@<host> docker ps` works.

CI:
3. Push develop so CI builds the image. Set the GHCR package public.
   Note its `sha-<7>` tag.
4. Add repo secrets `DEPLOY_HOST`, `DEPLOY_SSH_KEY`,
   `DEPLOY_KNOWN_HOSTS` (`ssh-keyscan -t ed25519` with the same
   name/IP as `DEPLOY_HOST`, checked against the fingerprint). Create
   the `prod` environment.

Stack:
5. As root: `mkdir /opt/gymbuddy && chown deploy:deploy /opt/gymbuddy`.
   Copy the stack files and `deploy.sh` (README "Server stack"). Fill
   `.env` (0600): `API_HOST=api.rukavina.app`,
   `BACKEND_IMAGE=…:sha-<7>` (never `:local` or `:develop`), a new
   `DB_PASSWORD` (into the password manager), the Firebase creds.
6. DNS: make `api` an A record to the VPS IP (replace the Render
   CNAME, DNS-only, no AAAA). Wait until it resolves to the VPS.
7. Suspend or delete Render and Neon.
8. `./init-cert.sh`, then `docker compose up -d`. Health 200 on
   `https://api.rukavina.app/v1/health`.

Check:
9. Run the Deploy workflow with the same `sha-` tag: green.
10. Run the backup workflow by hand: `vps` job green.
11. Phone (debug build): clear app data first (its sync cursor belongs
    to the old server), sign in, sync a workout, it shows up after
    reinstall.
12. Add the free uptime check on `/v1/health`.

No rollback: the old data is disposable.

Cleanup (owner: remove all old hosting now, not in 06):
13. Delete the Neon project. Delete `render.yaml`, the `neon` backup
    job, the `BACKUP_DATABASE_URL` secret and the Render/Neon parts of
    `docker/README.md`, `docs/status.md`, `docs/roadmap-details.md`.
    ADRs and old plans stay as history.

## Out of scope
Deleting the suspended Render service itself (owner, any time).
Staging (06). Android flavors (07).

## Done when
The api name serves from the VPS, the `vps` backup job ran green, the
phone syncs, no Render/Neon config left in the repo. progress and roadmap ticked with the hash of
the docs commit (plus e693e39).

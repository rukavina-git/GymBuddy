# Progress

- [x] 01 ADR 0016: VPS hosting + environment split (7430654)
- [x] 02 Server setup script (2d7d299)
- [x] 03 Server stack: compose, nginx, certbot (HASH)
- [ ] 04 CI: image to GHCR, SSH deploy, backups
- [ ] 05 (me) Buy VPS, cut over from Render + Neon
- [ ] 06 Staging stack, Render removed (spec change)
- [ ] 07 Android staging / prod flavors
- [ ] 08 (me) Staging Firebase, DNS, secrets, CI green, PR develop → main

## Deviations

- 01: §4 pointer is 3 lines, not one. ADR status is Proposed until the
  owner accepts it.
- 02: README runs the script as `ssh root@<host> 'DEPLOY_PUBKEY="$(cat)" bash
  setup.sh' < key.pub`, not `sudo DEPLOY_PUBKEY=...`. Added a guard the
  step didn't list: the script aborts if root has no SSH key (netcup's default
  is a root password; turning off password login would lock root out).
  Also installs `gnupg` and `openssh-server`.
- 03: nginx tag is `1.30-alpine` (current stable), not `1.28-alpine`;
  certbot pinned to `v5.10.0`. The server env file is `deploy`-owned
  0600, not root-only; ADR 0016 and the overview reworded to match.
  The step 02 setup.sh SHOULD-FIX items were done here.

## Gotchas

- 01: Postgres is not public, so the nightly backup (step 04) must run
  `pg_dump` over SSH (e.g. `docker exec`), not through a connection string.
  Keep the `postgres:17` tag in step with the server's Postgres.
- 01: step 02 adds ufw (GPL-3) and unattended-upgrades (GPL-2). They run
  only on the server and are never shipped, so they're fine. Record them
  in the ADR 0016 licence table with that note, along with Docker Compose
  (Apache 2.0).
- 02: Docker's iptables rules bypass ufw for published ports. Step 03
  must not publish Postgres or backend ports (`expose` only); only nginx
  gets 80/443.
- 02: Reviewer SHOULD-FIX items left open (fix in 03 or before step 05's
  real run): fstab gets `/swapfile` even when existing swap skipped
  creating it; apt needs `--force-confdef --force-confold` so conffile
  prompts can't stop an upgrade; `systemctl restart docker` on every run
  restarts the stack (restart only if daemon.json changed); use
  `reload-or-restart ssh` (24.04 uses ssh.socket); README should say the
  `docker` group is root-equivalent before step 04 gives CI that key.
- Owner note (2026-11-12): the VPS is already bought. netcup installed
  Debian 13 (trixie) minimal, not Ubuntu 24.04. setup.sh hardcodes
  Docker's Ubuntu repo, so before step 05 either pick the repo from
  `/etc/os-release` `ID` (and update ADR 0016) or reinstall Ubuntu
  24.04 in the netcup panel. Owner decides when we get there.
- 03: nginx resolves `backend` once at startup. Replacing the backend
  container (any deploy) gives it a new IP, so nginx 502s until its 6 h
  reload. Step 04's deploy must run `docker compose exec nginx nginx -s
  reload` after the swap, or switch the template to `resolver
  127.0.0.11` + a variable `proxy_pass`; smoke-test a backend swap.
- 03 reviewer NITs left open: init-cert.sh copies a quoted `API_HOST`
  from the env file with its quotes; its nginx check misses a
  restarting nginx that still holds port 80; `.env.example` should warn
  that compose expands `$` in unquoted values and that
  `POSTGRES_PASSWORD` only applies on a fresh volume.
- 03: the smoke run used a fake service account (generated RSA key) and
  a self-signed cert seeded into the letsencrypt volume at
  `live/localhost/`; reuse that for later stack tests.
- Step 05 checklist must include: change the root password (netcup
  mailed it), check the ED25519 host key fingerprint from the netcup
  email on first SSH, `ssh-copy-id root@<host>` before setup.sh. The
  netcup panel firewall only blocks SMTP; leave it.

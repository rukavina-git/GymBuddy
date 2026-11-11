# Progress

- [x] 01 ADR 0016: VPS hosting + environment split (7430654)
- [x] 02 Server setup script (2d7d299)
- [ ] 03 Server stack: compose, nginx, certbot
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

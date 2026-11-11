# Progress

- [x] 01 ADR 0016: VPS hosting + environment split (HASH)
- [ ] 02 Server setup script
- [ ] 03 Server stack: compose, nginx, certbot
- [ ] 04 CI: image to GHCR, SSH deploy, backups
- [ ] 05 (me) Buy VPS, cut over from Render + Neon
- [ ] 06 Staging stack, Render removed (spec change)
- [ ] 07 Android staging / prod flavors
- [ ] 08 (me) Staging Firebase, DNS, secrets, CI green, PR develop → main

## Deviations

- 01: §4 pointer is 3 lines, not one. ADR status is Proposed until the
  owner accepts it.

## Gotchas

- 01: Postgres is not public, so the nightly backup (step 04) must run
  `pg_dump` over SSH (e.g. `docker exec`), not through a connection string.
  Keep the `postgres:17` tag in step with the server's Postgres.
- 01: step 02 adds ufw (GPL-3) and unattended-upgrades (GPL-2). They run
  only on the server and are never shipped, so they're fine. Record them
  in the ADR 0016 licence table with that note, along with Docker Compose
  (Apache 2.0).

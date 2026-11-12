# Progress

- [x] 01 ADR 0016: VPS hosting + environment split (7430654)
- [x] 02 Server setup script (2d7d299)
- [x] 03 Server stack: compose, nginx, certbot (2be942d)
- [x] 04 CI: image to GHCR, SSH deploy, backups (d66f4a4)
- [x] 05 (me) Buy VPS, cut over from Render + Neon (e693e39, b6ac3fb)
- [x] 06 Test stack (spec change; Render removed in 05) (5e67a4f)
- [ ] 07 Android qa / prod flavors
- [ ] 08 (me) Test Firebase, DNS, secrets, CI green, PR develop → main

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
- 04: deploy.sh pulls with `docker pull <image>` before touching the
  env file (the step said `compose pull` after); a failed pull changes
  nothing. Added `ENV_FILE` (default `.env`) and `CURL_INSECURE=1` for
  local tests: Claude's hooks block any file named `.env`. `DEPLOY_*`
  secrets are repo-level, not in the `prod` environment, so the nightly
  backup can use them; `prod` only gates deploy.yml. Local tests used a
  PATH shim for `docker pull` (images exist only locally) and gpg in a
  `postgres:17` container (no gpg on the Mac).

- 05 (prep, not ticked): owner chose option A (keep Debian 13).
  setup.sh picks Docker's repo from `/etc/os-release` `ID` (debian or
  ubuntu, others refused); README and ADR 0016 updated. Tested with
  SKIP_SYSTEMD=1 in debian:13 and ubuntu:24.04 (fedora:41 refused).
  The checklist itself is still open; the owner runs it with Claude.
- 05: owner chose no migration (Render/Neon held test data only): the
  VPS started with an empty DB, no dry run, no restore, no rollback.
  This drops the overview invariant "prod data is never lost / restore
  a backup first" for this cutover only. Step file rewritten to match.
  Render/Neon cleanup moved here from 06: `render.yaml`, the `neon`
  backup job, `BACKUP_DATABASE_URL` and the Render/Neon docs are gone;
  the Neon project is deleted, the Render service only suspended.
  Deploy workflow not run: `workflow_dispatch` needs `deploy.yml` on
  `main` (404 from develop), so it moves to 08. Phone: push verified on
  the server (1 user, 1 workout, 3 sets); pull after reinstall not
  re-checked. Uptime monitor already existed on the same URL.
- 06: init-cert.sh uses `--cert-name $API_HOST` with both `-d` names
  (not `--expand`): a changed name list reissues the lineage despite
  `--keep-until-expiring`; same names rerun is a no-op. Not in the
  step: prod backend gets alias `backend-prod` and nginx proxies to
  it, because plain `backend` resolved to test's backend over the
  shared network (found by the smoke test: prod requests hit test).
  Shared ssl_* and resolver moved to http level in the template.
  init-cert.sh against real Let's Encrypt not tested (needs the box).
  Renamed after commit 5e67a4f (owner, 2026-11-12): "staging" is
  "test" everywhere (TEST_API_HOST, backend-test, gymbuddy-test,
  /opt/gymbuddy-test, api-test.rukavina.app, deploy target and GitHub
  environment `test`, spec server "Test"). Android flavor (07) is `qa`,
  not `test`: AGP rejects flavor names starting with "test".
  Memory, both stacks idle locally: backends 431 + 395 MB, Postgres
  85 + 42, nginx 17, certbot 29: about 1 GB.

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
- 03: nginx proxies via `resolver 127.0.0.11` + a variable, so it
  re-resolves `backend` (10 s cache). Tested: backend swapped to a new
  IP, health 200 with no nginx reload. Step 04's deploy needs no
  reload, but its smoke test should keep a backend swap check.
- 03: init-cert.sh strips quotes from `API_HOST` and also refuses while
  nginx is restarting. `.env.example` warns about `$` expansion and
  that `DB_PASSWORD` only applies to a fresh volume (use ALTER ROLE).
- 03: the smoke run used a fake service account (generated RSA key) and
  a self-signed cert seeded into the letsencrypt volume at
  `live/localhost/`; reuse that for later stack tests.
- Step 05 checklist must include: change the root password (netcup
  mailed it), check the ED25519 host key fingerprint from the netcup
  email on first SSH, `ssh-copy-id root@<host>` before setup.sh. The
  netcup panel firewall only blocks SMTP; leave it.
- 04: reviewer SHOULD-FIX left open: (a) rollback goes by tag, so
  deploying a mutable tag (`develop`) over itself can't roll back;
  accept only `sha-` tags or roll back by image ID. (b) Record the
  three docker/* actions (Apache 2.0) in the ADR 0016 licence table.
  NITs: a `prod` reviewer gate doesn't protect the repo-level key; VPS
  restore example still says `gymbuddy-<ts>`, artifacts are now
  `gymbuddy-<vps|neon>-<ts>`; `.env.example` default `:local` would
  make a first failed deploy roll back to a missing image (05: set a
  real `sha-` tag first); `set_image` truncates then writes.
- 04 for 05: set the GHCR package public after the first image push;
  add `DEPLOY_HOST`, `DEPLOY_SSH_KEY`, `DEPLOY_KNOWN_HOSTS` repo secrets
  and a `prod` environment; copy `deploy.sh` to /opt/gymbuddy.
  The VPS restore streams the dump into `pg_restore` via stdin
  (untested that way; restore.sh from a file was tested). Backups
  produce a pipe-made custom dump: don't restore it with `-j`.
- 05 reviewer items left open for the checklist run: (a) step 8 runs
  `init-cert.sh` twice (staging CA, then real) but it passes
  `--keep-until-expiring`, so the LE staging-CA cert may stay; check the
  issuer or delete the staging lineage first. (b) Debian's default
  unattended-upgrades also applies point releases, so README's
  "security only" is wrong there; fix the doc or limit origins.
  NITs: `chown deploy:deploy /opt/gymbuddy` (path missing in step 6
  and README); scan `DEPLOY_KNOWN_HOSTS` with the same name/IP as
  `DEPLOY_HOST`; the `api` record is likely a CNAME to Render: replace
  with A, no stray AAAA (rollback re-creates the CNAME); step 7 row
  counts are compared to the dump, not live Neon; setup.sh header
  example uses `sudo` (Debian minimal has none).
- 05: VPS 62.83.85.218 (`v2202611456973557814.hotsrv.de`), Debian 13.
  Cert: Let's Encrypt, expires 2027-02-10 (certbot renews). Running
  image `sha-0af8853`. Two keys on the Mac: `~/.ssh/id_ed25519` (owner,
  root + deploy) and `~/.ssh/gymbuddy_ci` (`DEPLOY_SSH_KEY`, deploy
  only); both in Bitwarden, no passphrase.
- 05: Claude's guard hook blocks `ssh deploy@…`/prod commands and
  reading `.env`; the owner runs those in Terminal. `!` in Claude can't
  answer SSH prompts (host key, password).
- 05 for 06: Render removal is done; 06 is only the test stack +
  spec server. Pasted base64 can wrap: check `.env` with `wc -l`.
- 05 for 08: run the Deploy workflow (`sha-` tag) once it's on main.
- 06 for 08, rollout order on the live box (wrong order breaks prod):
  1. add `TEST_API_HOST=api-test.rukavina.app` to
  /opt/gymbuddy's env file FIRST (compose's `:?` check otherwise fails
  every compose command there, incl. deploy.sh and the nightly
  backup); 2. DNS A record `api-test` (grey cloud); 3. copy new
  compose.yaml + nginx template + init-cert.sh together, `docker
  compose up -d` (recreates backend + nginx: ~30 s planned downtime);
  4. `WEBROOT=1 CERT_EMAIL=… ./init-cert.sh` (only works once nginx
  runs the new template), `docker compose exec nginx nginx -s reload`;
  5. test dir + env file, `up -d`; 6. `test` GitHub environment.
  A test deploy fails its health check until step 4 is done.
- 06 reviewer left open: init-cert WEBROOT could precheck the
  challenge path (token + curl) before spending an LE attempt;
  `DEPLOY_*` are repo secrets, so the `prod` reviewer gate is weak
  (move to environment secrets to really gate); `docker compose down`
  on prod can't remove gymbuddy-edge while test is attached, and
  test can't start until prod is up.

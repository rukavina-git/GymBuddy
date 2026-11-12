# Docker

Compose files for local dev and the VPS server stack.

## Local dev

    docker compose -f docker/compose.yaml up --build

Two services:

- `db` - postgres:16, database `gymbuddy`, data persisted in a named
  volume so it survives `docker compose down`. Port 5432 is exposed for
  local inspection (`psql`, a GUI client, etc.).
- `backend` - built from `backend/`, port 8080, waits for `db` to be
  healthy. Reaches Postgres only via the `DATABASE_URL` env var set here
  - no host/port/db config lives anywhere else.

TLS is only on the VPS: nginx in the server stack (below).

## Server setup

`docker/server/setup.sh` hardens a fresh Debian 13 or Ubuntu 24.04
VPS (ADR 0016); it refuses other distros.
Run it once as root; running it again is safe.

    ssh-copy-id root@<host>     # the script refuses to run without a root key
    scp docker/server/setup.sh root@<host>:
    ssh root@<host> 'DEPLOY_PUBKEY="$(cat)" bash setup.sh' < ~/.ssh/id_ed25519.pub

It installs Docker + Compose (Docker's apt repo), ufw (deny incoming
except 22/80/443), unattended-upgrades (security only, no auto-reboot),
2 GB swap (`vm.swappiness=10`) and Docker log rotation (10 MB × 3). It
creates a `deploy` user (key only, no password, no sudo, in `docker`)
and turns off SSH password login. Root keeps key login.

Keep the root session open until `ssh deploy@<host> docker ps` works
from a second terminal.

The `docker` group is root-equivalent: anyone with the deploy key can
mount `/` into a container. Treat that key like root's (step 04 puts
it in GitHub secrets).

Docker writes its own iptables rules, so a port published in compose
bypasses ufw. Never publish Postgres or the backend; only nginx gets
80/443.

## Server stack

`docker/server/` is the prod stack for the VPS (ADR 0016): Postgres 17,
the backend image, nginx (TLS, the only service with ports) and
certbot. It is separate from the local dev `compose.yaml`, which
publishes 5432/8080 and builds the image.

As `deploy`, once:

    mkdir -p /opt/gymbuddy        # as root: then chown deploy:deploy
    scp -r docker/server/{compose.yaml,init-cert.sh,deploy.sh,nginx} deploy@<host>:/opt/gymbuddy/
    # on the server: copy .env.example to /opt/gymbuddy/.env, fill it,
    # chmod 600 it
    cd /opt/gymbuddy
    CERT_EMAIL=<you> ./init-cert.sh   # STAGING_CA=1 for a test cert first
    docker compose up -d
    curl https://<API_HOST>/v1/health

- `init-cert.sh` gets the first cert with certbot standalone on port
  80, so nginx must be stopped while it runs. nginx can't start
  without a cert.
- After that the `certbot` service tries `certbot renew --webroot`
  every 12 h, and nginx reloads every 6 h to pick up a new cert.
- Unknown `Host` gets no response (444 on 80, TLS handshake refused on
  443), so scanners hitting the IP see nothing.
- The env file is owned by `deploy`, mode 0600: compose runs as
  `deploy`, and `deploy` is root-equivalent anyway.

## Hosting

The netcup VPS (ADR 0016) runs the server stack above and serves
`api.rukavina.app` since 2026-11-12, with the backend image from GHCR.
Render + Neon are gone. The image is the same everywhere; nothing in
the app knows where it runs.

### Image on GHCR

`backend-build.yml` job "Image to GHCR" runs after the tests on every
push to `develop` or `main` that touches `backend/`. It pushes
`ghcr.io/rukavina-git/gymbuddy-backend:sha-<7 chars>` and
`:<branch>` (linux/amd64). The package is public, like the repo, so
the server pulls without a login. After the first push, set it public
once: GitHub → Packages → gymbuddy-backend → Package settings → Change
visibility.

### Deploying to the VPS

Manual: Actions → Deploy backend → Run workflow → `image_tag`, e.g.
`sha-abc1234` (from the build run or the package page). The job SSHes
in as `deploy` and runs `/opt/gymbuddy/deploy.sh <image>`, which:

- refuses anything but a `gymbuddy-backend` GHCR tag,
- pulls it (nothing changes if the pull fails),
- writes it to `BACKEND_IMAGE` in `.env` and recreates only the backend,
- polls `https://<API_HOST>/v1/health` through the local nginx for 90 s,
- on failure writes the old image back, restarts it, and fails the job.

nginx is not reloaded: it re-resolves `backend` by itself. The same
script works by hand on the server. The workflow uses the GitHub
environment `prod`; add a required reviewer there to gate deploys.
Auto-deploy on push waits for Plan 8.4.

### Secrets

| Secret | Where | Value |
|---|---|---|
| `BACKUP_PASSPHRASE` | GitHub Actions secret + password manager | long random string; without it no backup can be restored |
| `DEPLOY_HOST` | GitHub Actions secret | VPS host name or IP |
| `DEPLOY_SSH_KEY` | GitHub Actions secret | private key of the `deploy` user (root-equivalent, see Server setup). Used by deploy and backup |
| `DEPLOY_KNOWN_HOSTS` | GitHub Actions secret | `ssh-keyscan -t ed25519 <host>` output, checked against the fingerprint netcup mailed |
| `BACKEND_IMAGE` | VPS `/opt/gymbuddy/.env` | `ghcr.io/rukavina-git/gymbuddy-backend:sha-<7>`; `deploy.sh` rewrites it (not secret) |
| `API_HOST` | VPS `/opt/gymbuddy/.env` | `api.rukavina.app` (not secret) |
| `DB_PASSWORD` | VPS `/opt/gymbuddy/.env` + password manager | Postgres `gymbuddy` role password |
| `FIREBASE_CREDENTIALS_B64` | VPS `/opt/gymbuddy/.env` + password manager | base64 service account JSON on one line (see backend/README.md) |

### Backups

`.github/workflows/db-backup.yml` runs nightly (02:30 UTC) and on manual
trigger. It SSHes in as `deploy` and runs `docker compose exec -T db
pg_dump -Fc` on the server (Postgres isn't public). The dump streams
over SSH into gpg on the runner. It skips itself with a notice when
the `DEPLOY_*` secrets are empty.

It encrypts with `gpg --symmetric` (AES256) and upload only the
`.gpg` file (`gymbuddy-vps-<ts>.dump.gpg`) as an artifact kept
30 days. The repo is public, so the dump is never stored unencrypted.

### Restore

1. Download the artifact from the workflow run and unzip it.
2. Run:

       BACKUP_PASSPHRASE=... docker/restore.sh gymbuddy-vps-<ts>.dump.gpg \
         'postgresql://<user>:<password>@<host>/<db>?sslmode=require'

   Needs `gpg` and `pg_restore` (same or newer major version) locally.
   `--clean --if-exists` drops and replaces existing objects.
   `--no-acl` skips GRANTs (old hosted dumps granted to roles that
   don't exist elsewhere); the app connects as the database owner.

Onto the VPS (Postgres isn't reachable from outside): decrypt locally,
then stream the plain dump into the db container. Stop the backend
first so nothing writes during the restore:

    gpg --decrypt gymbuddy-vps-<ts>.dump.gpg > gymbuddy.dump
    ssh deploy@<host> 'cd /opt/gymbuddy && docker compose stop backend'
    ssh deploy@<host> 'cd /opt/gymbuddy && docker compose exec -T db \
      pg_restore --clean --if-exists --no-owner --no-acl --exit-on-error \
      -U gymbuddy -d gymbuddy' < gymbuddy.dump
    ssh deploy@<host> 'cd /opt/gymbuddy && docker compose start backend'
    rm gymbuddy.dump

The target server must run the same Postgres major version as the
source, or newer. A pg_dump 17 dump fails on a 16 server
(`unrecognized configuration parameter "transaction_timeout"`), and
local compose runs `postgres:16`. Bump the compose image to the
server's version before restoring a hosted dump into it.

To run it from a container instead of installing tools (match the tag
to the target server):

    docker run --rm -it -v "$PWD:/w" -w /w postgres:16 \
      sh -c 'apt-get update -qq && apt-get install -yqq gnupg >/dev/null &&
             docker/restore.sh <dump.gpg> postgresql://gymbuddy:local@host.docker.internal:5432/gymbuddy'

### Moving to a VPS

Plan 5 (ADR 0016) does this: "Server setup" and "Server stack" above,
then the cutover checklist in Plan 5 step 05.

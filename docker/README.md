# Docker

Compose files and Caddy config for local dev and deployment.

## Local dev

    docker compose -f docker/compose.yaml up --build

Two services:

- `db` - postgres:16, database `gymbuddy`, data persisted in a named
  volume so it survives `docker compose down`. Port 5432 is exposed for
  local inspection (`psql`, a GUI client, etc.).
- `backend` - built from `backend/`, port 8080, waits for `db` to be
  healthy. Reaches Postgres only via the `DATABASE_URL` env var set here
  - no host/port/db config lives anywhere else.

No Caddy yet - Render terminates TLS (see Hosting). It earns its place
when the backend moves to a VPS.

## Hosting

Render free web service (Docker, Frankfurt) running `backend/Dockerfile`,
Neon free Postgres (EU). Blueprint: `render.yaml` at the repo root. The
image is the same one compose builds; nothing in the app knows about
Render or Neon.

### Neon: direct vs pooled host

Neon gives two hosts per branch: `ep-xxx.<region>.aws.neon.tech`
(direct) and `ep-xxx-pooler...` (PgBouncer). Use the **direct** host
everywhere: Flyway takes a session-level advisory lock that a
transaction pooler breaks, and one small instance doesn't need pooling.

### Secrets

| Secret | Where | Value |
|---|---|---|
| `DATABASE_URL` | Render env | `jdbc:postgresql://<direct-host>/<db>?sslmode=require&user=<user>` (no password) |
| `DB_PASSWORD` | Render env | Neon role password |
| `FIREBASE_CREDENTIALS_B64` | Render env | base64 service account JSON (see backend/README.md) |
| `JAVA_TOOL_OPTIONS` | `render.yaml` | heap limits for 512 MB, not secret |
| `BACKUP_DATABASE_URL` | GitHub Actions secret | `postgresql://<user>:<password>@<direct-host>/<db>?sslmode=require` (libpq form, password included) |
| `BACKUP_PASSPHRASE` | GitHub Actions secret + password manager | long random string; without it no backup can be restored |

Render's `PORT=8080` is in `render.yaml`; it only tells Render which
port the container listens on.

### Backups

`.github/workflows/db-backup.yml` runs nightly (02:30 UTC) and on manual
trigger. It runs `pg_dump -Fc` in a `postgres:17` container (keep the
tag at or above Neon's Postgres major version), encrypts with
`gpg --symmetric` (AES256), and uploads only the `.gpg` file as an
artifact kept 30 days. The repo is public, so the dump is never stored
unencrypted.

### Restore

1. Download the artifact from the workflow run and unzip it.
2. Run:

       BACKUP_PASSPHRASE=... docker/restore.sh gymbuddy-<ts>.dump.gpg \
         'postgresql://<user>:<password>@<host>/<db>?sslmode=require'

   Needs `gpg` and `pg_restore` (same or newer major version) locally.
   `--clean --if-exists` drops and replaces existing objects.

The target server must run the same Postgres major version as the
source, or newer. A pg_dump 17 dump fails on a 16 server
(`unrecognized configuration parameter "transaction_timeout"`), and
local compose runs `postgres:16`. Bump the compose image to Neon's
version before restoring a hosted dump into it.

To run it from a container instead of installing tools (match the tag
to the target server):

    docker run --rm -it -v "$PWD:/w" -w /w postgres:16 \
      sh -c 'apt-get update -qq && apt-get install -yqq gnupg >/dev/null &&
             docker/restore.sh <dump.gpg> postgresql://gymbuddy:local@host.docker.internal:5432/gymbuddy'

### Moving to a VPS

1. Add a Caddy service to `compose.yaml` that proxies to `backend:8080`
   and gets a certificate for `api.rukavina.app`.
2. Put `DB_PASSWORD` and `FIREBASE_CREDENTIALS_B64` in `docker/.env`
   on the server; `docker compose up -d --build`.
3. Pause the Render service, take a fresh dump (manual workflow run),
   restore it into the compose `db` with `restore.sh`.
4. Repoint the `api.rukavina.app` DNS record to the VPS, check
   `/v1/health`, then point `BACKUP_DATABASE_URL` at the new database.
5. Delete the Render service and Neon project once the VPS has run a
   few days with good backups.

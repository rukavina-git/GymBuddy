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

No Caddy yet - it earns its place in Phase 4 when there's TLS to
terminate.

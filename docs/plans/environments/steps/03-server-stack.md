# 03 Server stack: compose, nginx, certbot

## Problem
ADR 0016 runs the backend, Postgres and nginx with Docker Compose on
the VPS, TLS from Let's Encrypt. `docker/compose.yaml` is the local dev
stack: it publishes 5432 and 8080 and builds the image. The server
needs its own stack that publishes only 80/443. This step builds it
for prod only (staging is step 06) and tests it locally.

## Change
- `docker/server/compose.yaml` (project name `gymbuddy`, lives in
  `/opt/gymbuddy` on the server):
  - `db`: `postgres:17` (matches the backup tag, Gotcha 01), named
    volume, healthcheck, `expose` only, no `ports`.
  - `backend`: `image: ${BACKEND_IMAGE:?}` (GHCR tag from step 04;
    locally a `docker build` tag). `DATABASE_URL` points at `db`,
    `DB_PASSWORD`, `FIREBASE_CREDENTIALS_B64` from `.env`,
    `JAVA_TOOL_OPTIONS=-Xmx300m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m`,
    `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` (both as in render.yaml).
    No `ports`. `restart: unless-stopped` on every service.
  - `nginx`: `nginx:1.30-alpine` (stable), the only service with
    `ports: 80, 443`. Config from a template (`/etc/nginx/templates`,
    nginx's built-in envsubst) using `${API_HOST}`.
  - `certbot`: `certbot/certbot`, shares the cert and webroot volumes,
    runs `certbot renew --webroot` every 12 h. nginx reloads itself
    every 6 h so a renewed cert is picked up (no Docker socket in any
    container).
- `docker/server/nginx/api.conf.template`: port 80 serves
  `/.well-known/acme-challenge/` from the webroot and redirects the
  rest to HTTPS. Port 443: TLS 1.2/1.3, HSTS, `proxy_pass
  http://backend:8080` with `Host`, `X-Forwarded-For/-Proto` headers,
  `client_max_body_size 1m`, `server_tokens off`. Unknown `Host` → 444.
- `docker/server/init-cert.sh`: first certificate before nginx can
  start (nginx won't load without one). Runs `certbot certonly
  --standalone` once in the certbot container with port 80 mapped,
  then exits. Args: `API_HOST`, `CERT_EMAIL`. `STAGING_CA=1` uses
  Let's Encrypt's staging CA for dry runs.
- `docker/server/.env.example`: `BACKEND_IMAGE`, `API_HOST`,
  `DB_PASSWORD`, `FIREBASE_CREDENTIALS_B64` (placeholders, no values).
- Env file on the server: `/opt/gymbuddy/.env`, owner `deploy`, mode
  0600. ADR 0016 says "root-only", but compose runs as `deploy`, and
  `deploy` is root-equivalent through the `docker` group anyway. Fix
  the ADR wording to match.
- `docker/README.md`: new "Server stack" section: copy the folder to
  `/opt/gymbuddy`, fill `.env`, `init-cert.sh`, `docker compose up -d`,
  check `/v1/health`. Replace the Caddy-based "Moving to a VPS" list
  with a pointer to step 05. Secrets table: add the server `.env` rows.
- Leftover setup.sh SHOULD-FIX items from step 02 (Gotchas): fstab
  only when the script made the swapfile; apt `--force-confdef
  --force-confold`; restart Docker only if `daemon.json` changed;
  `reload-or-restart ssh`; README says `docker` group is
  root-equivalent. Debian 13 support stays an owner decision for 05.

## Tests
- `docker compose -f docker/server/compose.yaml config` passes with
  `.env.example` values; `config` shows no `ports` except nginx.
- Local smoke run: build the backend image, put a self-signed cert for
  `localhost` where certbot would, `up -d`, then
  `curl -k https://localhost/v1/health` → 200, `curl
  http://localhost/x` → 301, wrong Host → no response. Run `nginx -t`
  in the container. `docker compose down -v` after.
- `shellcheck` clean on `init-cert.sh` and `setup.sh`; step 02's
  container smoke run of `setup.sh` passes again twice.

## Out of scope
GHCR, deploy workflow, backups over SSH (04); a real certificate and
running anything on the VPS (05); staging service, second database,
`api-staging` server block (06); Cloudflare proxy, rate limiting.

## Done when
compose config check and local smoke run pass, shellcheck and the
setup.sh container run pass, README and ADR updated, progress and
roadmap ticked.

# 03 Hosting config and backups

## Problem
The backend runs only on the Mac. Phones and two-device checks need it
reachable at a stable URL. Hosting choice (roadmap-details §4): Render
free web service running `backend/Dockerfile`, Neon free Postgres, both
in the EU. It must stay easy to leave for a paid VPS later.

## Change
- `render.yaml` at the repo root (Render blueprint): one web service,
  Docker runtime, `backend/Dockerfile`, health check `/v1/health`, EU
  region, env vars declared but not valued (`DATABASE_URL`,
  `DB_PASSWORD`, `FIREBASE_CREDENTIALS_B64`, `JAVA_TOOL_OPTIONS`).
- JVM fits 512 MB: `JAVA_TOOL_OPTIONS=-Xmx300m -XX:+UseSerialGC
  -XX:MaxMetaspaceSize=128m` (tune in step 04 if it OOMs).
- Check the backend accepts Neon's URL as
  `jdbc:postgresql://<host>/<db>?sslmode=require&user=<user>`. Flyway
  must run on the direct (unpooled) host; note which one in the README.
- `.github/workflows/db-backup.yml`: nightly cron + manual trigger.
  `pg_dump -Fc` from `secrets.BACKUP_DATABASE_URL`, encrypted with
  `gpg --symmetric` using `secrets.BACKUP_PASSPHRASE`, uploaded as an
  artifact with 30-day retention. The repo is public, so the dump is
  never uploaded unencrypted.
- `docker/restore.sh <dump.gpg> <target-database-url>`: decrypt and
  `pg_restore --clean --no-owner`.
- `docker/README.md`: hosting section with the setup, where each secret
  goes, how to restore, and the move-to-VPS checklist (compose + Caddy,
  restore dump, repoint `api.rukavina.app`).

## Tests
- `docker compose up` locally still works (unchanged compose file).
- `restore.sh` tested against the local compose Postgres with a dump
  from it.
- Workflow YAML lints (actionlint if available, otherwise review).

## Out of scope
Creating the Render or Neon accounts, setting secrets, DNS (step 04),
Caddy config for the future VPS, any backend code change beyond what
Neon's URL strictly needs.

## Done when
Backend `./gradlew build` green, restore script round-trip works
locally, README explains every secret.

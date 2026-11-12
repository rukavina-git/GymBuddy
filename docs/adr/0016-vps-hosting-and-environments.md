# 16. Self-hosted VPS with separate test and prod

Date: 2026-11-12
Status: Proposed

## Context

The backend runs on a free Render web service with a free Neon Postgres
(roadmap-details §4). That one environment is both test and prod, and
debug builds write into it. Before release (Plan 8 on) nothing under
test may touch prod data, so we need two environments. Render's free
hours can't keep two always-on services, and the free service sleeps
when idle (~30–60 s cold start).

Measured 2026-11-11 (local compose, `-Xmx300m`): backend 403 MB idle,
494 MB after 6k requests; Postgres 86 MB. Prod + test + nginx + OS
come to about 1.3–1.4 GB.

## Decision

- **One VPS.** netcup VPS nano (2 vCore, 2 GB RAM, 60 GB, EU,
  €3.88/month), Debian 13 (Ubuntu 24.04 also works), 2 GB swap. A
  setup script in the repo hardens a fresh box: SSH by key only, ufw allowing 22/80/443,
  unattended-upgrades, a non-root deploy user, Docker.
- **Docker Compose runs everything.** The backend stays host-agnostic:
  the same `backend/Dockerfile` image, configured only through env vars.
- **nginx terminates TLS** and is the only service on 80/443.
  Certificates from Let's Encrypt via certbot, renewed automatically,
  nginx reloaded after renewal. Chosen over Caddy because it is the
  industry standard and the owner's company uses it.
- **One Postgres container**, one database and one role per
  environment. Not published to the host or the internet. Backend
  ports stay on the Docker network.
- **Two environments, fully separate:**

  | | Prod | Test |
  |---|---|---|
  | Host | `api.rukavina.app` | `api-test.rukavina.app` |
  | Backend | own container | own container, smaller heap |
  | Database | own database + role | own database + role |
  | Firebase | current project | new test project |
  | Android builds | prod release only | debug + `qa` flavor ("GymBuddy Test") |

- **Images on GHCR.** CI builds and pushes the backend image. A manual
  GitHub Actions workflow deploys a chosen tag over SSH as the deploy
  user. Auto-deploy waits for Plan 8.4.
- **Backups off the box.** The nightly workflow keeps its form (`pg_dump
  -Fc`, gpg AES256, encrypted artifact only) but reaches the database
  over SSH instead of a public connection string, one dump per
  environment database.
- **Secrets** live in GitHub secrets and an env file on the server
  (owner `deploy`, mode 0600; `deploy` runs compose and is
  root-equivalent through the `docker` group). `docker/README.md`
  lists each one. None in the repo.
- **DNS** stays at Cloudflare. The api records are DNS-only (grey
  cloud), so TLS ends on our nginx.

### Licences (commercial use allowed for all)

| Component | Licence |
|---|---|
| nginx | BSD-2-Clause |
| certbot | Apache 2.0 |
| Let's Encrypt certificates | free, no use restrictions |
| PostgreSQL | PostgreSQL Licence (BSD-style) |
| Docker Engine | Apache 2.0 |
| Docker Compose | Apache 2.0 |
| ufw | GPL-3.0, server only |
| unattended-upgrades | GPL-2.0, server only |
| Debian (Ubuntu also supported) | free to use; components under OSS licences, server only |

We only run these tools on the server and never ship them in the app, so their
licences put no conditions on selling the app.

## Consequences

- We patch and watch the box ourselves. unattended-upgrades covers
  security updates, and a free uptime check covers outages.
- One server is a single point of failure. Accepted for v1: backups
  live off the box, and a rebuild is the setup script, compose and a
  restore.
- No cold starts. The keep-awake ping goes away with Render.
- Test and prod share CPU and RAM. A test load test can slow prod.
  Test gets a smaller heap.
- Moving to another host stays cheap: the same script and compose on a
  new box, a restore, and a DNS change. No app update needed.
- Render, Neon and `render.yaml` are removed after the cutover (Plan 5
  steps 5–6). Neon stays for a few weeks first as a fallback.
- Cost goes from €0 to €3.88/month.

# Infrastructure

How GymBuddy runs: environments, the server, nginx, how a request and
a workout travel, CI/CD and backups. The reasons behind it are in
ADR 0016.

This page describes the design only. Host details, accounts and
secrets are kept outside the repo.

## 1. The big picture

```mermaid
flowchart LR
  subgraph Phone
    A[Android app<br/>local DB + outbox]
  end
  FB[Firebase Auth]
  DNS[DNS provider]
  subgraph VPS[VPS, Docker]
    N[nginx<br/>TLS, HTTPS only]
    BP[backend prod]
    BT[backend test]
    DP[(Postgres prod)]
    DT[(Postgres test)]
    C[certbot]
  end
  GH[GitHub Actions<br/>CI, deploy, backup]
  REG[Container registry]
  LE[Let's Encrypt]

  A -- sign in --> FB
  A -- lookup --> DNS
  A -- HTTPS + ID token --> N
  N --> BP --> DP
  N --> BT --> DT
  BP -. verify token .-> FB
  BT -. verify token .-> FB
  C <-- ACME --> LE
  GH -- push image --> REG
  GH -- SSH: deploy, backup --> VPS
  VPS -- pull image --> REG
```

- **One server, two environments.** Prod and test run side by side,
  each with its own backend, database, credentials and Firebase
  project. They share only nginx and the machine.
- **The app talks only to nginx.** Databases and backends have no
  public port.
- **One image everywhere.** CI builds the backend image once. It is
  configured only through environment variables and doesn't know
  where it runs.

## 2. Environments

| | Prod | Test | Local |
|---|---|---|---|
| API | `https://api.rukavina.app` | `https://api-test.rukavina.app` | `http://localhost:8080` |
| Backend | own container | own container, smaller heap | Docker Compose |
| Database | own Postgres | own Postgres | Postgres in Compose |
| Firebase | prod project | test project | either |
| Android flavor | `prod` | `qa` ("GymBuddy Test") | URL override |
| Backups | nightly, encrypted | none (disposable) | none |

The `qa` and `prod` apps install side by side on one phone. A `qa`
build can't touch prod data: it uses another URL and another Firebase
project, so prod rejects its tokens.

## 3. The server

One small VPS (2 vCPU, 2 GB RAM plus swap). It is set up from a
script, never by hand:

- SSH by key only, no password login.
- Firewall: only SSH, HTTP and HTTPS are open.
- Automatic security updates and log rotation.
- A non-root, no-sudo user runs the containers. CI has its own key.

Docker writes firewall rules that bypass the host firewall, so the
rule is: **only nginx publishes ports.** Everything else stays on
internal Docker networks.

### Containers and networks

```mermaid
flowchart TB
  NET((Internet))
  subgraph P[Prod stack]
    N[nginx]
    BP[backend]
    DP[(Postgres)]
    C[certbot]
  end
  subgraph T[Test stack]
    BT[backend]
    DT[(Postgres)]
  end
  E{{shared edge network}}

  NET --> N
  N --> BP --> DP
  N --- E --- BT
  BT --> DT
  C -. shared cert volume .- N
```

- Each stack has its own private network. Each database is reachable
  only from its own backend.
- A shared edge network connects nginx with the test backend only, so
  test can never reach prod's database.
- nginx reaches each backend by a unique name per environment.
  Two services with the same name on one network would make requests
  land in the wrong environment. A local smoke test caught exactly
  that.

The whole box idles at about half its memory.

## 4. How nginx works here

nginx is the front door. It ends TLS, picks prod or test by host
name, and forwards plain HTTP to a backend inside Docker. Its config is
a template: the host names are filled in at start from the
environment.

```mermaid
flowchart TD
  R[Request] --> P{Port}
  P -- 80 --> H80{What is it?}
  H80 -- cert challenge --> W[answer for Let's Encrypt]
  H80 -- our host --> RD[redirect to HTTPS]
  H80 -- anything else --> X[close, no answer]
  P -- 443 --> SNI{TLS host name}
  SNI -- unknown --> REJ[refuse handshake]
  SNI -- prod host --> BP[proxy to prod backend]
  SNI -- test host --> BT[proxy to test backend]
```

- **Unknown hosts get nothing.** Scanners that hit the bare IP see no
  page and no certificate.
- **Our hosts:** HTTP/2, HSTS and a small request size limit. nginx
  passes the client IP and protocol to the backend in the usual
  `X-Forwarded-*` headers.
- **Deploys need no reload.** nginx looks each backend up through
  Docker's DNS per request, with a short cache. When a deploy replaces
  a backend container, nginx finds the new one by itself. If test is
  down, test answers 502 and prod is unaffected.
- **One certificate for both names.** The test host can't point at a
  missing certificate and stop nginx (and with it prod) from starting.

### Certificates

```mermaid
sequenceDiagram
  participant CB as certbot (twice a day)
  participant LE as Let's Encrypt
  participant NX as nginx
  CB->>LE: renew if close to expiry
  LE->>NX: fetch challenge token over HTTP
  NX-->>LE: token (from a shared volume)
  LE-->>CB: new certificate
  Note over NX: reloads itself regularly<br/>and picks up the new cert
```

The first certificate comes from a one-time script. After that,
renewal is automatic, and no container needs access to Docker itself.

## 5. How a request travels

Example: the app pulls changes.

```mermaid
sequenceDiagram
  participant App as Android app
  participant FB as Firebase Auth
  participant NX as nginx
  participant BE as backend
  participant DB as Postgres
  App->>FB: get ID token (cached, auto-refreshed)
  App->>NX: HTTPS GET /v1/sync/pull<br/>Authorization: Bearer token
  NX->>BE: plain HTTP inside Docker
  BE->>BE: rate limit, verify token (Firebase Admin SDK)
  BE->>DB: changes after the cursor
  DB-->>BE: rows
  BE-->>NX: JSON page + next cursor
  NX-->>App: HTTPS response
```

The backend is Spring Boot. Its parts:

| Part | Job |
|---|---|
| auth | Token check, per-user rate limit, creates the user on first call |
| api | Controllers, error format, request IDs in logs |
| sync | Push/pull, change log and cursor, account deletion |
| persistence | Database access |
| domain | Rules and validation |

The database schema is versioned. Migrations run when the backend
starts.

## 6. API

The contract is `api/openapi.yaml`. It is written by hand, the Android
client is generated from it, and a backend test fails if the code
drifts from it. Published docs: https://rukavina-git.github.io/GymBuddy/

| Endpoint | What |
|---|---|
| `GET /v1/health` | Liveness, no auth |
| `GET /v1/me` | Current user's profile |
| `DELETE /v1/account` | Delete the account and all its data |
| `POST /v1/sync/push` | Upload local changes |
| `GET /v1/sync/pull` | Download changes since a cursor |
| `GET /v1/reference/*` | Built-in exercises, templates, catalogue version |

Wire rules:
- timestamps are epoch milliseconds
- units are metric
- ids are UUIDv7, made by the client
- the server sets the change times and revisions
- deletes are tombstones
- the cursor is opaque

## 7. How a workout travels (sync)

The app is offline-first: it saves everything on the phone first and
syncs when there is a network.

```mermaid
sequenceDiagram
  participant UI
  participant DB as Local DB
  participant OB as Outbox
  participant W as Background job
  participant API as Backend
  UI->>DB: save set / finish workout
  DB->>OB: note what changed
  OB->>W: schedule push (needs network)
  W->>API: push whole workout
  API->>API: server resolves conflicts,<br/>stamps time + revision
  API-->>W: accepted
  W->>API: pull changes since cursor
  API-->>W: changes
  W->>DB: server version overwrites local
```

The sync rules come from ADRs 0006–0010:
- Push first, then pull, never both at once.
- The server owns conflict resolution and every timestamp.
- A workout or template syncs as one whole tree.
- Guests keep their data on the phone until they sign up.

## 8. CI/CD

```mermaid
flowchart LR
  D[push] --> CI[tests]
  CI --> IMG[image to registry,<br/>tagged by commit]
  IMG --> MAN[deploy workflow<br/>manual: env + tag]
  MAN -- SSH --> DS[deploy script on server]
  DS --> PULL[pull] --> UP[swap backend] --> HC{healthy?}
  HC -- yes --> OK[done]
  HC -- no --> RB[roll back, fail]
```

- Deploys are manual for now and default to test. Auto-deploy is
  planned.
- A failed health check puts the previous image back by itself.
- Other checks: Android build and unit tests, end-to-end sync tests,
  CodeQL, Sonar, OWASP dependency check, API lint and docs.

## 9. Backups

```mermaid
flowchart LR
  CRON[GitHub Actions, nightly] -- SSH --> VPS
  VPS -- pg_dump --> GPG[encrypted on the runner]
  GPG --> ART[encrypted artifact,<br/>kept 30 days]
```

- Prod only; test data is disposable.
- The dump is encrypted before it is stored anywhere. Only the
  encrypted file is kept.

## 10. Monitoring

- An external uptime check on the health endpoint.
- Structured JSON logs from the backend, rotated on the box.
- Crash reporting on Android.

## 11. History

| When | What |
|---|---|
| Plan 1 | First host: a free PaaS plus a free managed Postgres |
| 2026-11-11 | ADR 0016: move to one self-hosted VPS, split test and prod |
| 2026-11-12 | Prod moved to the VPS, old hosting shut down |
| 2026-11-12 | Test environment live, Android `qa` flavor |

## 12. Third-party pieces

All are commercial-friendly: nginx (BSD-2), certbot (Apache 2.0),
PostgreSQL (PostgreSQL licence), Docker Engine and Compose
(Apache 2.0). The full table is in ADR 0016.

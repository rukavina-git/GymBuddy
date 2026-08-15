# GymBuddy

Offline-first workout tracker. Native Android and iOS clients against a
Kotlin backend, sharing nothing but an OpenAPI contract.

**[API documentation →](https://rukavina-git.github.io/GymBuddy/)**

## Why it's built this way

Your training data is yours. The app works completely without an
account: everything you log lives on your device and stays there.
Creating an account is opt-in, and it exists so you can back your data
up and use it on more than one device — not so it can be collected.

That makes local storage the source of truth rather than a cache, and
a server-side copy an optional mirror. The rest of the architecture
follows from that:

- **Client-generated UUIDv7 identifiers**, because a workout logged
  offline can't wait for the server to assign a key.
- **Soft deletes with tombstones**, because a deletion made offline has
  to propagate to your other devices — an absent row is indistinguishable
  from one that never existed.
- **Snapshotted display fields in workout history**, so renaming an
  exercise doesn't retroactively rewrite what you lifted last year.
- **User preferences in overlay tables**, so a reference-data update
  can't clobber which exercises you've hidden.
- **No per-entity CRUD endpoints.** Writes queue in an outbox and drain
  through a single sync endpoint, so the online and offline paths are
  the same path.
- **Server-authoritative conflict resolution.** Clients never merge —
  the duplicated logic would be paid for twice, in two languages.

## Features

- Exercise library with search, filtering, personal notes and hiding
- Custom exercises and reusable workout templates
- Six tracking types — weight and reps, reps only, duration, distance,
  and their weighted variants — so a plank and a farmer's walk are
  first-class, not reps in disguise
- Immutable workout history that survives changes to the exercise library
- Metric and imperial display, with metric storage and lossless conversion
- Optional account with Google, email or Apple sign-in
- Multi-device sync with in-app account deletion

## Repository

| Path                   | Contents                                            |
|------------------------|-----------------------------------------------------|
| [`api/`](api/)         | OpenAPI 3.1 contract, tooling, published docs       |
| [`android/`](android/) | Native Android client — Kotlin, Compose, Hilt, Room |
| [`backend/`](backend/) | Kotlin, Spring Boot, PostgreSQL                     |
| [`ios/`](ios/)         | Native iOS client — Swift, SwiftUI                  |
| [`docs/`](docs/)       | Architecture decision records                       |
| [`docker/`](docker/)   | Compose files for local development                 |

The contract is hand-authored and committed before implementation. Both
clients are generated from it; the backend implements against it, and CI
fails the build if the two drift.

## Status

| Component        | State                                                      |
|------------------|------------------------------------------------------------|
| API contract     | Complete                                                   |
| Android          | Schema and offline layer complete; sync engine not started |
| Backend          | In progress                                                |
| iOS              | Not started                                                |
| CI/CD            | Android and API builds; release automation not started     |
| Deployment       | Local only                                                 |

## Stack

**Android** — Kotlin, Jetpack Compose, Hilt, Room, Navigation, Coroutines
and Flow. Firebase Auth with Credential Manager. JaCoCo, 85%
domain-layer coverage.

**Backend** — Kotlin, Spring Boot, PostgreSQL, Flyway, Firebase Admin
SDK. Testcontainers.

**iOS** — Swift, SwiftUI.

**Infrastructure** — Docker Compose, nginx, GitHub Actions. Fastlane for
release automation, SonarQube for static analysis.

---

Ivan Karlo Rukavina · [rukavina.app](https://rukavina.app) · [ivankarlo.rukavina@gmail.com](mailto:ivankarlo.rukavina@gmail.com)
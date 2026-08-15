# API Contract

OpenAPI 3.1 specification for the GymBuddy sync API. Hand-authored — this
file is the contract, not a by-product of the backend. The Kotlin and
Swift clients are generated from it, and the backend implements against it.

Published docs: https://rukavina-git.github.io/GymBuddy/

## Commands

    npm ci                    install tooling
    npm run lint              validate with Spectral
    npm run docs              build a local HTML copy
    npm run generate:kotlin   generate the Retrofit client

## Conventions

- All timestamps are epoch milliseconds.
- Weights are kilograms, heights centimetres, distances metres.
  Conversion happens in the client UI, never on the wire.
- `updatedAt`, `deletedAt` and `revision` are server-assigned and
  marked `readOnly`. Clients must never set them.
- The sync cursor is opaque. Clients must not parse, construct, or
  compare cursors.
- Deletions arrive as entities with a non-null `deletedAt`, not as a
  separate list.
- Workout sessions and templates sync as whole aggregates; their nested
  children carry no sync metadata of their own.
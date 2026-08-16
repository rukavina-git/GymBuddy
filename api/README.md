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
- `updatedAt` and `revision` are server-assigned and marked
  `readOnly`. Clients must never set them.
- `deletedAt` is server-assigned and `readOnly` everywhere except on
  push for WorkoutSession, Exercise, and WorkoutTemplate, where it
  doubles as the client's delete signal: set it to any non-null value
  to delete the entity. The server ignores the value sent and always
  stamps its own current time - what pull returns is authoritative.
  `userProfile`'s `deletedAt` stays `readOnly` (account deletion is a
  separate endpoint); `userExerciseState`/`userTemplateState` have no
  `deletedAt` at all (upsert-only, no delete).
- The sync cursor is opaque. Clients must not parse, construct, or
  compare cursors.
- On pull, deletions arrive as entities with a non-null `deletedAt`,
  not as a separate list.
- Workout sessions and templates sync as whole aggregates; their nested
  children carry no sync metadata of their own.
# 4. Native clients rather than Kotlin Multiplatform

Date: 2026-08-08
Status: Accepted

## Context

The app targets both Android and iOS. Kotlin Multiplatform would allow
sharing domain models, use cases and repositories between them, and
Compose Multiplatform would allow sharing the UI as well.

At the time of the decision the Android domain layer was 808 lines, most
of it thin CRUD coordination over repository interfaces — precisely the
code that migrates into a backend once one exists.

## Decision

Two fully independent native clients. Kotlin and Compose on Android,
Swift and SwiftUI on iOS. Nothing is shared between them except the
OpenAPI contract.

## Consequences

- The offline persistence layer and sync engine are implemented twice,
  once per platform. This is the real cost and it is not small.
- `Uuid7Generator`, `WorkoutSetValidator` and the unit conversion logic
  also need Swift equivalents.
- The sync protocol is deliberately kept simple to reduce what has to be
  duplicated — see ADR 8 and ADR 9.
- No Objective-C interop boundary, no SKIE or KMP-NativeCoroutines, no
  expect/actual declarations, no multiplatform build toolchain.
- Each platform's UI is idiomatic rather than a shared abstraction
  rendered twice.
- The domain layer has since grown past 1,400 lines, so the duplication
  is larger than the original estimate assumed. The decision still
  holds, but the margin is thinner than it was.

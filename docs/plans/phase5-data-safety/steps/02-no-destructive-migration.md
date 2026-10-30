# 02 Remove the destructive migration fallback

## Problem
`AppModule.provideDatabase` calls `.fallbackToDestructiveMigration()`
(comment: "For development - remove in production"). The first schema
bump after release would silently delete every tester's workouts and
unsynced outbox. R-11 in the architecture plan.

## Change
- Remove `.fallbackToDestructiveMigration()`. No destructive fallback
  in debug either, so a missing migration fails in development, where
  it's cheap.
- Add a test that fails when `AppDatabase`'s current version has no
  migration path from 26: for each version from 26 to current, a
  `Migration(from, to)` exists in `ALL_MIGRATIONS`, and
  `MigrationTestHelper.runMigrationsAndValidate` passes from 26 to
  current. At version 26 this passes trivially; the test is the gate
  for the next bump.
- android/CLAUDE.md: a short rule for schema changes: bump version,
  export schema, add a `Migration` and a test, never a destructive
  fallback.

## Tests
- The migration-path test above.
- A throwaway check in the session (not committed): bump to 27 with
  a trivial column add and no migration → the test fails; add the
  migration → it passes. Record the result in progress.md.

## Out of scope
Any real schema change.

## Done when
`./gradlew testDebugUnitTest` green, `grep -rn fallbackToDestructive
app/src` finds nothing.

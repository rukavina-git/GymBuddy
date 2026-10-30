# 01 One DB builder, schema export, v26 baseline

## Problem
There are two Room builders for `AppDatabase`, with different file
names:
- `di/AppModule.provideDatabase`: `"gym_buddy_db"`,
  `.fallbackToDestructiveMigration()`. This is the one Hilt injects,
  so it holds every installed device's data.
- `AppDatabase.getDatabase` (companion): `"gym_app_database"`,
  `.fallbackToDestructiveMigration(false)`. No caller in app/src.

`exportSchema = false`, so there is no schema JSON to migrate from or
test against. Version is 26.

## Change
- Delete the `AppDatabase` companion (`INSTANCE`, `getDatabase`).
  `AppModule` is the only builder. Keep the file name `gym_buddy_db`.
- Move the builder's configuration into one place both production and
  tests can use, e.g. `AppDatabase.configure(builder)` or a
  `DatabaseFactory` in data/local/db, so migrations are added in one
  spot. `fallbackToDestructiveMigration()` stays for now (step 02
  removes it).
- `exportSchema = true`. Use the Room Gradle plugin
  (`androidx.room`, same version as `room`) with
  `room { schemaDirectory("$projectDir/schemas") }`, replacing any
  `ksp { arg("room.schemaLocation", ...) }`.
- Commit `app/schemas/.../26.json` as the baseline.
- Add the schema dir to the unit-test assets so `MigrationTestHelper`
  runs under Robolectric.
- Add an empty `Migrations.kt` (`val ALL_MIGRATIONS: Array<Migration>`)
  wired into the builder.

## Tests
- `MigrationTestHelper` smoke test (Robolectric): create v26 from the
  exported schema, open it with the production configuration, assert
  every table exists.
- Existing DAO and sync tests still pass (they use
  `inMemoryDatabaseBuilder`; leave them unless they need the shared
  configuration).

## Out of scope
Removing the destructive fallback (step 02), any schema change.

## Done when
`./gradlew testDebugUnitTest` green, `app/schemas/.../26.json`
committed, `grep -rn databaseBuilder app/src/main` finds one builder.

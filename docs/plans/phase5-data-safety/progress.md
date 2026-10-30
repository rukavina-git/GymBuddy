# Progress

- [x] 01 One DB builder, schema export, v26 baseline (steps/01-one-db-builder.md) (ef70690)
- [x] 02 Remove the destructive migration fallback (steps/02-no-destructive-migration.md) (85606bc)
- [x] 03 Crashlytics (steps/03-crashlytics.md) (5ea9e45)
- [ ] 04 Backend JSON logs, sync metrics, client headers (steps/04-backend-observability.md)
- [ ] 05 CI green, PR develop → main (me) (steps/05-merge-to-main.md)

## Deviations
- 01: schemas dir is added to the `debug` source set's assets, not
  `test`. Robolectric only reads `mergeDebugAssets`; test-source-set
  assets never reach it. Debug APKs carry the schema JSON; release
  doesn't.
- 01: shared config is an extension, `applyAppConfig()` in
  data/local/db/DatabaseConfig.kt, plus `DATABASE_NAME`.
- 01: the baseline test also checks a `sync_meta` row survives opening
  (proves no destructive wipe), beyond "every table exists".
- 02: the fallback was in DatabaseConfig.kt (moved by step 01), not
  AppModule. `MigrationPathTest` reads the current version from an
  in-memory Room DB (`openHelper.writableDatabase.version`) because
  `@Database` isn't retained at runtime.
- 02: throwaway check (not committed): v27 + `sync_meta.note`, no
  migration → path, validate and baseline tests all fail. With
  `Migration(26, 27)` → all pass. With a no-op migration → validate
  fails ("Migration didn't properly handle: sync_meta"). Reverted.
- 02: 26.json vs shipped v26 checked: no `@Entity` file changed after
  the v26 bump (7a59788), so 26.json is the shipped schema.
- 03: collection is off in debug via a manifest placeholder
  (`crashlyticsEnabled`), not `setCrashlyticsCollectionEnabled` at
  startup. This also covers crashes that happen before `onCreate`.
- 03: "offline" means any `IOException` in the cause chain. SyncEngine
  wraps every IOException in `SyncException(cause = e)`, so a bare
  `is IOException` check would never match.
- 03: the logout report has counts and ids only, no payloads (the
  step's recommendation). Ids go in per-call custom keys
  `discarded_<TYPE>` (comma-joined), the error is
  `DiscardedOnLogoutException`.
- 03: crash reporter is a constructor param with a `NoOpCrashReporter`
  default, so existing test call sites didn't change.
- 03: mapping upload is off unless `-Pgymbuddy.uploadMapping=true`.
- 03: no test for `setUserId` in `FirebaseAuthSession.observeSignIn`
  (needs a real FirebaseAuth).
- 03: reviewer findings not fixed (no blockers):
  - id keys truncate at 1024 chars (about 27 UUIDs per type)
  - the IOException filter also hides TLS errors and malformed JSON
  - `reportDiscarded()` isn't guarded, so a reporter failure blocks
    logout

## Gotchas
- From phase4-closeout: the guard hook greps the whole Bash command,
  so text with " main ", "deploy" or a secret file name
  (google-services.json) is blocked. Write docs and PR bodies with
  Write/Edit; if `gh pr create` is blocked, ask me to run it.
- From phase4-closeout: on Render `autoDeploy: false`; backend changes
  need a Manual Deploy.
- From phase4-closeout: ViewModel tests using
  `stateIn(WhileSubscribed(5_000))` over Room must cancel
  `viewModelScope` in `@After`.
- 01: `MigrationTestHelper` needs `InstrumentationRegistry
  .getInstrumentation()` for context; `androidx.test:core`
  (ApplicationProvider) isn't on the unit-test classpath.
- 01: `MigrationBaselineTest.EXPECTED_TABLES` is the v26 list by hand.
  Step 02's path test should derive tables from the latest schema JSON
  instead of reusing it.
- 01: 26.json is generated from today's entities. It's the shipped v26
  only if no entity changed since the last tester build without a
  version bump; worth a check in step 02.
- Root `/local.properties` is untracked and not in the root
  .gitignore. Don't stage it.
- 1.6 (device checks against the hosted backend) is still open in
  Plan 1. Doing them before step 02 means they test today's DB setup.
- 02: without the fallback, any version bump without a migration
  crashes on open, in debug too. `MigrationPathTest` only accepts
  single steps `Migration(n, n+1)`.
- 02: the first build after a version bump exports `<n>.json` after
  assets merge; Robolectric may report "Cannot find the schema file"
  until the next run. Commit the new JSON.
- 03: tester or release builds without `-Pgymbuddy.uploadMapping=true`
  give obfuscated stack traces in the Firebase console. Add it to the
  release checklist.
- 03: Crashlytics custom key values are capped at 1024 chars.
  Discarded ids beyond that are lost silently.

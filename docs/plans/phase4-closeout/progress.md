# Progress

- [x] 01 Account deletion reaches the backend (steps/01-account-deletion.md) (da09768)
- [x] 02 Sync debug screen (steps/02-sync-debug-screen.md) (dcb57db)
- [x] 03 Hosting config and backups (steps/03-hosting-config.md)
- [ ] 04 Accounts, first release, DNS, restore test (me) (steps/04-host-setup.md)
- [ ] 05 Release build and spec point at api.rukavina.app (steps/05-real-host-url.md)
- [ ] 06 Device checks against the hosted backend (me, deferred) (steps/06-device-checks.md)
- [ ] 07 CI green, PR develop → main (me) (steps/07-merge-to-main.md)

## Deviations
- 01: the coordinator takes `deleteOnServer: suspend () -> Int` (HTTP
  status) and `deleteProfileImages` lambdas instead of the API object, so
  tests fake the server without OkHttp.
- 01: any status other than 204/401 (403, 5xx) is `Retryable`.
- 01: after a 204 the coordinator remembers the server delete, so if the
  wipe throws, a retry only wipes (the server would answer 401 by then).
  This is the "account provably already gone" case. Cleared after a
  successful wipe.
- 01: `SettingsViewModel` no longer injects `AppDatabase`.
- 01: no Figma frame. The step changes dialogs only, and Figma starts
  with Plan 3.
- 02: "Sync now" calls `SyncRequester.syncNow(MANUAL)`. `SyncTriggers`
  has no manual trigger to go through.
- 02: `SyncException` gained `traceId`, parsed from the error body in
  `SyncEngine.bodyOf`. Nothing captured it before.
- 02: `SyncScheduler` inner loop is now
  `while (rerunRequested.getAndSet(false))`. The status write widened an
  existing late-claim race (an already-served request claimed `running`
  and ran an extra empty cycle), and `SyncSchedulerTest` started failing.
- 02: the nav route is also behind `BuildConfig.DEBUG`, not only the
  Settings row.
- 02: a "Signed out, no sync ran" note when "Sync now" is skipped.
- 03: `render.yaml` also sets `PORT=8080` (Render routes to `PORT`;
  Spring ignores it). `autoDeploy: false`, region frankfurt.
- 03: the workflow runs `pg_dump` in a `postgres:17` container, not the
  runner's client, so the client version is pinned and at least Neon's.
- 03: `restore.sh` reads `BACKUP_PASSPHRASE` from env or prompts;
  passes `--if-exists --exit-on-error` besides `--clean --no-owner`.
- 03: no backend change. pgjdbc already accepts
  `?sslmode=require&user=` through `DATABASE_URL`.
- 03: round-trip tested with postgres:16 tools against compose
  postgres:16 (throwaway compose project, torn down after).

## Gotchas
- 01: reviewer SHOULD items left open: the delete runs in viewModelScope,
  so clearing the VM between the 204 and the wipe cancels the wipe; 403
  shows the "couldn't reach the server" message; the images are deleted
  on the caller's dispatcher (Main); the unsynced-changes list isn't
  cleared before the dialog reopens.
- 02: reviewer items left open: `SyncEngine.traceIdOf` has no engine
  test (Error body → traceId, non-JSON body → null); `runAndRecord`
  catches CancellationException, so leaving the debug screen mid-sync
  records FAILED "Job was cancelled"; `SyncDebugUiState.isEmpty` is
  unused by the screen; the debug VM uses Room entities directly.
- 02: ViewModel tests that use `stateIn(WhileSubscribed(5_000))` over
  Room must cancel `viewModelScope` in `@After`. Otherwise the
  collectors outlive the test and a later test fails with
  "Dispatchers.Main is used concurrently with setting it".
- 02: the debug screen shows last sync time/outcome only for cycles
  since process start (in-memory `SyncScheduler.status`).
- 03: a pg_dump 17 dump does not restore into a Postgres 16 server
  (`transaction_timeout`). Create the Neon project on 17 (matches the
  workflow image), or change the image tag to Neon's version. Local
  compose is still 16.
- 03: reviewer SHOULD items left open: `restore.sh` has no `--no-acl`,
  so a Neon dump with GRANTs to Neon-only roles (`neon_superuser`)
  would stop partway when restored into compose/VPS. Check this with
  the real dump in step 04. GitHub disables cron workflows in public
  repos after 60 days without activity, and the schedule only runs once
  the file is on main.
- 03: reviewer NITs left open: the VPS checklist should repeat the
  version bump and say to stop `backend` before restoring;
  `--no-owner` on `pg_dump -Fc` does nothing; `host.docker.internal` in
  the README example needs `--add-host=...:host-gateway` on Linux.
- 04: set the Neon project's region to EU (Frankfurt) to sit near the
  Render service.
- From phase4-hardening: the guard hook greps the whole command, so
  text containing " main " or "deploy" in a Bash command is blocked.
  Write docs with Write/Edit, and push in its own command.

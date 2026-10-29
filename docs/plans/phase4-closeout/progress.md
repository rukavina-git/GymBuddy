# Progress

- [x] 01 Account deletion reaches the backend (steps/01-account-deletion.md) (da09768)
- [x] 02 Sync debug screen (steps/02-sync-debug-screen.md)
- [ ] 03 Hosting config and backups (steps/03-hosting-config.md)
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
- From phase4-hardening: the guard hook greps the whole command, so
  text containing " main " or "deploy" in a Bash command is blocked.
  Write docs with Write/Edit, and push in its own command.

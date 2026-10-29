# Progress

- [x] 01 Account deletion reaches the backend (steps/01-account-deletion.md)
- [ ] 02 Sync debug screen (steps/02-sync-debug-screen.md)
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

## Gotchas
- 01: reviewer SHOULD items left open: the delete runs in viewModelScope,
  so clearing the VM between the 204 and the wipe cancels the wipe; 403
  shows the "couldn't reach the server" message; the images are deleted
  on the caller's dispatcher (Main); the unsynced-changes list isn't
  cleared before the dialog reopens.
- From phase4-hardening: the guard hook greps the whole command, so
  text containing " main " or "deploy" in a Bash command is blocked.
  Write docs with Write/Edit, and push in its own command.

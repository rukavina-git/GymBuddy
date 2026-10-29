# Progress

- [ ] 01 Account deletion reaches the backend (steps/01-account-deletion.md)
- [ ] 02 Sync debug screen (steps/02-sync-debug-screen.md)
- [ ] 03 Hosting config and backups (steps/03-hosting-config.md)
- [ ] 04 Accounts, first release, DNS, restore test (me) (steps/04-host-setup.md)
- [ ] 05 Release build and spec point at api.rukavina.app (steps/05-real-host-url.md)
- [ ] 06 Device checks against the hosted backend (me, deferred) (steps/06-device-checks.md)
- [ ] 07 CI green, PR develop → main (me) (steps/07-merge-to-main.md)

## Deviations
None yet.

## Gotchas
- From phase4-hardening: the guard hook greps the whole command, so
  text containing " main " or "deploy" in a Bash command is blocked.
  Write docs with Write/Edit, and push in its own command.

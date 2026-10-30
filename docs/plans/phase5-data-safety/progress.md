# Progress

- [ ] 01 One DB builder, schema export, v26 baseline (steps/01-one-db-builder.md)
- [ ] 02 Remove the destructive migration fallback (steps/02-no-destructive-migration.md)
- [ ] 03 Crashlytics (steps/03-crashlytics.md)
- [ ] 04 Backend JSON logs, sync metrics, client headers (steps/04-backend-observability.md)
- [ ] 05 CI green, PR develop → main (me) (steps/05-merge-to-main.md)

## Deviations
None yet.

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
- 1.6 (device checks against the hosted backend) is still open in
  Plan 1. Doing them before step 02 means they test today's DB setup.

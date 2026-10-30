# Roadmap

The short version. What each step means and why it's there:
[roadmap-details.md](roadmap-details.md). Backend + Android only; iOS
starts after Android closed testing.

Legend: [x] done · [ ] to do · (me) manual, done by me · (deferred)
waiting on me

## Done

- [x] Phase 0: monorepo
- [x] Phase 1: offline-first schema (favourites and rest timer are v2)
- [x] Phase 2: OpenAPI contract, 8 endpoints
- [x] Phase 3: backend (sync, reference, /me, account deletion,
      tombstone job, rate limit, drift test)
- [x] Phase 4: Android sync engine (outbox, push, pull, 410 resync,
      reference data, triggers, logout flush-and-warn)
- [x] Phase 4 hardening (`docs/plans/phase4-hardening`), steps 01–04
- [x] Claude setup: CLAUDE.md, /step, reviewer, guard hook

## Plan 1: phase4-closeout

- [x] 1.1 Account deletion calls the backend, wipes under sync lock
- [x] 1.2 Sync debug screen
- [x] 1.3 Deploy config for the free host, nightly backup, restore script
- [x] 1.4 (me) Create host + database accounts, deploy, DNS for
      api.rukavina.app, test a restore
- [x] 1.4a restore.sh skips Neon GRANTs (--no-acl)
- [x] 1.5 Release build and spec point at api.rukavina.app (spec change)
- [x] 1.5a (me) Uptime ping keeps the free Render instance awake
- [ ] 1.6 (me, deferred) Device checks: hardening list, two devices,
      airplane mode
- [x] 1.7 (me) CI green, PR develop → main

## Plan 2: phase5-data-safety

- [x] 2.1 One DB builder, schema export, v26 baseline, migration tests
- [x] 2.2 Remove destructive migration fallback
- [x] 2.3 Crashlytics (crashes, sync non-fatals, discarded logout data)
- [x] 2.4 Backend JSON logs, sync metrics, client version headers
      (spec change)
- [ ] 2.5 (me) CI green, PR develop → main

## Plan 3: figma-foundation

- [x] 3.1 (me) Figma file + connect the Figma MCP server to Claude Code
- [ ] 3.2 Design tokens: current theme as Figma variables
- [ ] 3.3 Component library in Figma, matching ui/components
- [ ] 3.4 Every v1 screen as a Figma frame
- [ ] 3.5 Code reads tokens from Figma; UI steps link a frame from now on

## Plan 4: phase5-release

- [ ] 4.0 Dependency CVEs: Dependabot alerts + OWASP, restore OWASP triggers
- [ ] 4.1 ViewModel test gaps (active workout failure paths)
- [ ] 4.2 Fix ImageStorageUtil data-loss bug
- [ ] 4.2a Auth gate: dead session → Login with "session expired"
- [ ] 4.2b Durable push: WorkManager job when the outbox is non-empty
- [ ] 4.3 Tag-triggered signed release AAB workflow
- [ ] 4.4 (me) Release build manual pass
- [ ] 4.5 (me) Privacy policy online + in-app link, Play Console, Data Safety

## Plan 5: phase5-closed-testing

- [ ] 5.1 (me) Closed testing track (check current tester rule)
- [ ] 5.2 Fix tester findings
- [ ] 5.3 (me) Full manual pass → Android v1 done, start iOS planning

## Later

- Move to paid hosting (e.g. DigitalOcean): restore dump, repoint DNS
- Phase 6 iOS (designs already in Figma), Phase 7 store launch
- v2 list: architecture plan §14.2

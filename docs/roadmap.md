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
- [x] 1.6 (me) Device checks: hardening list, two devices,
      airplane mode
- [x] 1.7 (me) CI green, PR develop → main

## Plan 2: phase5-data-safety

- [x] 2.1 One DB builder, schema export, v26 baseline, migration tests
- [x] 2.2 Remove destructive migration fallback
- [x] 2.3 Crashlytics (crashes, sync non-fatals, discarded logout data)
- [x] 2.4 Backend JSON logs, sync metrics, client version headers
      (spec change)
- [x] 2.5 (me) CI green, PR develop → main

## Plan 3: figma-foundation

- [x] 3.1 (me) Figma file + connect the Figma MCP server to Claude Code
- [x] 3.2 Direction: Soft Dark locked (lime, cobalt, #121418), dark only in v1
- [x] 3.2b Colours file + Do/Don't list + PRODUCT.md palette
- [x] 3.3 Tokens as Figma variables + design/tokens.json
- [x] 3.4 Brand and icons (app icon, wordmark, gym glyphs). Logo: own ECG
      line (owner 2026-11-02, replaced the animal idea)
- [x] 3.5 Component library (M3-based, on the tokens)
- [x] 3.6 Screens per flow: app frame, Home, Templates, Active workout,
      Exercise page + create, Session detail, Progress, You + Settings,
      Onboarding + auth (incl. session expired), error/offline states
- [x] 3.7 Motion and haptics spec
- [x] 3.8 DESIGN.md + redesign handoff (now Plan 7)

Order (owner, 2026-11-03): fix what's there → features → redesign →
test + prod environments → CI/CD → legal → release last. Plan ids
kept, so Plan 5 comes after Plan 7.
Old step ids (4.0–4.6, 4.D1–4.D11, old Plan 5) map to the new ones in
roadmap-details.md §3.

## Plan 4: phase5-code-health

- 4.1 OWASP moved to 10.0, just before release (owner 2026-11-02;
      Dependabot is clean, 35/35 fixed)
- [x] 4.2 Remove custom profile photos (preset avatars come in 6.5)
- [x] 4.3 ViewModel test gaps (active workout failure paths)
- [x] 4.4 Durable push: WorkManager job when the outbox is non-empty
- [x] 4.5 (me) CI green, PR develop → main

## Plan 6: v1-features (data + logic)

- [x] 6.1 Per-account local data + account switching rules (ADR 0014)
- [x] 6.2 Guest mode: local-only use, create account / sign in merges (ADR 0014)
- [x] 6.3 Auth gate: dead session → "sign in again" banner, no wipe
- [x] 6.4 Body weight tracking: new table (spec change)
- [x] 6.5 Profile fields: experience, avatar, consent record, days per week
      (spec change, owner OK 2026-11-02)
- [x] 6.6 Plans (templates to rotate through) + rework of the default
      templates and starter plans (data model change)
- [x] 6.7 Data export: versioned JSON (ADR 0015; CSV dropped)
- [x] 6.8 (me) CI green, PR develop → main (PR #6, 596850c)

## Plan 7: redesign build (Compose)

- [x] 7.1 Theme: tokens.json → colour, type (tnum), shape, motion, haptics
- [x] 7.2 Icons + component library (01 System)
- [x] 7.3 App frame: 5-tab nav, top bars, predictive back
- [x] 7.4 Onboarding + auth (UI for 6.2, 6.3)
- [ ] 7.5 Home + Templates (Workouts tab, plans from 6.6)
- [ ] 7.6 Active workout (keypad, live bar, swipe + undo, rest)
- [ ] 7.7 Exercise page + create
- [ ] 7.8 Progress tab (replaces Sessions + Stats)
- [ ] 7.9 Session detail
- [ ] 7.10 You + Settings (UI for 6.4, 6.5, 6.7)
- [ ] 7.11 Cross-flow states: error, offline, empty, sync
- [ ] 7.12 (me) Device pass on the current backend, CI green, PR develop → main

## Plan 5: environments (test + prod), after Plan 7

- [ ] 5.1 ADR: staging + prod split (backend, database, Firebase, app)
- [ ] 5.2 Backend: staging service in render.yaml, backups prod only
- [ ] 5.3 Android: `staging` / `prod` flavors (app id suffix, own
      Firebase config, API URL); CI builds both
- [ ] 5.4 (me) Staging Firebase project, Neon branch, Render service,
      DNS api-staging.rukavina.app, secrets
- [ ] 5.5 (me) CI green, PR develop → main

## Plan 8: delivery (CI/CD + fastlane)

- [ ] 8.1 CI gates: Android Lint, SonarQube (Q-E). OWASP is 10.0
- [ ] 8.2 Signed release AAB on a `v*` tag, versionCode from the tag
- [ ] 8.3 fastlane: staging build to testers, prod AAB to the Play
      internal track, store listing kept in the repo
- [ ] 8.4 Backend CD: develop → staging, `v*` tag → prod
- [ ] 8.5 (me) Play Console app, upload key, service account, secrets
- [ ] 8.6 (me) CI green, PR develop → main

## Plan 9: legal + privacy

- [ ] 9.1 Third-party licence audit (incl. stock exercise photos) +
      in-app open-source licences screen
- [ ] 9.2 Privacy policy + terms at a public URL, linked in-app
- [ ] 9.3 Web account deletion page (Play requires one)
- [ ] 9.4 Compliance pass: health-data consent, 16+ audience,
      Crashlytics payloads, timer/notification rules
      (list: docs/council/2026-11-02-onboarding.md)
- [ ] 9.5 (me) Play Console: Data safety, content rating, audience,
      store listing
- [ ] 9.6 (me) CI green, PR develop → main

## Plan 10: release

- [ ] 10.0 OWASP triage + triggers back on (fix parked on
      `feature/owasp-triage`)
- [ ] 10.1 (me) Release build manual pass on staging, then prod: R8,
      sign-in, guest, sync, logout, account deletion
- [ ] 10.2 (me) Internal testing track via fastlane
- [ ] 10.3 (me) Closed testing (check the current tester rule)
- [ ] 10.4 Fix tester findings (one step per batch)
- [ ] 10.5 (me) Full manual pass, production release → Android v1 done

## Later

- Move to paid hosting (e.g. DigitalOcean): restore dump, repoint DNS
- iOS (designs already in Figma), after Android v1 is live
- v2 list: architecture plan §14.2

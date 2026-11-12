# Status

Current project status, short. Dated audits with full evidence are in
STATUS_YYYY-MM-DD.md files. What's next: roadmap.md (checklist) and
roadmap-details.md (what each step means).

Updated 2026-11-02.

## Where things are

- **Backend:** on the netcup VPS at api.rukavina.app (since 2026-11-12). Sync,
  reference, /me, account deletion, tombstone job, rate limit, spec
  drift test, JSON logs with traceId/userId/client version. One
  environment only (staging comes in Plan 5). Deploys by hand via the
  Deploy workflow (docker/README.md, Hosting).
- **Android:** v1 skeleton UI on the sync engine (outbox, push, pull,
  410 resync, logout flush-and-warn). Room v26 baseline with migration
  tests, no destructive fallback, Crashlytics. Device checks passed
  2026-10-31 (phase5-data-safety progress.md, step 05).
- **Design:** full Soft Dark redesign locked in Figma; build guide is
  design/DESIGN.md. Not built in Compose yet (Plan 7).
- **CI:** green on main at 4349324 (PR #4): Android Build, Backend
  Build, Sync E2E, API Client Codegen, API Docs, CodeQL. OWASP is
  manual-only and non-blocking (ADR 0013). Dependabot: 35/35 fixed.
  develop is 44 commits ahead of main (Plan 3 docs).
- **iOS:** not started.

## Known gaps (planned)

- An offline edit waits for the next app open if the app is swiped
  away (4.4).
- A dead session fails sync silently with 401 (6.3).
- No staging environment; debug builds use prod (Plan 5).
- No release pipeline, store listing, privacy policy or licence record
  for the stock exercise photos (Plans 8, 9).

## Schema history

Phase 1 schema hardening (groups A–J) is done; evidence in
STATUS_2026-09-26.md §4a. Group K favourites and rest timer are v2.

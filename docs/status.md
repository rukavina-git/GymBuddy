# Status

Current project status, short. Dated audits with full evidence are in
STATUS_YYYY-MM-DD.md files. What's next: roadmap.md (checklist) and
roadmap-details.md (what each step means).

Updated 2026-11-25.

## Where things are

- **Stage:** Android v1 in Play closed testing (Plan 10). 0.2.0-0.2.3
  shipped to alpha; tester day 0 was 2026-11-18, Play's 14 days end
  ~2026-12-02, then apply for production. Details:
  docs/plans/release/progress.md.
- **Backend:** netcup VPS, prod at api.rukavina.app plus a test stack
  on the same box (docs/infrastructure.md). develop deploys test, a
  `v*` tag deploys prod after approval.
- **Android:** Soft Dark redesign built in Compose (Plan 7), qa/prod
  flavors, guest mode, export, Play in-app updates and rating prompt,
  Crashlytics. Exercise images parked for v2.
- **CI/CD:** Android/Backend build, Sync E2E, Lint, Sonar, CodeQL;
  fastlane uploads (ADR 0017). OWASP manual. Dependabot version PRs off.
- **iOS:** not started; after Android v2.

## Next (owner, 2026-11-25)

1. In-app feedback (text box + Send), v1, ships in 0.2.4.
2. Owner's full app check → refinement batches (10.1a).
3. Store screenshots (10.4a), licence audit (9.1), library refresh
   (10.1b), production release (10.5).

## Known gaps

- Backend Release fails when backend/ is unchanged (image lookup
  only checks 20 same-tree commits).

## Schema history

Phase 1 schema hardening (groups A–J) is done; evidence in
STATUS_2026-09-26.md §4a. Group K favourites and rest timer are v2.

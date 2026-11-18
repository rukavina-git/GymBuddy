# Plan 10: release

## Why
Play needs 12 testers opted in for 14 days before production
(personal account; re-check the rule before enrolling). Owner order
(2026-11-17): get the whole tester path ready and through Google's
review first, then refine, then ship a fresh version, and only then
enroll ~15 testers.

## Read first
`docs/roadmap-details.md` (Plan 10), `android/fastlane/Fastfile`,
`.github/workflows/android-release.yml`, ADR 0017 (branch rule),
`docs/plans/legal-privacy/data-safety.md`.

## Order
1. **Ready for testers + Google review.**
   - 01 Release dry run: develop → main, tag v0.2.0, prod AAB on the
     internal track, owner installs from Play, prod smoke check
     (10.1 + 10.2).
   - 02 Closed track ready: closed testing track, tester list (empty
     or owner only), opt-in link, feedback route, tester
     instructions. Promote to closed and send it for Google review.
2. **Refinement:** batches from the owner's list (10.1a), one step
   per batch, plus the 16:9 set card clip and real store screenshots
   (10.4a).
3. **Fix issues** found while refining and testing.
4. **New version:** bump, tag, internal → promote to closed.
5. **Enroll ~15 testers** (owner picks after refinement: friends +
   tester group, checklist in memory), 14 days, then tester fixes
   (10.4) and production (10.5).

Order change (2026-11-17, owner): Google approved the closed track
the same day, so enrollment (phase 5) runs now, in parallel with
refinement. The 14 days run while phases 2-4 ship as 0.2.x updates
to the closed track; testers get them automatically.

Step files are written one at a time, each after the previous step
lands.

## Invariants
- Branch rule (ADR 0017): develop → test (App Distribution, qa app),
  v* tags on main → prod (Play). Never push main; main only via PR.
- Testers are enrolled while refining (order change above). Every
  update to the closed track must be at least as stable as the last:
  no half-done batches on alpha.
- Clean before release: Sonar 0 issues, Dependabot 0 open, before
  any v* tag or main PR.
- No new data collection, no new permissions without asking.
- UI changes follow a linked Figma frame; device feel wins.

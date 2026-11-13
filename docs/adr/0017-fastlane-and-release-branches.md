# 17. fastlane uploads; develop is test, main is prod

Date: 2026-11-13
Status: Accepted

## Context

Plan 8.2 builds a signed prod AAB on a `v*` tag but leaves it as a CI
artifact. Testers need test builds, Play needs uploads, and both should
come from CI, not the owner's laptop. The owner wants one rule to know
what runs where: one branch is always test, one is always prod.

## Decision

- **Branches.** develop is the test stage: test backend, qa app
  (`.qa` id) to testers. main is prod: it changes only through a PR
  from develop, and each merge is tagged `vX.Y.Z` right away. The tag
  releases (Play, and the prod backend in 8.4). The tag gives the
  versionCode and makes a prod release a deliberate act.
- **Enforced in CI.** GitHub environment `release` (v* tags only)
  holds the Play upload key and the Play service account. Environments
  can't tie a tag to a branch, so the release workflow fails if the tag
  isn't on main. Environment `testers` (develop only) holds a separate
  qa signing key and the Firebase App Distribution account. develop
  can't read anything that reaches Play.
- **fastlane** (MIT) with `fastlane-plugin-firebase_app_distribution`
  (MIT), run through `ruby/setup-ruby` (MIT). Lanes only upload what
  Gradle built: `test` (qa APK → App Distribution), `internal` (prod
  AAB → Play internal), `promote` (internal → closed `alpha`).
- **Store listing** in `android/fastlane/metadata` (supply layout), so
  listing text is versioned. Placeholder until Plan 9/10.
- Test builds are started by hand, not on every push.

## Consequences

- One manual step per prod release (the tag); a merge to main without
  a tag leaves main ahead of prod until tagged.
- qa builds have their own key: they never share a signature with the
  prod app, and losing it costs only a reinstall for testers.
- Ruby joins the toolchain (CI only; local fallback needs Ruby 3.x).
- All licences are commercial-friendly; nothing blocks paid features.

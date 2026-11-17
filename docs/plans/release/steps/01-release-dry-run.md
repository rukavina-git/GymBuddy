# 01 Release dry run

No Figma frame: no UI change.

## Problem
The release pipeline (v* tag on main → signed prod AAB → fastlane →
Play internal track) has never run end to end. Play holds a hand-made
0.1.0 AAB from before the legal links. The privacy policy changes
(2026-11-17) and delete-account.sh on prod also wait for a main
release. Before testers, the owner must have the prod build from
Play on the phone and see that it works against the prod backend.

## Change
1. (Claude) Pre-flight: Sonar 0 issues, Dependabot 0 open, develop CI
   green. Bump `appVersion` to 0.2.0 in `android/app/build.gradle.kts`.
   Release note for the internal track (fastlane metadata changelog
   for the new versionCode, if the lane uploads it).
2. (Claude) PR develop → main. Merge when green (owner OK).
3. (owner) Approve the prod `legal-site.yml` publish (privacy update
   live) and the backend release (delete-account.sh on prod).
4. (Claude) Tag `v0.2.0` on the main merge commit, push the tag,
   watch `android-release.yml` until the AAB is on internal.
5. (owner, Claude guides) Add yourself as internal tester if needed,
   install from Play, smoke check on prod: R8 build starts, Google
   sign-in, email sign-in, guest, workout logs and syncs, logout,
   delete a throwaway account (rows gone, app signed out).
6. Fix only blockers found in 5 (new step if bigger).

No openapi change. No Room schema change.

## Done when
- v0.2.0 is on the internal track, installed from Play on the
  owner's phone.
- Smoke check in 5 passes; results in progress.md.
- gymtracker.rukavina.app/privacy shows "Last updated: 2026-11-17".

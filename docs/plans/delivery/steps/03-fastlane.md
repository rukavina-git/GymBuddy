# 03 fastlane: testers, Play internal, store listing

## Problem
02 builds a signed prod AAB on a `v*` tag but leaves it as an artifact.
Testers get nothing automatically; Play uploads would be by hand.
Store listing text lives nowhere in the repo.

## Rule (owner, 2026-11-13)
develop is always test, main is always prod. Nothing on develop can
read `release` secrets (upload key, Play account) or upload to Play.
Prod releases are `v*` tags on main, made right after each merge;
the release workflow fails on a tag that isn't on main.

## Change (Claude)
- `android/Gemfile` + `Gemfile.lock` (fastlane, MIT;
  `fastlane-plugin-firebase_app_distribution`, MIT), `fastlane/Appfile`
  (package `com.rukavina.gymbuddy`), `fastlane/Pluginfile`,
  `fastlane/Fastfile` with 3 lanes:
  - `test`: upload an existing qaRelease APK to Firebase App
    Distribution (qa Firebase project), group `testers`, release
    notes = short commit log.
  - `internal`: upload an existing prod AAB (path from 02) to the Play
    `internal` track as a completed release; metadata/screenshots
    skipped here.
  - `promote`: internal → `alpha` (closed), no rebuild.
  Lanes only upload; the build stays in Gradle.
- qaRelease is signed with a separate qa key, never the Play upload
  key. CI writes it into `keystore.properties` like 02 does, so no
  Gradle change.
- `android/fastlane/metadata/android/en-US/`: `title.txt`,
  `short_description.txt`, `full_description.txt`, `changelogs/`
  (supply layout). Placeholder text from the onboarding copy; real
  listing and screenshots come in Plan 9/10.
- `.github/workflows/android-release.yml`: after the AAB, run
  `bundle exec fastlane internal` (needs `PLAY_SERVICE_ACCOUNT_JSON`
  in `release`). Missing secret → step skipped with a notice, so
  tags still build before 8.5.
- New `.github/workflows/android-testers.yml`: `workflow_dispatch`,
  environment `testers` (branch `develop` only; holds only the qa key
  and the App Distribution account). Builds `assembleQaRelease`, runs
  `fastlane test`. Manual so testers aren't paged on every push.
- `promote`: `workflow_dispatch` job in environment `release`
  (v* tags only), so it can't run from develop.
- ADR 0017: fastlane + App Distribution plugin, licences, lanes upload
  only, the develop/prod split. `android/README.md`: Release section
  gets the lanes, local fallback (`bundle exec fastlane …`), secrets
  table. `.gitignore`: `fastlane/report.xml`, generated
  `fastlane/README.md`, service-account files.

## Checklist (owner, can wait for 8.5)
1. qa Firebase project: enable App Distribution, group `testers`,
   service account with "Firebase App Distribution Admin".
2. qa signing key (keytool command in README). Both in Bitwarden.
3. GitHub environment `testers`, deployment branch `develop` only:
   `ANDROID_QA_KEYSTORE_B64`, `ANDROID_QA_STORE_PASSWORD`,
   `ANDROID_QA_KEY_ALIAS`, `ANDROID_QA_KEY_PASSWORD`,
   `FIREBASE_APP_DISTRIBUTION_SA_JSON` (`GOOGLE_SERVICES_JSON_QA`
   stays repo-level).
4. 8.5: Play service account JSON into `PLAY_SERVICE_ACCOUNT_JSON`
   (env `release`).
5. Run "Android testers" from develop; build arrives on your phone.

## Out of scope
Play Console setup (8.5). Store screenshots, final listing copy (9/10).
iOS. Backend CD (04). Auto-promote to production. versionCode for test
builds (stays 5 unless tagged; reinstall with the same code works).

## Done when
`bundle exec fastlane lanes` lists `test`, `internal`, `promote`
(owner runs it if the guard hook blocks fastlane). `actionlint`
clean. `assembleQaDebug`, `testQaDebugUnitTest` still pass. Release
workflow skips the Play upload with a notice when the secret is
missing. README + ADR written. Progress and roadmap ticked.

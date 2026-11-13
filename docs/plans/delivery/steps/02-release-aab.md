# 02 Signed release AAB on a `v*` tag

## Problem
Release builds only work on the owner's laptop (`keystore.properties`,
gitignored). versionCode/versionName are hand-edited in
`app/build.gradle.kts` (5, `0.0.2-SNAPSHOT`). Crashlytics mapping
upload is off everywhere, so prod stack traces stay obfuscated.
fastlane (03) needs a signed prod AAB from CI to upload.

## Change (Claude)
- `android/app/build.gradle.kts`: optional `-Pgymbuddy.releaseTag=vX.Y.Z`.
  When set: versionName `X.Y.Z`, versionCode
  `X*1_000_000 + Y*1_000 + Z` (Y, Z < 1000); anything else fails the
  build. Absent: today's 5 / `0.0.2-SNAPSHOT` stay. Small task
  `printReleaseVersion` to check the mapping without signing.
- New `.github/workflows/android-release.yml`: on push of tags `v*`
  (and `workflow_dispatch` off, tag only). One job in environment
  `release`: JDK 17, Gradle cache, prod `google-services.json`,
  keystore decoded from base64 into `$RUNNER_TEMP`, `keystore.properties`
  written there too, then
  `./gradlew bundleProdRelease -Pgymbuddy.releaseTag=$GITHUB_REF_NAME
  -Pgymbuddy.uploadMapping=true`. Uploads AAB + `mapping.txt` as an
  artifact (30 days). Keystore files removed in an `always()` step.
- If the mapping upload needs more than `google-services.json`, add
  the credential as an env secret and say so (Deviations).
- `android/README.md`: Release section, secrets table
  (`ANDROID_UPLOAD_KEYSTORE_B64`, `ANDROID_UPLOAD_STORE_PASSWORD`,
  `ANDROID_UPLOAD_KEY_ALIAS`, `ANDROID_UPLOAD_KEY_PASSWORD`, all in
  environment `release`; `GOOGLE_SERVICES_JSON` reused), how to tag.

## Checklist (owner)
1. Make the upload keystore (keytool command in README), save it and
   its passwords in Bitwarden. It is the Play upload key for 8.5 too.
2. GitHub environment `release`: deployment tags `v*` only; add the
   4 secrets above.
3. Push tag `v0.1.0` on main, after 8.6 (main = prod since 03). Run is green, artifact has the AAB,
   Crashlytics shows the mapping for 0.1.0 (1000000).

## Out of scope
Uploading anywhere (03). GitHub Releases page. iOS. Lint on release.
Backend deploy on tags (04). Changing proguard rules unless the
release build fails.

## Done when
`./gradlew :app:printReleaseVersion -Pgymbuddy.releaseTag=v1.2.3`
prints `1.2.3 1002003`; `v1.2` fails. `assembleQaDebug` and
`testQaDebugUnitTest` still pass. `actionlint` clean. README updated.
Progress and roadmap ticked.

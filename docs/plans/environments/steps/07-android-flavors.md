# 07 Android qa / prod flavors

## Problem
Every Android build (debug and release) talks to prod
(`https://api.rukavina.app/`) and uses the one Firebase project, so
device testing writes real data. Step 06 added the test backend at
`https://api-test.rukavina.app`. The app needs a build that points at
it, with its own Firebase config, installable next to the prod app.

## Change
- **Flavors** in `android/app/build.gradle.kts`: dimension `env`,
  flavors `qa` and `prod` (AGP rejects names starting with "test").
  - `qa`: `applicationIdSuffix = ".qa"`, `API_BASE_URL`
    `https://api-test.rukavina.app/`, app name "GymBuddy Test".
  - `prod`: no suffix, `API_BASE_URL` `https://api.rukavina.app/`,
    app name unchanged ("Gym Buddy").
  - `API_BASE_URL` moves from the build types to the flavors. The
    `-Pgymbuddy.apiBaseUrl` override stays and wins for both.
  - App name per flavor via `src/qa/res/values/strings.xml`
    (`app_name` only); main keeps the prod name.
  - Build types unchanged (Crashlytics off in debug, R8 in release,
    schemas in debug assets).
- **Firebase config per flavor**: `app/src/qa/google-services.json`
  and `app/src/prod/google-services.json` (the current file moves).
  Both gitignored; `app/src/google-services.json` no longer used.
- **jacocoTestReport**: depends on `testQaDebugUnitTest`; class dirs
  and exec path for `qaDebug`.
- **CI** (`android-build.yml`, `codeql.yml`, `sonar-full.yml`,
  `sync-e2e.yml`, `owasp.yml`, `_gradle-test.yml` callers): write
  both json files from secrets `GOOGLE_SERVICES_JSON` (prod, existing)
  and `GOOGLE_SERVICES_JSON_QA` (new, owner adds in 08). Unit tests
  run once on `qaDebug` (code is the same per flavor); assemble both
  `assembleQaDebug assembleProdDebug`. CodeQL compiles `qaDebug`.
  Until 08 adds the secret, Android CI fails at "write json": say so
  in progress.
- **Docs**: `CLAUDE.md` and `android/CLAUDE.md` / `android/README.md`
  commands (`testQaDebugUnitTest`, `assembleQaDebug`, json paths),
  `.claude/settings.json` allow-list entries for the new task names.
  Old plan step files stay as written.

## Tests
- Local with a fake qa json (prod json, package name rewritten to
  `com.rukavina.gymbuddy.qa`): `./gradlew assembleQaDebug
  assembleProdDebug testQaDebugUnitTest jacocoTestReport` green.
- `aapt2 dump badging` (or merged manifest): qa APK package
  `com.rukavina.gymbuddy.qa`, label "GymBuddy Test"; prod APK
  unchanged package and label.
- Generated `BuildConfig.API_BASE_URL` per variant is the right URL;
  `-Pgymbuddy.apiBaseUrl=http://10.0.2.2:8080/` overrides it.
- `actionlint` on the changed workflows.
- Device (adb): install qaDebug next to the existing prod app, both
  open. Sign-in is not checked (needs the real test Firebase, 08).

## Out of scope
The test Firebase project, its json, the GitHub secret, DNS (08).
Launcher icon badge for qa. Release signing per flavor (Plan 8).
Room schema, sync, domain, `api/openapi.yaml`: no changes.

## Done when
Tests above pass, docs updated, progress and roadmap ticked.

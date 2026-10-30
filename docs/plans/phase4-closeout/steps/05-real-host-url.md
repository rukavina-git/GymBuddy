# 05 Release build and spec point at api.rukavina.app

## Problem
The release `API_BASE_URL` (android/app/build.gradle.kts) and the
spec's production server (api/openapi.yaml `servers`) are
`https://api.gymbuddy.app`, a domain nobody owns. Swagger UI's
"Try it out" fails against it (architecture plan §8.3).

## Change
- **Spec change:** `servers[0].url` → `https://api.rukavina.app`.
- Release `API_BASE_URL` default → `https://api.rukavina.app/`. Keep
  the existing property override (`apiBaseUrl(...)`) so a local or
  staging URL can still be passed in.
- Debug stays `http://10.0.2.2:8080/`.
- api/README.md and docs that mention `api.gymbuddy.app` updated.

## Tests
- `npm run lint` in api/.
- Backend `OpenApiSpecDriftTest` still passes (servers block shouldn't
  matter; confirm).
- `./gradlew assembleRelease` builds (or `assembleDebug` if release
  signing isn't available in the session) and `BuildConfig.API_BASE_URL`
  for release is the new URL.

## Out of scope
Release signing, versionCode, Play Console.

## Done when
Lint and builds above are green, `grep -r gymbuddy.app` finds nothing
outside history docs.

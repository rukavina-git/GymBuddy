# 13. Repo-wide dependency scanning and static analysis

Date: 2026-09-26 (recorded retroactively; the work landed 2026-08-17
to 2026-09-07)
Status: Accepted

## Context

Phase 3's Group J scoped CI as backend-only: build, test against real
Postgres, and a spec-drift check. While that was built, OWASP dependency
scanning and SonarCloud analysis were added too, for both `backend/` and
`android/` (commits `29f31d6`, `425da95`, `f567ce1`, `d2e9734`,
`22cc099`, `966aff8`, `92794ef`). Neither the original plan nor any
Group covered that work, so this record documents it after the fact.

Both modules ship third-party dependency graphs of a few hundred
artifacts (roughly 510 on Android's debug runtime classpath and 296 on
the backend's runtime classpath). Neither had any vulnerability scanning
or tracked code-quality baseline.

## Decision

Treat dependency scanning and static analysis as repo-wide
infrastructure, not part of any one module's phase. Build it from
shared reusable workflows, with one caller per module.

- **OWASP dependency-check** runs through the
  `org.owasp:dependency-check-gradle` plugin inside each module's own
  build (`_owasp.yml`, called by `owasp.yml`). An earlier version used
  the standalone CLI action. It scanned the source tree for jars on
  disk and reported one dependency per module, so it was dropped. The
  scan is manual (`workflow_dispatch`), with one job per module. It
  needs an `NVD_API_KEY` secret: the plugin fails without one and does
  not fall back to anonymous access. Results go to the Security tab as
  SARIF.
- **SonarCloud** has one project per module
  (`rukavina-git_GymBuddy-backend`, `rukavina-git_GymBuddy-android`).
  It runs on every build via `_sonar.yml` and reuses the JaCoCo report
  that `_gradle-test.yml` already produced. `sonar-full.yml` is a
  manual whole-codebase scan. Sonar reports only and never waits on a
  quality gate.
- **Coverage exclusions** live only in
  `.github/actions/coverage-exclusions`, so the per-module and
  whole-codebase scans cannot drift apart. Exclusions are for code that
  is untested by design. They must not hide real gaps.
- **Workflow secrets** are passed to steps through the environment, and
  every workflow narrows its token `permissions`.

## Consequences

- Android gets vulnerability scanning and a quality baseline in Phase 1,
  well before its own CI phase, because the reusable workflows made it
  cheap to add a second caller. An iOS module would follow the same
  pattern.
- The OWASP scan is manual, so a new CVE shows up only when someone runs
  it. Scheduling it is a one-line change once NVD sync times on a warm
  cache are known.
- Sonar never fails a build. That is deliberate while coverage is still
  being built up, and should be revisited once the gate is meaningful.
- Two more secrets need managing: `NVD_API_KEY` and `SONAR_TOKEN`.
- The Android JaCoCo and Sonar wiring is CI configuration and changes
  nothing in Android's Phase 1 schema work.

## Update — 2026-09-26

The "OWASP scan is manual" description above (Decision and
Consequences) is superseded. `owasp.yml` now also runs automatically,
path-filtered to the files that can change the dependency graph
(`build.gradle.kts`, `libs.versions.toml`, `gradle-wrapper.properties`,
plus its own workflow files), on push to `main` or `develop` and on pull
requests into `main`. Manual dispatch still works as a fallback. A
newly published CVE against unchanged dependencies still needs a manual
run to surface.

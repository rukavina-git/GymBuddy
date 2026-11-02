# 01 OWASP triage + triggers back on

## Problem
`owasp.yml` is manual-only since 2026-10-30. Its last runs failed and
took 14 min to 4.5 h, mostly the NVD database sync. Findings were
never triaged. Dependabot is clean (35/35 fixed), but OWASP also sees
transitive Android/Gradle artifacts Dependabot doesn't.

## Change
- Cache `dependency-check-data` (NVD data dir) in `_owasp.yml` with
  `actions/cache`, keyed by week, so a run is a delta sync.
- Run the scan for backend and Android (CI via workflow_dispatch, or
  locally with an NVD key). For each finding: bump the dependency,
  replace it, or suppress it in a `dependency-check-suppressions.xml`
  per module with a comment saying why (not reachable, false CPE
  match, no fix yet + link).
- Restore triggers: push to develop/main and PRs to main, path-filtered
  like the build workflows, plus a weekly schedule. Still non-blocking
  (ADR 0013).
- Note the triage outcome in docs/status.md (one line).

## Tests
- A workflow_dispatch run on develop finishes green, or with only
  suppressed findings, in under 20 min on a warm cache.

## Out of scope
Making OWASP blocking. SonarQube (8.1). Version bumps that need code
changes beyond a few lines: list them as follow-ups instead.

## Done when
OWASP workflow green on develop for both modules, triggers restored,
every suppression has a reason, `./gradlew build` (backend) and
`./gradlew testDebugUnitTest` (android) still green.

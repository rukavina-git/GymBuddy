# 02a Clean Sonar issues and Dependabot alerts

## Problem
Owner rule: nothing gets a `v*` tag or a main PR with open Sonar issues
or Dependabot alerts. State on 2026-11-13:
- Dependabot: 1 open alert, #39 medium, `minimatch` 10.2.6 (DoS via
  globparts, vulnerable `>=7 <11.0.1`) in `api/package-lock.json`,
  transitive via `@openapitools/openapi-generator-cli` 2.42.0 →
  `glob` 13.0.6. The `minimatch` 3.1.5 under Spectral is outside the
  range. Dev tool only, not shipped.
- Sonar: no issue list exists. The free plan shows only SonarCloud's
  main branch (`main`), which was never scanned; the API refuses
  develop ("not allowed to access data from non main branches").

## Decision (owner, 2026-11-13)
`develop` becomes SonarCloud's main branch: findings before release,
not after. The gate on develop warns, never blocks; the block is the
release rule (gate green + alerts closed before `v*` tag / main PR).
Recorded in ADR 0013 (update 8.2a) and `docs/infrastructure.md` §8.

## Change (Claude)
Part A (CI + Dependabot, first commit):
- `api/package.json` + lock: generator-cli 2.42.2 (`glob` 14 →
  `minimatch` 11.0.8, BlueOak-1.0.0, permissive).
- `_sonar.yml`: `wait-for-gate` scan step gets `continue-on-error`; a
  red gate posts a warning annotation + step summary with the link.
- `backend-build.yml`, `android-build.yml`: `wait-for-gate` on pushes
  to `develop` instead of `main`. PRs/`main` keep scanning
  report-only (the ruleset requires `Sonar / Sonar (…)`).
- ADR 0013 update, `docs/infrastructure.md` "Code quality gate".

Part B (after owner checklist and one develop scan):
- Pull the issue list + hotspots via API. Fix every BUG/VULNERABILITY
  and CRITICAL/BLOCKER smell; MAJOR and below when cheap, else owner
  marks "accepted" with a reason. Behaviour-neutral; no change to
  `api/openapi.yaml`, Room schema, sync, domain logic.

## Checklist (owner)
1. In each SonarCloud project (backend, android): Branches → delete
   the short branch `develop` (a name clash would block the rename),
   then rename main branch `main` → `develop`.
2. Re-run the latest Backend Build and Android Build on develop (or
   push). The Sonar job reads the gate without an error.
3. Dependabot alert #39 closes only when the fix reaches `main` (8.6):
   Dependabot reads the default branch. Until then check `npm ls`.

## Out of scope
Dependabot version-update PRs. Gate/rule changes. Refactors beyond
what an issue needs. A CI check for the release rule.

## Done when
A: `npm ls minimatch` has nothing in `>=7 <11.0.1`; `npm run lint`,
`generate:kotlin` pass; `actionlint` clean (#39 closes at 8.6).
B: develop gate green, no open BUG/VULNERABILITY/CRITICAL;
backend `./gradlew test`, android `testQaDebugUnitTest` +
`lintQaDebug` pass. Progress and roadmap ticked.

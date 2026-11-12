# 01 CI gates: Android Lint, SonarQube (Q-E)

## Problem
Android Lint exists but nothing runs it, so new lint errors land
unseen. Sonar reports on every build but never fails one (ADR 0013,
Q-E "revisit in 8.1"). Before signed releases (02) and auto-deploy
(04), both should be able to stop a bad merge.

## Change (Claude)
- `android/app/build.gradle.kts`: `lint { }` block with
  `baseline = file("lint-baseline.xml")`, `abortOnError = true`,
  `checkReleaseBuilds = false` (release gets its own job in 02).
  Generate `android/app/lint-baseline.xml` with
  `./gradlew updateLintBaseline` and commit it. Only new issues fail.
- `android-build.yml`: new job `lint` (needs nothing, parallel to
  `test`): JDK 17, Gradle cache, qa `google-services.json`,
  `./gradlew lintQaDebug`, upload the HTML/SARIF report as an
  artifact (`if: always()`).
- `_sonar.yml`: new input `wait-for-gate` (bool, default false). When
  true, add `-Dsonar.qualitygate.wait=true` so a red gate fails the
  job. Both callers (`android-build.yml`, `backend-build.yml`) pass
  `true`. `sonar-full.yml` stays report-only.
- ADR 0013: "Update 2026-11-12": Sonar gate blocking, Android Lint
  in CI with a baseline. Q-E answered in `roadmap-details.md`.

## Checklist (owner)
1. SonarCloud, both projects: quality gate "Sonar way" (80 % new-code
   coverage, no new bugs/vulns), new-code definition = previous
   version or reference branch `main`. Say which.
2. After the push: the Sonar jobs on develop are green. If a gate is
   red on code not touched by this step, tell Claude before going on.
3. Branch ruleset on `main`: add the new `Lint` check as required.

## Out of scope
OWASP (10.0). ktlint/detekt. Lint on release builds (02). Fixing the
baselined warnings. Changing coverage exclusions.

## Done when
`./gradlew lintQaDebug` passes locally with the baseline, and fails
on a deliberately added lint error (then reverted). `actionlint`
clean on the changed workflows. ADR 0013 updated, Q-E answered.
Progress and roadmap ticked.

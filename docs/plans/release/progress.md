# Plan 10 progress

- Started 2026-11-17. 10.0 OWASP done earlier (roadmap). Owner order:
  tester path ready first, then refinement, then a fresh build before
  inviting anyone.
- [ ] 01 release dry run (v0.2.0 on the internal track)
  - 01.1 done (2026-11-17): pre-flight Sonar 0/0, Dependabot 0, develop
    CI green (Dependency graph failed once on a Gradle plugin portal
    fetch of foojay 1.0.0, rerun). appVersion 0.2.0 = versionCode 2000,
    release note changelogs/2000.txt (internal lane uploads changelogs;
    promote lane skips them). Build, unit tests, lint green.
  - Deviations: none so far.
  - Gotchas: no v0.1.0 tag exists (0.1.0 AAB was hand-made); v0.2.0 is
    the first tag. Next: PR develop → main, owner approvals, tag.
  - 01.2-01.4 done (2026-11-17): PR #16 merged (8ecd5d7). Privacy page
    live ("Last updated: 2026-11-17"). Tag v0.2.0 pushed (admin bypass
    of the tag-creation rule). Backend Release deployed prod (owner
    approved). Android Release uploaded AAB 2000 + changelog to
    internal (fastlane success).
  - Gotcha: a stale Legal pages run on main (02:31, PR #11) sat
    "waiting" for prod approval and held the `legal-site` concurrency
    group, so new runs stayed pending with no jobs. Owner cancelled
    it. Backend prod deploy runs from the v* tag (backend-release.yml),
    not from the main merge.
  - Open: 01.5 owner smoke check from Play.

# Plan 10 progress

- Started 2026-11-17. 10.0 OWASP done earlier (roadmap). Owner order:
  tester path ready first, then refinement, then a fresh build before
  inviting anyone.
- [x] 01 release dry run (v0.2.0 on the internal track) 4a024a0, 54c4607
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
  - Deviation: 01.5 owner smoke check from Play deferred to the
    refinement pass (10.1a), owner wants to move straight to the closed
    track. 10.1 stays open until then.
- [x] 02 closed track ready (v0.2.0 on alpha, sent for review) be12539
  - Rule checked 2026-11-17 (Play Console Help 14151465): personal
    accounts created after 2023-11-13 need >= 12 testers opted in
    continuously for >= 14 days in a closed test; leave + rejoin
    restarts the count; internal testing doesn't count; Google may
    ask for more testing if engagement is low. Production access
    review ~7 days.
  - Track "Closed testing - Alpha": 177 countries (rest of world),
    email list "me" (owner only), feedback ivankarlo.rukavina@gmail.com.
    App content completed in the same submission (data safety,
    content rating, target audience 16+, ads, health apps, sign-in
    details, category Health & Fitness, store listing en-US).
  - Opt-in link: https://play.google.com/apps/testing/com.rukavina.gymbuddy
    Store link: https://play.google.com/store/apps/details?id=com.rukavina.gymbuddy
  - Review state: approved 2026-11-17, closed release 2000 live on
    alpha (full rollout).
  - Tester guide: docs/release/tester-guide.md.
  - Deviations: tester list named "me", not "gym-tracker-testers".
    Release created by hand in Console (Add from library, 2000), not
    by the promote job (see gotcha).
  - Gotchas: promote job (run 44097008238) failed: "Only releases
    with status draft may be created on draft app." The app stays a
    draft app until its first review passes; after that the promote
    lane should work unchanged. If it fails again, set
    track_promote_release_status "draft" or promote in Console.
    Reviewer sign-in details must work on prod.
- Enrollment started early (2026-11-17, owner): order change in
  00-overview.md. Track switched from email list "me" to Google Groups:
  owner group (rukavina@…, owner + friends) and
  testers-community@googlegroups.com (paid tester service, checklist
  in memory). Countries: all. These track changes are in Google review
  now; then owner publishes, then submits the service's form (join
  link, no test account: testers make their own, icon
  ic_launcher-playstore.png). Day 0 of the 14 = the day opted-in
  reaches 12; log it here.
  - Gotchas: the opt-in "download it on Google Play" link errored
    right after opting in, worked minutes later (same URL as the
    "public version" link). Testers: if it errors, wait and retry.
    Email lists and Google Groups are exclusive on a track (radio
    buttons); several groups are allowed.
  - Only the owner's account uses prod so far.
  - 2026-11-17: Google approved the group/country changes, owner
    published and submitted the tester service form. Waiting for
    opt-ins; day 0 = first day with 12+.
- [x] 03 prod release-build pass (10.1), 2026-11-17, USB phone
  (SM-S731B), Play 0.2.0 / 2000, prod backend. All passed: sign out,
  guest onboarding + workout, email sign-up from the workout summary
  (consent screen, guest workout moved into the account, "Backed up"),
  sign out + email sign-in (workout back from server), delete account
  (type delete + password; Welcome after; sign-in then fails with
  "Email or password is wrong"), Google sign-in back to the owner's
  test account (jabuka390, Full Body C Nov 12 back). Logcat: no FATAL,
  no R8 / missing-class errors.
  - Finding (10.1a): after "Create account" on the workout summary,
    the summary still shows the "Keep this safe? Create account" card.
    Hide it once signed in.
  - Gotchas: `adb shell input text` drops the first text sent right
    after focusing a field; send it again. Google Password Manager on
    the owner's phone offers the playreview@ account on every password
    field and offers to overwrite it on save: always Cancel / Back.
- Play review warnings on 0.2.0 (2026-11-17, owner recalls, not
  confirmed by text): deprecated edge-to-edge APIs + likely the
  large-screen orientation/resizability one. Our code uses
  enableEdgeToEdge and locks no orientation, so both come from
  libraries; not blocking. If a warning names a com.rukavina class,
  fix it in 0.2.x.
- [x] 04 refinement batch 1 (2026-11-17): summary backup card hides
  after sign-in, Up next row dropped below 720dp (16:9 clip), Clock
  provider local zone (Home showed starts 1h early). Tests, lint,
  device checked (wm size 1080x1920, then reset). Ships in 0.2.1.
  - Gotcha: lint flags Configuration.screenHeightDp; use
    LocalWindowInfo.containerSize.
- Day 0 corrected (2026-11-18): the tester service's dashboard showed
  0/15 testers, testing starting 2026-11-18 (~2.5 h after check). The
  "12+" seen in Console on 11-17 was not real opt-ins. Service plan:
  15 testers, 16 days (ends ~2026-12-04). Play's 14 days end
  ~2026-12-02: apply once Console's checklist shows it met. Track it
  on the service dashboard; Statistics "Data unavailable" for a new app.
- Update cadence (2026-11-18, tester service advice + owner): 3-5
  updates during the 16 days, about one every 3-4 days: 0.2.1 ~11-19,
  0.2.2 ~11-23, 0.2.3 ~11-26, 0.2.4 ~11-29, 0.2.5 optional ~12-01.
  Each one stable (tests, lint, Sonar/Dependabot clean, device check).
  Owner break 11-18..~11-21, then a fresh full pass feeds 0.2.2+.
  Production form: service's "Production Access Report" pre-fills
  answers; check every claim against real data before pasting.
- 0.2.1 (2001) prep 2026-11-18: pre-flight Sonar 0/0, Dependabot 0,
  develop CI green (604c096). Bump + changelogs/2001.txt (step 04
  fixes). Next: PR develop → main, owner merges, tag v0.2.1, internal
  → promote to alpha.
- 0.2.1 crash fix 2026-11-19: Crashlytics, 0.1.0/0.2.0, 42 crashes, 6
  users. Offline ID token refresh threw ExecutionException out of the
  OkHttp interceptor, which OkHttp rethrows on its thread. Now offline
  → IOException (sync retries), dead session → no token (401 →
  AuthGate). Reproduced and verified on device (airplane mode, clock
  +2h). Added to changelogs/2001.txt.
- 0.2.1 released 2026-11-19: PR #19 → main (b520e14), tag v0.2.1,
  AAB on internal, promoted to closed (alpha). Backend Release failed
  as expected-harmless: backend/ unchanged since v0.2.0, but its image
  lookup only checks 20 same-tree commits and the imaged one (8ecd5d7)
  is #27. Fix the window before the next backend change.
- Step 05 written (2026-11-18): default library + plans refresh from a
  4-reviewer council (05a library 60->64, 05b plans, 05c onboarding
  mapping; all in one update, 0.2.2, owner). Owner OK. Starts after the owner's
  break. Images stay on Gemini (owner), plan exercises first.
  free-exercise-db images rejected: "Unlicense" label, but maintainer
  doesn't know their source and some trace to ExRx.net (issues #2, #13).
- Forced updates for 0.2.2 (2026-11-20, owner): 0.2.0 kept crashing
  after 0.2.1 shipped (testers update slowly, Play can't force it).
  Add Play in-app updates: on start, a release with update priority
  >= 4 blocks the app until updated (IMMEDIATE); lower ones get a
  dismissable prompt or nothing. Priority is chosen per release in the
  release workflow (fastlane in_app_update_priority, set only on
  upload; check it survives the internal → alpha promote). Record the
  library's licence (Play SDK terms) in docs/third-party.md.

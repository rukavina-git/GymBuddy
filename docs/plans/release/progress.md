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
- In-app updates built 2026-11-20 (PR feat/in-app-updates, ships in
  0.2.2): priority per version in android/fastlane/update-priority.yml
  (missing = 0, owner's default). Unit + FakeAppUpdateManager tests.
  Device check pending (phone not connected); real Play flow can only
  be seen once 0.2.3 is out to phones on 0.2.2.
- 0.2.2 released 2026-11-24: PR #25 → main, tag v0.2.2, AAB on
  internal, promoted to closed (alpha). Ships in-app updates (priority
  0), Ward fixes (Play security provider update, overlay hiding on
  password screens; both checked on device) and sync failure-mode
  tests. No exercise images (Gemini set removed; Qwen set next).
  Backend Release cancelled (backend/ unchanged; image lookup window
  still to fix). Dependabot version PRs off (limit 0).
- 0.2.3 released 2026-11-24: PR #26 → main (50bd048), tag v0.2.3, AAB
  on internal, promoted to closed (alpha). Update priority 4: first
  forced update for 0.2.2 phones. Sign-out confirmation, Play rating
  prompt; new store listing text pasted by hand. Backend Release
  waiting on the image check again (backend/ unchanged).
- Step 06 written (2026-11-25): in-app feedback for 0.2.4 (owner:
  v1, runs before 05; ntfy ping without text + read script; email
  option removed). Next after 06: owner's full app check (10.1a).
- [x] 06a feedback API + backend (2026-11-25), a5a9d97
  - **Spec change:** POST /v1/feedback (security optional) +
    FeedbackRequest. V9__feedback.sql. New backend package `feedback`
    (service, rate limiter, ntfy notifier), added to CLAUDE.md.
  - Tests: FeedbackControllerTest (guest, signed in, invalid token,
    retry, 400s, 429 per IP and per uid, window, account delete);
    backend 156/156, Spectral clean.
  - Deviations: nginx limit 10/min per IP + burst 5 (step said ~5/min).
    Body cap 16k. Notifier uses java.net.http, no @Async.
  - Gotchas: guests reach controllers with Spring's anonymous token
    (principal "anonymousUser"); never read the principal as a uid
    without excluding AnonymousAuthenticationToken. nginx config not
    syntax-checked locally (needs certs); check `nginx -t` on deploy.
  - Open (reviewer, not fixed): nginx 429 is HTML without Retry-After
    (spec says JSON + header); createdAt unbounded (Long.MAX -> 500);
    feedback.sh prints raw control chars (terminal escapes); expired
    token stores as guest (06b: refresh token first; 06c wording);
    rate token spent before dedupe; UTF-16 vs code point length.
  - (me) Before 06b on test: NTFY_FEEDBACK_TOPIC in both stacks' .env,
    scp feedback.sh + nginx template, deploy.
- 06a on the server (2026-11-25, owner): topic set in both env files
  (separate test/prod topics, Bitwarden), both compose files,
  feedback.sh and nginx template copied, `nginx -t` in a throwaway
  container passed, nginx restarted. Test: curl POST 204, same id
  again = one row, ping on the phone, feedback.sh shows it.
  - Gotchas: a changed env file needs `docker compose up -d backend`
    (the backend ran without the topic until then; check with
    `docker compose exec backend printenv NTFY_FEEDBACK_TOPIC`).
    Resending the same id is a dedupe: no row, no ping.
- 06d added (2026-11-25, owner): feedback admin page at /admin/ on
  each API host (Firebase Google sign-in, uid allowlist, status open /
  solved / won't fix / spam + note). Spec change. Order: 06b, 06c,
  06d, ship 0.2.4. Backend image lookup window fix still due before
  the next backend release.
- Root SSH login alert turned on (2026-11-25, owner): it had been off
  since Plan 5. setup.sh ran before the alert existed, so the pam line
  and script were missing and no topic was set. Installed by hand
  (same code as setup.sh), own topic in /etc/gymbuddy/ntfy-topic
  (Bitwarden "GymBuddy ntfy SSH login topic"); test login pinged.
  - Open (hardening, after 0.2.4): `deploy` logins don't alert but
    deploy is root-equivalent; alert on deploy unless it's the GitHub
    Actions key (needs separate keys + a way to see the key in PAM).
- Backend Release image lookup fixed (2026-11-25): matches commits by
  backend/ tree hash over the last 500, first pass checks all of them
  (no 20 cap), retries check the newest 10. Simulated on v0.2.3: 79
  same-tree commits, finds sha-8ecd5d7 (the old code gave up).
  actionlint clean. Real proof: the 0.2.4 tag.
- [x] 06b Android feedback (2026-11-25), 1ea75a9 + device fixes
  - You › Help › Send feedback (row above About; mailto row and
    FEEDBACK_EMAIL removed). One field (6 lines), counter from 1800,
    "too long" error past 2000, what-is-sent line, Send; then "Thanks,
    sent" or "Saved, it sends by itself when you're online".
  - Deviation (owner OK): queue in its own Room file `feedback_db`
    (FeedbackDatabase v1, schema exported), not a table in the
    per-account AppDatabase, so a sign-in/out can't strand a message.
    Row keeps the author uid; the token is sent only if that uid is
    still signed in, else it goes as guest. Account deletion drops
    that uid's unsent rows.
  - FeedbackOutbox: insert, try at once (8 s cap), else FeedbackWorker
    (network constraint, exponential backoff). 2xx and other 4xx drop
    the row; no answer, 429, 5xx keep it and stop the run.
  - SyncApis.retrofit() extracted so FeedbackApi shares the client
    setup. AppTextField got `minLines`.
  - Tests: FeedbackOutboxTest, FeedbackWorkerTest,
    RetrofitFeedbackRemoteTest (token rule), FeedbackViewModelTest,
    AccountDeletionCoordinatorTest extended. Build, all unit tests,
    lint green.
  - Gotcha: outbox tests use runBlocking; runTest's virtual clock fires
    the send timeout while Room works on its own executor.
  - Device check (SM-S731B, qa, signed in as crashfix-test-1119):
    online send → "Thanks, sent"; airplane mode → "Saved"
    (UnknownHostException logged), back online → FeedbackWorker
    SUCCESS. Guest send on device not done (would sign out the test
    account); covered by unit tests + the curl check. Owner: confirm
    both rows and pings on test with feedback.sh.
  - Device fixes: keyboard hid Send (tab bar + IME padding counted
    twice). Feedback route is now full screen (isFullScreen, like Edit
    workout), and the screen consumes the navigation-bar inset before
    imePadding.
  - Gotchas: `am force-stop` cancels the app's WorkManager jobs until
    the next launch (a swipe-away doesn't); relaunching ran the worker.
    No sqlite3 on the phone; use WM-WorkerWrapper logs. A blank Home
    seen once in airplane mode didn't reproduce (likely a transition
    caught mid-way).
  - Wording (owner: "more professional"): "Feedback sent / Thank you
    for helping us improve the app", "Feedback saved / It couldn't be
    sent right now and will be sent automatically as soon as
    possible" (not "offline": 429/5xx/slow also queue). 8448fbf.
  - Owner: no guest device test needed (same request without a token).
- [x] 06c legal + Play (2026-11-25); Play Data safety row 9 done by owner 2026-11-26
  - privacy.md: Feedback section (what, why 6(1)(f), ntfy gets no
    content, don't send health details, 2 years or with the account,
    guests ask by email), retention line, date 2026-11-25. ntfy.sh not
    in the providers table: it receives no personal data.
  - Retention 2 years chosen by Claude (step had none; backend kept
    forever): cleanup job added to 06d.
  - records-of-processing.md, data-safety.md (row 9: App activity →
    Other user-generated content, optional; device fields = crash log
    fields), third-party.md (ntfy row, data map) updated.
  - Page goes live with the 0.2.4 merge to main (legal-site.yml).
  - (me) Play Console Data safety: add row 9, submit with 0.2.4.
- 06d split in two (2026-11-25, owner OK).
- [x] 06d1 admin backend + spec (2026-11-25), 070f496
  - **Spec change:** tag Admin, GET /v1/admin/feedback (status,
    cursor, limit; items + nextCursor + counts), PATCH
    /v1/admin/feedback/{id} (status and/or note), schemas
    FeedbackStatus, AdminFeedbackItem/Counts/Page/Update, Error enum
    + NOT_FOUND, NotFound response. createdAt range documented.
  - V10__feedback_status.sql: status (check 4 values, default open),
    note (<= 500), updated_at (backfilled = received_at), index
    (status, received_at DESC, id DESC).
  - auth/AdminAccess: one AuthorizationManager on /v1/admin/** in
    SecurityConfig; env ADMIN_UIDS (comma list, blank = nobody).
    No token / invalid token 401, other uid 403.
  - FeedbackAdminService (keyset cursor base64url "receivedAt|id"),
    AdminFeedbackController, FeedbackRetention (730 days, daily 03:30,
    off under test). ntfy Click = ADMIN_URL. createdAt must be
    2020-01-01 .. now + 1 day. feedback.sh strips C0/C1 controls
    (keeps newline, tab), shows status + note. Both compose files pass
    ADMIN_UIDS, ADMIN_URL.
  - Tests: AdminFeedbackControllerTest (401, 403, invalid token, list
    order/account/counts/cursor/filter, last page null cursor, 400s,
    patch status/note/clear, 404, retention), AdminAccessAndNotifierTest
    (empty list, anonymous, Click header), createdAt bounds. Backend
    168/168 (incl. drift test), Spectral clean, Android
    compileQaDebugKotlin green with the new generated AdminApi. Clean
    regex checked on postgres:16 (ESC, BEL, C1 gone).
  - Deviations: limit is clamped to 1-100 (like sync pull), not 400.
    Empty-list 403 is a unit test, not MockMvc (see gotcha). Added a
    global MethodArgumentTypeMismatchException -> 400 handler (limit=abc
    was 500; also affects sync pull). Empty note clears it.
  - Gotchas: a new per-class `properties =` Spring context made
    TombstoneRetentionConcurrencyTest fail with "too many clients"
    (each context has its own Hikari pool on the shared container).
    Test admin uid lives in src/test/resources/application-test.yml.
    KDoc containing `/v1/admin/**` opens a nested comment (Kotlin):
    don't write `/**` inside a doc comment.
  - Open (reviewer, not fixed): **SHOULD before 0.2.4 Sonar check:**
    AdminDtos `val wont_fix` likely trips Sonar naming; use
    @JsonProperty("wont_fix") val wontFix. NITs: spec says null status
    = unchanged but FeedbackStatus isn't nullable, and `{}` is 400 but
    the schema lacks minProperties: 1; note length counted in UTF-16
    (stricter than char_length); a control char in ADMIN_URL would
    throw in buildRequest() after the insert (500 on POST); type
    mismatch handler is global.
  - Roadmap: 10.4b stays open until 0.2.4 ships (no 06d1 line).
- [x] 06d2 admin page + nginx (2026-11-25), repo part; 8bc8315
  - backend static/admin/ (index.html, admin.js, admin.css; plain ES
    module, Firebase JS SDK 13.3.0 from gstatic, Google popup sign-in).
    AdminPageController (@Hidden, no spec change): /admin -> /admin/,
    /admin/ forwards to index.html, GET /admin/config.js from
    ADMIN_FIREBASE_API_KEY / _AUTH_DOMAIN / _PROJECT_ID (any blank =
    null = "Admin is off"), no-store, `<` and U+2028/9 escaped.
    SecurityConfig: GET /admin and /admin/** permitAll; /v1/admin/**
    unchanged. Both compose files pass the 3 new env values.
  - Page: chips Open/Solved/Won't fix/Spam/All with counts (default
    Open), cards with message (textContent, pre-wrap), date
    Europe/Zagreb (createdAt), app / OS / device, guest or uid, status
    select + note (saves on change, Saved / error), Load more (50 per
    page). 401 -> one forced token refresh + retry; 403 -> "not an admin".
  - nginx: zone admin 30r/m per IP, burst 20, on /admin/ and /v1/admin/
    (prod + test). CSP on /admin/ (HSTS repeated there: a location's
    add_header replaces the server's). `nginx -t` passed locally with
    envsubst + a throwaway cert (nginx:1.27-alpine).
  - third-party.md: Firebase JS SDK row (section 5).
  - Tests: AdminPageTest (redirect, page/js/css without token, config
    null when unset, values, blank, escaping, /v1/admin still 401).
    Backend 176/176 incl. drift test.
  - Deviations: CSP script-src also allows https://apis.google.com
    (Firebase popup loads its gapi loader from there). frame-src is
    https://*.firebaseapp.com (one template for both projects). No
    Figma frame (owner-only internal page, owner OK); colours from
    design/tokens.json, system font (no Figtree download).
  - Open (reviewer, not fixed): SHOULD: a chip tap during a running
    load is dropped but the filter still changes (list and chip
    disagree; Load more mixes filters) -> ignore taps while loading;
    two quick status changes race -> disable the select while saving.
    NITs: card shows createdAt but list sorts by receivedAt; osVersion
    shown raw; config.js load failure says "Admin is off"; burst 20
    may 429 during fast triage; raw U+2028/9 chars in
    AdminPageController source (write as escapes).
  - (me) Deploy, test first then prod: Firebase console per project:
    register a web app (copy apiKey, authDomain, projectId), add the
    API host to Auth > Settings > Authorized domains. .env: the 3
    ADMIN_FIREBASE_* + ADMIN_UIDS (owner uid in that project) +
    ADMIN_URL (https://<host>/admin/). Backend image from develop
    (test) / tag (prod). scp both compose.yaml + nginx template,
    `nginx -t` in a throwaway container, restart nginx, `docker
    compose up -d backend`, check printenv. Bitwarden entry.
  - Check (me, phone): sign in on test, see curl + device messages,
    mark one solved, filter shows it; second Google account gets
    "not an admin"; ntfy ping opens the page. Then prod.
- 06d2 server setup (2026-11-26, owner): 5 ADMIN_* values in both
  stacks' env files (Firebase web app "admin" in each project, owner's
  Google uid per project), compose files + nginx template copied,
  nginx -t passed, nginx recreated, both backends restarted. Test
  /admin/ sign-in works, feedback list shows. Prod page goes live with
  the v0.2.4 tag (prod runs the 0.2.3 image until then).
  - Gotchas: a Google user appears in Firebase Users only after its
    first sign-in (console can't add one). Web apiKey and uids are not
    secrets: no Bitwarden entry.
  - Still to check on test: mark solved + filter, second Google account
    gets "not an admin", ntfy ping opens the page.
  - Owner checked on test (2026-11-26): solved + filter, second account
    "not an admin", ntfy ping opens the page. All work.
- Reviewer SHOULDs fixed (2026-11-26): admin.js chip taps are ignored
  while a load runs and the filter only switches once its page arrives;
  a card's status + note are disabled while a save runs. AdminDtos
  `wontFix` with @get:JsonProperty("wont_fix") (JSON unchanged, no spec
  change). Backend 176/176.
- Next: ship 0.2.4 (bump, changelog, closed-test log, PR, tag); check
  /admin/ on prod after.
- [x] 0.2.4 ship prep (2026-11-26): pre-flight Dependabot 0, Sonar
  android 0, backend 22 (clean-before-release rule). Fixed 21: useless
  `!!` / `?:` after transactionTemplate.execute / queryForObject
  (Spring 7 nullness), `.toList()` on jdbcTemplate.query results,
  `rows.take(limit)` for subList pages, admin.js `void load(...)`,
  index.html `<output>` for the status line. appVersion 0.2.4 (2004),
  update priority 0 (owner), changelogs/2004.txt, closed-test-updates
  entry. Backend build green, android build + unit tests + lint green.
  - Reviewer: no blockers. NIT: ReferenceVersionRepository lost the
    "V2 should have seeded it" hint (queryForObject throws anyway).
  - (me) SonarCloud: mark V10__feedback_status.sql UPDATE-without-WHERE
    (plsql BLOCKER) as Accepted; the backfill is meant to hit every row
    and V10 can't change (Flyway checksum on the servers).
  - Deviations: Sonar cleanup not in step 06; done under the
    clean-before-release rule.
  - Gotchas: Sonar's develop analysis showed new-rule hits (S6619,
    S6524) in old sync code, not only new code.

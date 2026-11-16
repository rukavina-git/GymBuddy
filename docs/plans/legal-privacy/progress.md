# Plan 9 progress

- Started 2026-11-13. Owner: photos get replaced from a clear source
  (step 01b); licences page built from existing components.
- 01 licence audit, Android part done: AboutLibraries gate (216
  libraries: 196 Apache, 3 MIT, 1 BSD-3, 16 Google SDK terms allowed
  for Google groups only), negative check (MIT removed → build fails),
  licences page under About checked on the phone. Open: backend gate,
  01b photos.
- Owner review: page is one table, licence text on its own screen.
  Audit found MIT/BSD/OFL shown as blank SPDX templates; now each such
  project's own licence file (copyright line) is used (ADR 0018).
  All 217 entries have a licence; Google SDK terms show as links.
- qa key fingerprints added to Firebase (gymbuddy-test-31dc1) by the
  owner: Google sign-in works in the App Distribution build. Same
  needed for Play's app-signing key on the prod app at first upload.
- 01 backend gate done: 206 modules, `check` runs `checkLicense`;
  10 weak-copyleft Spring Boot modules allowed by exact name (ADR
  0018). Negative check: Hibernate removed from the list → build
  fails. Not shown in the app (never shipped). Open in 01: 01b photos.
- 01b deferred (2026-11-14, owner): runs alongside, images land
  whenever the owner sends a grid (next: grid 02). 9.1 stays open
  until it's done. Next step: 02 privacy policy + terms.
- [x] 02 privacy policy + terms: web half done (097fad1). Texts in
  legal/, `legal/build.py` builds them, nginx serves SITE_HOST
  (rukavina.app) /gym-tracker/privacy (renamed in 02a; was /gymbuddy/) and /terms on the API cert,
  `legal-site.yml` publishes from main (prod approval). Contacts:
  privacy@rukavina.app (Cloudflare route, made by owner 2026-11-15),
  info@rukavina.app. Open: owner rollout on the VPS, then the Android
  links (need a Figma frame for the Welcome line). Terms need a
  lawyer glance before 9.6.
  - Deviations: texts built by a stdlib Python script (no Markdown
    library to licence or update). Reviewer factual fixes applied:
    Google picture link listed, units dropped (never sent), bio,
    notes/favourites and active plan listed.
  - Gotchas: VPS rollout order matters. compose.yaml now requires
    SITE_HOST, so add it to the server env file BEFORE copying the
    new compose.yaml (else deploy.sh and the nightly backup fail).
    Create /opt/gymbuddy/site as deploy before recreating nginx (else
    Docker makes it root-owned and the publish fails). nginx needs
    `up -d nginx` (new env + volume), not a reload. Publish only adds
    files, never deletes. Not fixed (NITs): double-escaped & in link
    URLs, docker/README.md lacks SITE_HOST + site folder.
- [x] 02a rename to "Gym Tracker": code + Figma done (6943160), device
  check open (no phone attached). Name in `brand_name` only; qa
  overrides `app_name` ("Gym Tracker Test"). Wordmark "gym tracker."
  (Figma R19 option A, component 151:106 updated in all locked
  frames). Legal URLs /gym-tracker/, export file
  gym-tracker-export-<date>.json. BrandNameTest guards strings and
  Kotlin literals (negative check: fails on "GymBuddy feedback").
  - Deviations: feedback subject uses `brand_name`, not `app_name`
    (qa mail would say "Gym Tracker Test feedback"). Package, class
    names, theme, repo, /opt/gymbuddy on the VPS unchanged (owner).
  - Gotchas: VPS rollout copies legal/site/gym-tracker (not
    gymbuddy). NIT left: HomeScreen wordmark contentDescription uses
    `app_name` (qa TalkBack says "Gym Tracker Test"); use
    `cd_app_name`. "by ikr" byline parked for v3/v4 (option D).
- 02a follow-ups (2026-11-15): Home wordmark contentDescription now
  `cd_app_name` (brand name, also on qa); docker/README.md documents
  SITE_HOST, the site folder and the rollout order. Figma board R20
  (02 Rounds, node 255-5853): Welcome terms line W1–W6 and consent
  link style C1–C4, owner picks before the 9.2 Android links.
- 02 Android links (a554cdb): Welcome line (R20 W1), consent (C1),
  About rows "Privacy policy" / "Terms of use". `ui/legal/LegalLinks.kt`
  holds the URLs and the link builder (LegalLinksTest). Also fixes CI:
  6943160 broke Android Lint (context.getString in onClick, now
  stringResource). 02 stays open: device check, VPS rollout (pages
  live), lawyer glance at the terms before 9.6.
  - Deviations: no link on Sign in; LinkAnnotation.Url instead of
    ACTION_VIEW (see step file notes).
  - Gotchas: Welcome Column doesn't scroll; on a small screen at 200%
    font the new legal line could be clipped. Check on the device; if
    it clips, make the Column scroll. Inline links are under 48dp
    (About rows are the full-size way in).
- 02 URL move (2026-11-15): rukavina.app is the owner's freelance site
  on Vercel, so SITE_HOST is now gymtracker.rukavina.app (owner pick)
  and the pages live at its root: /privacy, /terms, /legal.css
  (`BASE = ""` in legal/build.py). LegalLinks + test updated. No
  released build had the old URLs (AAB 0.1.0 predates the links).
  - Gotchas: DNS is a new A record for gymtracker only; never touch
    the apex. HSTS on SITE_HOST has no includeSubDomains, keep it so.
- 02 device check (2026-11-15, S731B over adb): qa launcher "Gym
  Tracker Test", Welcome + Home wordmark "gym tracker.", legal line
  with both links. At 200% font on a 1080x1920 screen the line was
  cut off ("policy." missing), so Welcome now scrolls when it can't
  fit (BoxWithConstraints + heightIn(min = viewport) keeps the
  weighted spacers). Rechecked: full line reachable; normal size
  unchanged. Open in 02: VPS rollout (gymtracker.rukavina.app),
  lawyer glance before 9.6. privacy@ forwarding tested by the owner.
- 02 live (2026-11-15): owner did the VPS rollout. gymtracker.rukavina.app
  /privacy, /terms, /legal.css return 200 with a valid cert, http
  redirects to https, / is 404 (free for a landing page later),
  rukavina.app (Vercel) untouched. Terms and Privacy links tapped on
  the phone open the pages. 02 + 02a done; carried to 9.6: lawyer
  glance at the terms.
- [x] 03 web account deletion: code done (abcc483), live after
  the next main release (carried to 9.6). Owner test 2026-11-16 on
  the test stack: delete-account.sh deleted a throwaway account
  (rows=4, firebaseUserDeleted=true), the app was signed out. Page gymtracker.rukavina.app/delete-account (in-app
  steps, email from the account's address, deleted vs kept), linked
  from the privacy policy. `delete-account.sh <uid>` runs the backend
  image once under the `account-delete` profile (AccountDeleteRunner
  → AccountDeletionService, no web server, main exits). Runbook in
  docker/README.md. Tests: runner deletes one user only, rejects bad
  uids, fails loudly on Firebase failure; profile starts no web
  server.
  - Deviations: reviewer SHOULD fixed (page said "nothing else is
    kept", but server logs are, per the privacy policy). build.py
    gained "1." ordered lists. SecurityConfig is now
    @ConditionalOnWebApplication (the non-web profile can't build
    HttpSecurity; the web app is unchanged). AccountDeletionService
    returns rows + firebaseUserDeleted (the endpoint ignores it).
  - Gotchas: the test stack only gets the runner once develop deploys
    (already true after this push); prod needs a main release first.
    Not fixed, follow-up for 9.4: FirebaseTokenVerifier doesn't use
    checkRevoked, so a still-signed-in device can push for up to ~1 h
    after deletion and recreate rows under the deleted uid. NITs left:
    mailto subject hardcodes "Gym Tracker" (not {app}); the script's
    output is ECS JSON (could pass LOGGING_STRUCTURED_FORMAT_CONSOLE=);
    "Google sign-in link" wording.
  - Decision (2026-11-16, owner): no 24/48 h grace period to undo a
    deletion. Deletion stays immediate (pending state, delayed job,
    restore UI and sync rules cost more than they save; the confirm
    dialog covers accidents). Optional for 9.4: stronger confirm
    (type DELETE or re-auth). Backups are never a restore path.
- [x] 04 compliance pass (51dd02f). Owner picks: checkRevoked on every
  request (1a), no age gate (2a). Verdicts:
  - Consent: OK, no change. Separate "Keep a copy on our servers?"
    screen with "Agree and create account" before anything uploads;
    the Sign in path with a new Google account goes NoAccount →
    Create account → consent (the probe user is deleted). Version +
    time stored (V6). Withdrawal = delete account, in the policy.
  - 16+: no gate; terms + policy say 16+; Play audience 16–17 and 18+
    (data-safety.md).
  - Crashlytics: OK. Keys are reason/traceId/httpStatus/discarded_<type>
    (random row ids); IOExceptions aren't reported; Moshi errors carry
    JSON paths, not values. Added an allow-list (allowedCrashKeys) +
    CrashKeysTest so a new key can't slip in.
  - Notifications: OK, no change. POST_NOTIFICATIONS at the first
    workout start; the service starts only while the app updates the
    notice (background start caught). Lock screen shows exercise, set
    and kg × reps (VISIBILITY_PUBLIC): training numbers, not body data;
    kept on purpose (glanceable, calm-workout rule).
  - Revoked tokens: fixed. FirebaseTokenVerifier uses
    verifyIdToken(token, true): deleted/disabled/revoked → 401 at once.
    No auth code or CERTIFICATE_FETCH_FAILED → 500 INTERNAL from the
    filter (the app retries on 500, logs out only on 401).
  - data-safety.md: Data safety rows, specialUse justification text,
    audience answers for 9.5.
  - Deviations: no end-to-end "delete then push gets 401" test:
    FirebaseTokenVerifier is off under the test profile and needs real
    Firebase; FirebaseTokenVerifierTest covers the mapping with the SDK
    mocked plus the filter's 500 path. No device check: no app UI or
    notification code changed.
  - Gotchas: one extra Firebase lookup per request (latency ~50–150 ms
    from the VPS); if it shows in p95, cache "uid alive" for a minute.
    A Bearer token on a public endpoint (/v1/health) during a Firebase
    outage now gets 500. Reviewer NIT: any no-auth-code Firebase error
    (e.g. bad service account) is a 500, not a 401, on purpose.
- 03 device check (2026-11-16) found two problems: the deleted account
  showed the quiet "Backup paused" banner instead of the deleted dialog
  (race: Firebase's own sign-out reaches AuthGate before probe records
  DELETED; logcat 20:22:32.539 vs .541), and the You card still said
  "Backed up · just now". Council 2026-11-16 (signed-out dialog) +
  owner decision: new step 04a, before 05. Owner also wants a full test
  + refinement pass before the real release (roadmap 10.1a).
- [x] 04a signed-out dialog + markers + race fix (HASH). Figma
  "Signed out · locked 2026-11-16" (271-7257). Blocking dialog in
  MainScreen over every tab, the workout and the summary; Back/outside
  do nothing; 500 ms tap guard. SignedOutPrompt: auto-show first time,
  then ≥ 48 h after Continue, max 3 (prefs `signed_out_prompt`, out of
  Auto Backup). Home chip, You card with lime Sign in, no Sign out row.
  Race: AuthGate waits ≤ 2 s for the failure + one-way expired → deleted.
  Device (S731B): dialog on launch, Back keeps it, Continue counts,
  restart < 48 h no dialog, chip opens it, Sign in → back brings it back,
  You card as R22-5.
  - Deviations: counter reset is in SyncModule afterSignIn, not the VM
    (at startup the gate briefly says SignedIn from the stored owner;
    found on the device, the count was reset on every launch). No
    Compose test for Back / over-workout (checked on the device; the
    dialog's onDismissRequest is a no-op). Not checked on the device:
    deleted-account case (needs a fresh Google account; unit-tested)
    and 200% font.
  - Gotchas: cold start with a stored owner and no Firebase user waits
    the full 2 s before SessionExpired (gate says SignedIn meanwhile).
    Reviewer, not fixed: no AuthGateViewModel tests (rule vs chip
    counting, sign-in back path); DELETED landing between the 2 s
    timeout and the state write stays expired (re-read failure after
    setting the state); onContinue writes prefs in two edits. Worth
    covering in 10.1a.

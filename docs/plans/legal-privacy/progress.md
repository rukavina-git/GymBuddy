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
- [ ] 02 privacy policy + terms: web half done (097fad1). Texts in
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
- [ ] 02a rename to "Gym Tracker": code + Figma done (6943160), device
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

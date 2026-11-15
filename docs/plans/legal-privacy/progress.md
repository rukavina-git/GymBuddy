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
  (rukavina.app) /gymbuddy/privacy and /terms on the API cert,
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

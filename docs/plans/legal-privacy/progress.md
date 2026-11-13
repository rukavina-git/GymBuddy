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

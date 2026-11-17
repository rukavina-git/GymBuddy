# 01 Licence audit + open-source licences page

## Problem
Assets are recorded by hand in DESIGN.md §10, but libraries aren't
recorded anywhere, and nothing stops a GPL or LGPL dependency from
slipping in. The built-in exercise photos are hotlinked from
github.com/yuhonas/free-exercise-db: the repo is Unlicense, but the
photos' original source isn't stated (inherited from
wrkout/exercises.json), so the app can't show them commercially with
confidence. Play and most licences (Apache 2.0 NOTICE, MIT, OFL) also
want the licence texts shown to users.

## Change (Claude)
- Android: AboutLibraries Gradle plugin + core (Apache 2.0,
  github.com/mikepenz/AboutLibraries). It collects every runtime
  dependency's licence at build time. `strictMode = FAIL` with
  `allowedLicenses` = the invariant list, so CI fails on anything
  else. Unknown licences get an explicit entry in
  `android/config/libraries/` after a manual check, never a blanket
  allow.
- Manual entries for bundled assets: Material Symbols (Apache 2.0),
  Figtree (OFL 1.1), Fluent Emoji (MIT), with their texts.
- In-app page: About → "Open-source licences" row → list of names and
  licences; tapping one shows the full text. Built from existing
  components (ListRow, BackTopBar, body text).
- Backend: a Gradle task that writes the runtime dependency list with
  licences to `backend/build/reports/licences`, and the same allow-list
  check in CI (backend isn't shipped to users, but it is our code to
  sell later).
- DESIGN.md §10: add the libraries line ("generated, see the app's
  licences page").
- ADR for the licence gate (tool, allow-list, how to add an exception).

## Owner decisions (2026-11-13)
- Exercise photos: **replace with a clear source** before release.
  Own step (01b): find candidates, owner picks, then swap the URLs.
  Not part of this step.
- Licences page: build from existing components (ListRow, BackTopBar)
  without a Figma frame first; add a frame afterwards for the record.

## Tests
- Unit: licences screen lists entries from the generated data.
- CI: a deliberately disallowed licence fails the build (checked once
  locally, not committed).
- Device: About → licences → one entry's text, screenshot.

## Done when
Both builds pass the licence gate and the page is on the phone. The
photo item in DESIGN.md §10 stays open until 01b.

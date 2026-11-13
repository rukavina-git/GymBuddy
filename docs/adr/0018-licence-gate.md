# 18. Licence gate in the build, licences page in the app

Date: 2026-11-13
Status: Accepted

## Context

The owner rule (CLAUDE.md) allows only commercial-friendly licences and
wants every third-party asset recorded. Assets were recorded by hand in
`design/DESIGN.md` §10, libraries nowhere, and nothing stopped a GPL or
LGPL dependency from arriving with an update. Most licences also ask
for their text to reach users.

## Decision

- **Android:** AboutLibraries Gradle plugin 15.3.0 (Apache 2.0),
  `com.mikepenz.aboutlibraries.plugin.android`. It collects every
  runtime library's licence each build. `strictMode = FAIL`: any
  licence outside the allow-list fails the build, CI included.
- **Allow-list:** Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause, ISC,
  OFL-1.1, CC0-1.0, CC-BY-4.0, Unlicense. Google's SDK terms (Android
  SDK License, Play Core and Play Integrity terms) are allowed only for
  Google's own SDK groups (`allowedLicensesMap`), since Firebase Auth
  and Play services ship under them.
- **Exceptions:** a library with an unknown or missing licence gets a
  JSON entry in `android/config/aboutlibraries/libraries/` after a
  manual check, never a blanket allow. Bundled assets (icons, font,
  avatars) are listed there too, so they show in the app.
- **In-app page:** About → Open-source licences: a table of every library and asset with its licence; tapping a licence opens its text on its own screen. Built from the report
  the plugin bundles (`res/raw/aboutlibraries.json`), parsed with the
  app's Moshi. No AboutLibraries runtime library: its 15.x core targets
  Java 21, and the page uses the app's own components.
- **Real licence texts:** MIT, BSD and OFL must ship with the project's
  own copyright line, but the plugin's SPDX texts are blank templates.
  Those projects get their own licence file in
  `android/config/aboutlibraries/licenses/` (copied from the project
  repo), and the original artifacts are excluded by exact name and
  re-added from `config/aboutlibraries/libraries/` with that licence.
  Today: Calendar (kizitonwose), DataStore's repackaged Protocol
  Buffers, Fluent Emoji, Figtree. A new MIT/BSD/OFL dependency needs
  the same; Apache 2.0 needs nothing extra.
- **Backend:** same allow-list, checked in its build (step 01, second
  part).

## Consequences

- A dependency update that changes licence fails CI and needs a
  decision before merge.
- The licences page updates itself with every build.
- DESIGN.md §10 keeps the asset rows (source, date, how bundled);
  libraries are no longer listed by hand.

# Plan 9: legal + privacy

## Why
Play won't take the app to production without a privacy policy, a
web account-deletion page, a Data safety form and a content rating.
The owner rule (CLAUDE.md) also needs every third-party asset and
library recorded with a commercial-friendly licence before release.
Closed testing (10.3) creates real accounts on prod, so all of this
must be live first.

## Read first
`docs/roadmap-details.md` (Plan 9), `design/DESIGN.md` §10 (Licences),
`docs/council/2026-11-02-onboarding.md` (compliance checklist).

## Scope
1. Licence audit: every library and asset with source and licence,
   enforced in CI; in-app open-source licences page from About.
2. Privacy policy + terms at a public URL, linked in-app.
3. Web account deletion page.
4. Compliance pass: health-data consent, 16+, Crashlytics payloads,
   timer/notification rules.
5. (me) Play Console: Data safety, content rating, audience, listing.
6. (me) CI green, PR develop → main.

Step files are written one at a time, each after the previous step
lands.

## Invariants
- Only MIT, Apache 2.0, BSD, ISC, OFL, CC0, CC BY (credit), Unlicense
  or public domain. Never NC, ND, SA, GPL, LGPL or AGPL.
- Every third-party asset has source, licence and date in
  `design/DESIGN.md` §10; every library is listed by the build.
- Legal texts live in the repo (reviewable diffs), published from it.
- No new data collection. If a step needs one, it stops and asks.

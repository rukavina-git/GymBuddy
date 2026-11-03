# 05 Profile fields

## Problem
Onboarding and You (Plan 7.4, Figma 114-20520) need four things the
profile can't hold: experience ("New to lifting" / "Some experience" /
"Experienced"), a preset avatar (Fluent Emoji animals, MIT; no photos,
memory avatar-presets), the health-data consent given at account
creation ("Back up my workouts and body data on GymBuddy servers."),
and days a week ("1–3 days" / "4 days" / "5–7 days" / "Not sure yet").
Roadmap 6.5. **Spec change (owner OK 2026-11-02), Flyway migration,
Room schema change.**

## Owner decisions: 1A, 2A (2026-11-03)
1. **Days a week**
   - **A (recommended): the bucket the screen offers.** Enum
     `ONE_TO_THREE`, `FOUR`, `FIVE_TO_SEVEN`; null = "Not sure yet".
     Stores exactly what was asked; plan picking (06) maps a bucket
     to a plan.
   - **B: a number 1–7.** Finer, but the screen never asks for one,
     so we'd invent 2, 4 or 6.
2. **Consent**
   - **A (recommended): record only.** `healthDataConsentVersion`
     (e.g. "2026-11") and `healthDataConsentAt` (when the box was
     ticked). The server stores what it gets. Whether sync requires it
     is decided in the privacy pass (roadmap 4.6) with the real wording.
   - **B: enforce now.** The server refuses workouts and body weight
     without consent. Existing accounts have none, so their sync stops
     until a consent screen exists (Plan 7.4).

## Change
- **Spec** (said out loud), UserProfile gains, all optional/nullable:
  `experienceLevel` (enum `NEW`, `SOME`, `EXPERIENCED`),
  `avatarId` (string, `^[a-z0-9-]{1,32}$`, e.g. `cat`; the client
  owns the list), `trainingDays` (per decision 1),
  `healthDataConsentVersion` (string), `healthDataConsentAt` (epoch
  ms). `profileImageUrl` stays in the contract, marked deprecated
  ("no photos in v1"); clients stop sending it.
- **Backend:** `V6__profile_fields.sql` (nullable columns, CHECKs for
  the enums and the avatar pattern), DTOs, repository, validation
  (bad enum/pattern → INVALID), drift test.
- **Android:** Room v29 (`MIGRATION_28_29`, nullable columns, 29.json,
  MigrationTestHelper test). Domain `UserProfile` + enums, mapper,
  SyncDtoMapper. Existing saves keep the new fields (no field is
  dropped when Profile edits other ones).
- No UI (Plan 7.4 sets them). Old photo picker code, if any remains,
  is removed.
- Step 02 note: a guest → account merge keeps the account's profile
  when it has one, so a guest's experience/avatar are kept only for a
  new account. Unchanged here.

## Tests
- Backend: push with all fields → pull returns them; bad enum, bad
  avatar id → INVALID; null fields stay null; drift test.
- Android: migration 28→29 keeps profile rows; mapper round trip of
  each field; saving the profile from the current Profile screen path
  keeps experience/avatar/consent untouched; push/pull round trip
  with a canned server.

## Out of scope
Onboarding/You screens (7.4), avatar artwork and its licence record
(7.2, MIT, recorded there), consent wording and enforcement (4.6),
plan picking from days (06).

## Done when
`npm run lint`, backend `./gradlew test`, android `testDebugUnitTest`
and `assembleDebug` green. No manual checks (owner: none until the
new UI).

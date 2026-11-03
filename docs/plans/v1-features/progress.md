# Progress

- [x] 01 Per-account local data (steps/01-per-account-data.md) — 4b61dce
- [x] 02 Guest mode (steps/02-guest-mode.md) — ab07df5
- [x] 03 Auth gate (steps/03-auth-gate.md) — 12dd9ea
- [x] 04 Body weight (steps/04-body-weight.md) — c60d997
- [x] 05 Profile fields (steps/05-profile-fields.md) — 291c23b
- [x] 06 Plans + default templates (steps/06-plans.md) — 224ea8e
- [x] 07 Data export (steps/07-export.md) — 1f4f248
- [ ] 08 CI green, PR develop → main (me)

## Deviations
- 01: Signed out opens `gym_buddy_signed_out.db` (the step file didn't
  say). Step 02 should turn it into the guest file.
- 01: The owner check sits in SyncEngine (`isSignedIn` = auth uid ==
  holder owner, checked under the lock, also in flushOutbox). The step
  put it in the workers. Workers and scheduler are unchanged.
- 01: OutboxRecorder's write lambda now receives the transaction's
  AppDatabase, so a write and its outbox entry land in the same file.
- 01: Plan 4 push-trigger fix: KEEP while a push waits, APPEND_OR_REPLACE
  when one is RUNNING. No test for the RUNNING case.
- 01: Backup rules: comments only. The defaults already back up every
  Room file.
- 02: No sign-up vs sign-in check. Rule: account empty after a good
  sync → merge without asking; otherwise (data, or sync failed) ask.
  A new account is always empty.
- 02: Sign-in completes before the question; Cancel signs out back to
  the guest (Figma asks before sign-in completes).
- 02: DatabaseHolder keeps the previous file open until the next switch
  (step 01 gotcha), instead of closing it on the switch.
- 02: Guests got a plain "Sign in or create account" button in Settings
  (reviewer BLOCKER: nothing led a guest back to Login). Not in the
  step file.
- 02: Profile and Settings read the uid per call (`?: "guest"`), so a
  switch can't leave them writing under the old owner.
- 03: Firebase's own sign-out can't say why. Without an error seen first
  (`lastFailure`), it counts as expired.
- 03: The gate also watches the owner release (reviewer BLOCKER: logout
  while expired left a spinner, since Firebase's uid was already null).
- 03: Profile/Settings use `holder.owner` (via UserProfileRepository)
  instead of Firebase's user, so an expired session isn't a guest.
- 04: No body weight count in Sync debug (the step file mentioned it).
- 04: Spec (said out loud): `BodyWeightEntry` schema, `bodyWeightEntries`
  in PushRequest/PullResponse, `BODY_WEIGHT_ENTRY` in EntityType.
  Flyway V5, Room v28 (MIGRATION_27_28, 28.json).
- 04: The backend also checks `measuredAt > 0`; the spec has no
  `minimum` for it.
- 05: Spec (said out loud): UserProfile gains experienceLevel, avatarId,
  trainingDays, healthDataConsentVersion, healthDataConsentAt; enums
  ExperienceLevel, TrainingDays; profileImageUrl marked deprecated.
  Flyway V6, Room v29 (MIGRATION_28_29, 29.json).
- 05: A bad enum isn't INVALID as the step said: Jackson rejects it
  first and the server answers 500 for any unreadable push body (also
  for Gender etc., older than this step). Fixing it needs a 400 on the
  push endpoint in the spec. Not done; test dropped.
- 05: ProfileViewModel saves start from the stored row (so new fields
  survive). The Plan 4 note "inject AuthSession" was already covered by
  step 03 (uid from the data owner).

- 06: Spec (said out loud): `StarterPlan` schema, required
  `TemplateLibraryResponse.plans`, `UserProfile.activePlanId`. Flyway V7,
  Room v30 (MIGRATION_29_30, 30.json), template library v6.
- 06: V2's generator is frozen (refuses inputs past v5). V7 has its own
  generator (backend/tools/generate_starter_plans_seed.py) that emits
  only the v6 delta. Not in the step file.
- 06: `suggestPlan(plans, days, experience)` takes the plan list and
  picks by `suggestedFor`; `nextInPlan` takes an `isAvailable` filter
  for "deprecated template skipped". Both in domain/usecase/template.
- 06: `setActivePlan` returns false (writes nothing) without a profile
  row for the owner in the open file; that also covers an owner switch.
- 06: Backend checks activePlanId is a UUID (INVALID otherwise); the
  plan itself isn't checked (soft reference).
- 06: Templates picked by Claude (5–6 exercises each, existing defaults).
- 07: CSV dropped (owner): ADR 0015 amended, roadmap/overview updated,
  Figma note added beside R15-11 (no checkboxes in 7.10).
- 07: Sheet follows R15-11 minus checkboxes, plain M3 (ExportSheet.kt).
  Replaces the old "not implemented" Export Data row.
- 07: No "owner checked before and after": the read runs under
  `holder.lock` in one transaction, so no switch can happen meanwhile.
- 07: New `ExportDao` (one-shot reads). Profile is exported without
  uid and profileImageUrl; `owner` is "guest" or "account".
- 07: R8 keep rules for `domain.export.**` and all `domain.model` enums
  (reviewer BLOCKER: reflective Moshi in release would rename keys).

## Gotchas
- From 01 (reviewer, not fixed):
  - `switchTo` closes the old file right after publishing the new one. A
    repo read or write in flight, or `getPerformedExercisesWithSets`
    inside a flow, can hit the closed DB and crash. Step 02/03 should
    delay the close (after the new DB's first emission) or ref-count.
  - The UI isn't gated on the switch: after B signs in, screens show the
    previous file until `switchTo` gets the sync lock. Harmless now
    (empty signed-out file), but a guest → B leak once step 02 adds
    guest data. Gate the UI on `holder.owner == uid`.
  - AppModule deletes the legacy file if `currentUid` is null. It also
    is null when Firebase errors (`runCatching`). Only delete when
    Firebase is known to be up.
  - Legacy rename moves the main file before `-wal`. Move side files
    first.
  - Seeders read `holder.db`, not the DB the flow emitted.
  - `observeSignIn` has no test (switch before SIGNED_IN, no flip
    through the signed-out file on cold start).
- Repos/stores take `DatabaseHolder`; tests use `DatabaseHolder.fixed(db)`.
- Open from Plan 4 (manual, owner): step 03 "Don't keep activities"
  check and a release build reading a draft; step 04 airplane-mode
  push check.
- From Plan 4 step 04: `push-outbox` uses KEEP, so a set logged while
  the job is still running can wait up to 6 h. Fix it if step 01
  touches the workers anyway.
- ProfileViewModel reads the Firebase uid statically; step 05 should
  inject AuthSession so its save paths become testable.
- From 02 (reviewer, not fixed):
  - Guest flag isn't cleared when a guest with no data signs in. After
    that account logs out, Splash opens Main as a guest, not Login.
  - `active_workout_draft` is one row per file: INSERT OR IGNORE drops
    the guest's draft if the account has one, and `guestData()` ignores
    drafts (not in the outbox), so a draft-only guest never merges.
  - No tests for "erase clears the flag" (SettingsViewModel) or the
    `ownerReady` gate.
  - Merge commits, then deletes the guest file. A crash in between
    asks again and re-adds the outbox. Clear the guest outbox first.
  - `ownerReady` can skip its false frame, so `MainContent` isn't
    recreated and init-only state (Settings `isGuest`) can go stale.
  - The uid collector waits on `syncNow`; a sign-out during it waits.
- Step 03 should reuse `GuestMerge` for "account deleted elsewhere:
  keep as guest" (copy into the guest file is the reverse direction;
  not built).
- From 03 (reviewer, not fixed):
  - A deletion can show as "expired" for good: Firebase's listener can
    beat `remember()` in the catch, and `lastFailure` is memory only
    (lost on restart). Persist it with the owner; re-check while expired.
  - Keep/Erase: no in-flight guard, no try/catch. A double tap or an IO
    error crashes (`check(holder.owner == owner)` in viewModelScope).
  - Kept rows keep their ids. If the server still has A's rows
    (USER_DISABLED, or a console delete without AccountDataDeleter), a
    later account's push gets FORBIDDEN per row. Re-key or limit Keep
    to USER_NOT_FOUND.
  - Untested: SyncScheduler → onUnauthorized, OwnerReleasingAuthSession
    and LogoutCoordinator releasing, `probe()` with no user, keep-as-guest
    for templates, PENDING state, tombstones left behind.
  - ProfileViewModel.logout() calls FirebaseAuth.signOut() directly
    (unused). Delete it.
  - `idToken()` remembers EXPIRED for transient FirebaseAuthExceptions
    until the next sign-in.
  - The expired banner sits above MainContent's Scaffold; the status bar
    inset may be doubled. Plain UI, Plan 7.4 replaces it.
- From 04 (reviewer, not fixed):
  - BodyWeightRepositoryImpl reads `holder.owner` for the profile uid
    before the transaction; a switch in between can skip the 1A profile
    update or queue USER_PROFILE under the wrong key. Read it inside.
  - 1A only sets profile.weight on save of the newest entry. Deleting
    it or back-dating it leaves the profile at the old value.
  - Untested: 1A for a guest ("guest" profile), backend idempotent
    replay for body weight, deleting an id the server never saw.
  - A tombstone with an out-of-range weight hits the DB CHECK → ERROR
    (retried forever), not INVALID. Android can't send one today.
- Step 07 (export) must include body weight entries.
- From 05 (reviewer, not fixed):
  - A pre-05 app build pushing its profile sends no new fields, and the
    server overwrites them with null (incl. the consent record). Fine
    while no such build is out; otherwise keep stored values on omit.
  - No backend test that a replay changing only a new field is a
    CONFLICT (storedContent covers them today).
  - The per-field save path (saveProfileToDatabase) has no test; it
    now also keeps deletedAt from the stored row.
- Fixed (cleanup before 08): an unreadable push body now answers 400
  VALIDATION_FAILED (GlobalExceptionHandler, SyncUnreadableBodyTest).
  No spec change: push already listed 400 BadRequest. Android still
  retries a 400 like any failed sync (no client change).
  Reviewer nits (not fixed): enum test body lacks required profile
  fields; no assert on the generic message; handler logs Jackson's
  message (may echo a sent value) at info, cursor handler uses debug.
- From 06 (reviewer, not fixed):
  - Fixed (cleanup before 08): both seeders replace DEFAULT rows (and
    plans) from the bundle when the guest file's version is older.
    Custom rows and overlays kept; accounts unchanged (D-35). Seeding
    runs under `holder.lock` and reads db + owner once, so a switch
    can't land mid-seed (reviewer should-fix, fixed). First install now
    also writes in one transaction. Seeding holds the sync lock briefly
    at each file open. Not fixed: no test for overlays surviving or for
    templates untouched at equal version.
  - `nextInPlan` doesn't drop sessions with null endedAt (falls back to
    startedAt). Sessions from a copied template (derivedFromId) don't
    advance the rotation; decide before 7.4/7.5.
  - Backend accepts uppercase activePlanId; Postgres returns lowercase,
    so an uppercase replay is a CONFLICT. Android sends lowercase.
  - No test ties the bundled JSON v6 to V7; only the generator does.
- From 07 (reviewer, not fixed):
  - `exerciseStates`/`templateStates` include overlays on tombstoned
    custom rows (dangling ids). Importers must tolerate unknown ids, or
    filter them before the format is in users' hands.
  - `exportSummary` (sheet text) has no test.
  - `loadExportCounts` (like erase counts) doesn't catch a DB error.
  - The other-owner test only checks the profile; isolation comes from
    the per-owner file.
- The v1 fixture `app/src/test/resources/export/v1-sample.json` is the
  format contract: only add fields; any change to it needs a reason.
- Release build (R8) not run locally; first CI release build checks
  the new keep rules.

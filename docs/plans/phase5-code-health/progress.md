# Progress

- 01 OWASP: moved to roadmap 10.0, just before release (owner 2026-11-02). Work so far is
  on `feature/owasp-triage` (steps/01-owasp.md)
- [x] 02 Remove custom profile photos (steps/02-image-storage.md) (fd871c3)
- [x] 03 Active workout failure paths + process death (steps/03-active-workout-tests.md) (aedd05a)
- [x] 04 Durable push via WorkManager (steps/04-durable-push.md) (3e5c458)
- [x] 05 CI green, PR develop → main (me) (steps/05-merge-to-main.md) (61a2de5)

## Deviations
- 01 moved out of this plan (owner: not before the core app).
  Findings so far: last runs died on NVD 503s (cache was only saved on
  success, so every run started cold) and, on Android, an
  IllegalAccessError from commons-lang3 3.16 (root, AGP) vs 3.20
  (app). Both fixed on the branch; no scan finished yet.
- 02 rewritten from "fix ImageStorageUtil" to "remove custom photos"
  (owner: preset avatars only). Overview scope and the no-UI-change
  invariant updated to match.
- 02: no test that a save sends `profileImageUrl = null`.
  ProfileViewModel reads the Firebase uid statically, so every save
  path is unreachable in unit tests. Two hard-coded nulls
  (ProfileViewModel onSaveClick, saveProfileToDatabase).
- 03: owner chose B (2026-11-02). Room v27, MIGRATION_26_27 adds the
  local-only `active_workout_draft` (one row, JSON via a reflective
  Moshi). Not in the step file: the draft is also wiped in
  `SyncDao.wipeLocalUserData` (logout/account switch); R8 keep rule
  for `ActiveWorkoutDraft` in proguard-rules.pro (reviewer BLOCKER);
  a running timer adds the time away on restore, a paused one doesn't.
- 03 test audit (gaps): no tests at all for Auth, Login, Exercise,
  Home, AppPreferences ViewModels. Profile: save paths (static uid).
  WorkoutTemplate: no delete, no repository exception. WorkoutSession:
  no repository exception. Settings and SyncDebug look complete.
- 04: trigger is the 0 → non-empty edge of `observeOutboxCount()`, so
  a non-empty outbox at app start also enqueues. `PushOutboxWorker` is
  a copy of `SyncWorker` with reason OUTBOX_PENDING. Backoff:
  exponential, 30 s base. Wired in GymBuddyApplication via SyncDao.
- 05: owner checked step 03 on a device (workout survives, 2026-11-02).
  Step 04's airplane-mode check skipped for now (owner).

## Gotchas
- Dependabot: 35 alerts, all fixed (checked 2026-11-02 via gh api).
- Last OWASP runs (2026-10-29/30) failed after 14 min to 4.5 h: the NVD
  sync. Step 01 must cache `dependency-check-data` between runs.
- ActiveWorkoutViewModel keeps the session in memory only (no
  SavedStateHandle, no draft table). Step 03 needs an owner call.
- From 02 (reviewer, not fixed): AccountDeletionCoordinatorTest passes
  its own folder lambda, so SyncModule's real "profile_images" lambda
  is untested. Server junk paths clear only when the user saves some
  other profile field (hasUnsavedChanges ignores the image). 6.5
  replaces profileImageUrl with an avatar id: drop the column then
  (Room migration + spec change). ProfileViewModel needs an injected
  uid/AuthSession to be testable (6.5 or 6.1 is a good time).
- From 03 (reviewer, not fixed): draft read/write errors in
  ActiveWorkoutViewModel aren't caught, so a DB error would crash
  viewModelScope. The draft is cleared after the session insert, not
  in the same transaction: death in between brings a saved workout
  back (duplicate on re-save). New workout started before the draft
  loads overwrites it silently (fine until 7.6's resume prompt).
  Draft weights don't record their units. No test of the conflated
  writer's ordering or of a throwing draft repo.
- 03 manual check still open: "Don't keep activities", leave the app,
  come back, workout is still there. Also check a release build reads
  a draft (only R8 mapping was checked, no device run).
- From 04 (reviewer, not fixed): KEEP also keeps a *running* job. A set
  logged during the pull after the push emptied the outbox (0 → 1)
  gets no new job and waits for app open / 6 h periodic. Fix:
  APPEND_OR_REPLACE, or the worker re-checks outboxCount after syncNow.
  Untested: backoff policy, syncNow itself returning SIGNED_OUT.
  PushOutboxWorker duplicates SyncWorker; FakeAuth duplicates the
  inline fake in SyncTriggersTest.
- 04 manual check still open: airplane mode, log a set, swipe away,
  airplane off, set reaches backend without opening the app.

# Progress

- 01 OWASP: moved to roadmap 8.1 (owner 2026-11-02). Work so far is
  on `feature/owasp-triage` (steps/01-owasp.md)
- [x] 02 Remove custom profile photos (steps/02-image-storage.md) (HASH)
- [ ] 03 Active workout failure paths + process death (steps/03-active-workout-tests.md)
- [ ] 04 Durable push via WorkManager (steps/04-durable-push.md)
- [ ] 05 CI green, PR develop → main (me) (steps/05-merge-to-main.md)

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

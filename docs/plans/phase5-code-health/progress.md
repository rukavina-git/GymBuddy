# Progress

- 01 OWASP: moved to roadmap 8.1 (owner 2026-11-02). Work so far is
  on `feature/owasp-triage` (steps/01-owasp.md)
- [ ] 02 ImageStorageUtil write-then-delete (steps/02-image-storage.md)
- [ ] 03 Active workout failure paths + process death (steps/03-active-workout-tests.md)
- [ ] 04 Durable push via WorkManager (steps/04-durable-push.md)
- [ ] 05 CI green, PR develop → main (me) (steps/05-merge-to-main.md)

## Deviations
- 01 moved out of this plan (owner: not before the core app).
  Findings so far: last runs died on NVD 503s (cache was only saved on
  success, so every run started cold) and, on Android, an
  IllegalAccessError from commons-lang3 3.16 (root, AGP) vs 3.20
  (app). Both fixed on the branch; no scan finished yet.

## Gotchas
- Dependabot: 35 alerts, all fixed (checked 2026-11-02 via gh api).
- Last OWASP runs (2026-10-29/30) failed after 14 min to 4.5 h: the NVD
  sync. Step 01 must cache `dependency-check-data` between runs.
- ActiveWorkoutViewModel keeps the session in memory only (no
  SavedStateHandle, no draft table). Step 03 needs an owner call.

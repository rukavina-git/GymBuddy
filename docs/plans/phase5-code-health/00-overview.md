# Plan 4: code health

## Why
Fix what's known to be broken before Plans 5–7 build on it: CVE
scanning that nobody runs, a profile-photo bug that can lose the photo,
untested failure paths in the active workout, and offline edits that
wait for the next app open (roadmap-details.md, Plan 4).

## Scope
1. (moved to roadmap 8.1) OWASP.
2. ImageStorageUtil: write the new file before deleting the old one.
3. Active workout: test gaps, and decide what happens on process death.
4. Durable push: a WorkManager job when the outbox is non-empty.
5. (me) CI green, PR develop → main.

Out of scope: staging/prod split (Plan 5), new features (Plan 6), UI
changes (Plan 7), Android Lint in CI (8.1).

## Invariants
- Sync rules in android/CLAUDE.md hold.
- No `api/openapi.yaml` change. Room schema stays v26 unless step 03's
  owner decision says otherwise (then a Migration + test).
- No user-visible UI change.
- OWASP stays non-blocking (ADR 0013) unless the owner says otherwise.
- Every new dependency: commercial-friendly licence, recorded.
- Each step builds and passes its tests on its own.

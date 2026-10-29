# 07 CI green, PR develop → main (me)

1. All workflows green on the latest develop commit: Android Build,
   Backend Build, Sync E2E, API Client Codegen, API Docs (OWASP may
   fail, non-blocking per ADR 0013).
2. Claude drafts the PR description (what Plan 1 and the
   phase4-hardening plan changed, CI links). I open and merge the PR.
3. Update docs/status.md "CI green at" to the merged commit.
4. Tick Plan 1 in docs/roadmap.md, set `docs/plans/ACTIVE` to the
   next plan once its folder exists.

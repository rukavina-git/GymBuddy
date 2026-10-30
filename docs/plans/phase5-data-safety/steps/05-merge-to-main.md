# 05 CI green, PR develop → main (me)

1. All required checks green on the latest develop commit, plus the
   CodeQL Analyze jobs.
2. Claude drafts the PR description (what Plan 2 changed, CI links).
   I open and merge the PR.
3. Manual Deploy on Render (autoDeploy is off), then the step 04
   manual check: one JSON log line with traceId, userId and client
   version.
4. Update docs/status.md "CI green at" to the merged commit.
5. Tick Plan 2 in docs/roadmap.md, set `docs/plans/ACTIVE` to the
   next plan once its folder exists.

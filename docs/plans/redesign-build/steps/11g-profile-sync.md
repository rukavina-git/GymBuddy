# 11g Feedback: F1 + F12 profile sync (avatar revert, backup "not possible")

## Problem
F1: a picked avatar shows for a second, then goes back to empty.
F12: Back up with 2 changes waiting says it's not possible right now.

Diagnosed on the phone (2026-11-07, adb):
- Picked Fox. Push APPLIED (no sync_log row, outbox empty), profile
  revision 7 → 8, but `avatarId` is null again after the pull.
- logcat: `reference data check failed: Required value 'plans' missing`.
  The live `/v1/reference` has no `plans`, so the server on Render is
  older than Plan 6 (V6 avatarId, V7 starter plans). It ignores
  `avatarId` on push and returns the row without it.
- `render.yaml` has `autoDeploy: false`. PR #6 (Plan 6) reached main,
  but Render was never redeployed. Client and backend code are fine.
- F12 is likely the same stale server (Plan 6 fields or endpoints the
  old build rejects). Confirm after the redeploy.

## Change
1. (me) Render dashboard → gymbuddy-backend → Manual Deploy → latest
   commit. The service tracks develop (backend same as main but one
   enum line); it stays the de facto test server until Plan 5. Flyway
   runs V6 + V7 on Neon at startup.
2. Claude, on the phone: check `/v1/reference` returns `plans`, re-pick
   an avatar, confirm it survives a sync (Room row + UI). Make a change,
   tap Back up, confirm it backs up. If F12 still fails, read the
   reason (sync_log / logcat) and fix in this step if it's small,
   otherwise split to its own step.
3. Docs: `docs/status.md` and docker/README.md "Hosting": Render does
   not auto-deploy; redeploy after every merge to main. Add "Redeploy
   Render" to step 12's checklist.

No code change expected; no api/openapi.yaml or Room change.

## Out of scope
- Auto-deploy or a version check between client and server (Plan 5 /
  8.1 CI gates).
- Client-side guard against a server that drops fields.

## Done when
- Avatar survives sync on the phone; Back up succeeds with changes
  waiting.
- F1 and F12 ticked in device-feedback.md with the commit hash.
- Docs updated; committed and pushed on develop.

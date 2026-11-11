# 11 Cross-flow states

## Problem
Steps 04–10 built every screen, but the states that cut across them
are still v1 or missing. Roadmap 7.11.

What exists vs Figma:
- 410 re-download (`SyncEngine.handleCursorExpired`) pushes the outbox,
  then `wipeLocalUserData()`, which **also deletes the active workout
  draft** (`wipeActiveWorkoutDraft`). Sets logged during a re-download
  are lost. Nothing tells the UI a re-download is running or failed.
- Couldn't save in the workout: `WorkoutEvent.SaveFailed` shows a
  snackbar, not the inline card (B1). `Banner` (ui/design/Feedback.kt)
  exists, unused here.
- Reopening a workout: `restoreDraft()` always resumes silently, any age.
  No "Unfinished workout" prompt after 3 h (E2).
- Backup stuck: the You card shows "Couldn't back up" after 24 h (10a).
  Home's week card has no line (C1b, 3+ days).
- Exercise page: shows Progress for any lift with points; no "First
  time" box for a lift done once (D3, Progress from 3 sessions).
- Photo offline: `Photo()` hides itself on error; Figma wants a quiet
  stand-in (E4).
- Body weight empty (E5) and Hidden lists empty (E6) use the new
  `EmptyState` already; check copy against Figma only.
- `ui/components/**` (EmptyState, LoadingState, ThumbnailCard and 8
  more) has no callers left. The roadmap's `AppSnackBar` and
  `UnsyncedChanges` screens are already gone.

Figma (03 Locked designs):
- Error/offline states https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=124-26620
  (R17-B1 couldn't save 124:22976, E1 restored 124:25747, E2 unfinished
  124:25838, C1b backup line 124:23423, R1 backing up 124:24870, E3
  retry offline 124:25915, D3 no section 124:24149, C2a re-download in
  place 124:23644, R2 couldn't get 124:24944, R3 start sheet while
  reloading 124:24981, E4 photo offline 124:25987, E5 body weight empty
  124:26089, E6 hidden empty 124:26161; notes 124:26636).

## Owner decisions (proposed, need "go")
1. **Split**: 11a workout safety (410 keeps the draft, B1, E1/E2);
   11b everything else (C1b/R1/E3, C2a/R2/R3, D3, E4, E5/E6, delete
   `ui/components`). Recommended: split; 11a touches sync.
2. **410 and the draft**: exclude the draft from the 410 wipe (keep
   `wipeActiveWorkoutDraft` for logout/guest erase only). The draft's
   exercise ids stay valid: defaults are kept, own exercises come back
   in the pull. A data/sync change, allowed by this step. No Room schema
   change.
3. **"Last set" time for E2** (3 h rule, Finish ends at last set): use
   the draft's `savedAt` (last write) if the draft has no per-set time.
   Recommended: `savedAt`, no schema change. If it's wrong in use,
   follow-up.
4. **Re-download signal**: `SyncEngine` exposes a small
   `StateFlow<Resync>` (Idle / Running / Failed); Home's templates card
   (C2a/R2) and the Start sheet (R3: starters + Empty) read it. No new
   sync behaviour.
5. **Backup line** (C1b): reuse 10a's backup-state logic, threshold 3
   days on Home, 24 h stays on You. Server down = "changes waiting", no
   own message (notes).

## Change (11a): workout safety
- `SyncDao`: a `wipeForResync()` without the draft; `handleCursorExpired`
  uses it. Logout and guest erase unchanged.
- B1: `SaveFailed` shows a danger `Banner` above the Done/Finish button,
  "Storage settings" (Android storage intent) + "Try again" (retries the
  write). Live region. `Haptic.SaveFailed` split from `Reject` with the
  API < 30 LONG_PRESS fallback (gotcha from 01).
- E1/E2: draft < 3 h → straight back in (today). ≥ 3 h → dialog:
  Discard (confirms), Resume, Finish (ends at `savedAt`).

## Change (11b): backup, re-download, small states
- Home week card: one 48dp line "Not backed up for N days · Retry"
  (C1b); tap → "Backing up…" then gone (R1); offline → "Needs internet"
  4 s, back to Retry (E3). TalkBack per notes.
- Re-download: templates card in place, Start still works (C2a);
  failed → same card + Retry (R2); Start sheet shows starters + Empty
  while reloading (R3).
- Exercise page: done once → "First time" box, no Progress section until
  3 sessions (D3).
- Photo offline: quiet stand-in at the photo size (E4).
- E5/E6 copy to Figma. Delete `ui/components/**`.

## Tests
- Unit (11a): 410 path keeps the draft, logout still wipes it; draft
  age < 3 h restores, ≥ 3 h asks; Finish uses `savedAt`; Try again
  retries; HapticsTest SaveFailed fallback.
- Unit (11b): backup line hidden < 3 days, shown ≥ 3, Retry
  online/offline; Resync Running/Failed → card states; D3 rule
  (1, 2, 3 sessions).
- Compose UI: couldn't-save banner is a live region with both actions.

## Out of scope
Global offline indicator (Figma: none), skeletons, server-down message,
cases locked elsewhere (search empty, Progress empty, sign in/out
offline), keep-as-guest on delete.

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

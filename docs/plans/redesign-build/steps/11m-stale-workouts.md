# 11m Feedback: F25 stale workouts

## Problem
F25: a workout left running ran ~22 h. Home shows "22 h 53 m" and
template estimates read "about 1360 min". There is no end limit, and
the estimate is a plain average, so one outlier ruins it.

Owner decision (device-feedback.md F25): no set for 2 h = forgotten.
Ask on open; auto-finish after 24 h at the last set; estimates skip
these sessions.

## Change
1. Draft: `ActiveWorkoutDraft.Set` gets `doneAt: Long? = null` (epoch
   ms) and `doneAtElapsed: Long? = null` (seconds into the workout),
   set when a set is completed, cleared when un-done. JSON draft, local
   only: defaults keep old drafts loading. Not Room, not synced.
2. Domain rule (pure, `domain/usecase/workout/StaleWorkout.kt`):
   - last activity = latest `doneAt`; no done sets → `startedAt`.
   - `Stale` when now − last activity ≥ 2 h; `Expired` at ≥ 24 h.
   - Finish at last set: `endedAt` = last `doneAt`, `durationSeconds`
     = its `doneAtElapsed`. No done sets → nothing to finish (discard).
3. Active workout, on open while stale: dialog "Still training? Your
   last set was at 18:42 yesterday." Finish at last set (primary,
   saves like Finish with the end above) / Keep going (resets nothing,
   asks again 2 h after the next set) / Discard (only when no sets are
   done). Same check when the app returns to the foreground.
4. Expired on app start (draft repo checked in MainViewModel or the
   existing resume path): finish at last set without asking (or drop
   the draft if no sets), then snackbar "Workout from <day> finished at
   your last set." with View.
5. Notification: `WorkoutService` schedules an update at last activity
   + 2 h; the notice text becomes "Workout still running? Tap to
   finish"; tap opens the workout, which shows the dialog.
6. Estimate (`TemplateStats.averageMinutes`): median instead of mean,
   and sessions over 4 h are ignored. **Deviation from owner note**:
   "skip auto-finished sessions" would need a stored flag (Room +
   api/openapi.yaml + backend). Auto-finish already ends at the last
   set, so those sessions are no longer outliers; the median + 4 h cap
   handles old ones like the 22 h session. Owner to confirm.

No api/openapi.yaml, Room schema, sync or backend change.

The existing 22 h session: Edit workout (09b) already edits the end
time (duration = end − start). Owner fixes it there; no migration.

## Figma
No frame. Ask owner to waive (device feel wins, like 11f/11i/11k/11l).
Dialog uses the existing `AppDialog` style.

## Out of scope
- A stored "auto-finished" flag or any sync/API change.
- Home week total ("22 h 53 m") rules beyond what fixing data gives.
- Changing how the elapsed timer pauses.

## Done when
- Unit test: StaleWorkout (no sets → startedAt, 2 h / 24 h edges,
  finish end and duration from the last done set, un-done set ignored);
  averageMinutes (median, > 4 h ignored, null when none left); old draft
  JSON without `doneAt` loads.
- Unit test (VM): stale draft shows the dialog; Finish at last set saves
  endedAt = last doneAt; Discard hidden with done sets; expired draft
  auto-finishes and emits the snackbar.
- `./gradlew testDebugUnitTest` and `assembleDebug` green.
- Phone via adb: fake a stale draft (debug clock or edit draft time),
  see dialog, notification text, auto-finish snackbar; template estimate
  sane.
- F25 ticked in device-feedback.md with the commit hash; progress.md
  line points to this file.

## As built (owner "go" on the revised plan)
The R17-E2 Home "Unfinished workout" card already existed; it was reused
instead of a new dialog, and the workout screen got an `AppDialog` with
the same choice. Activity also counts a Resume (`resumedAt`). The check
runs on each timer tick, on ON_RESUME and on restore (no WorkoutService
schedule). The auto-finish snackbar has no View action. See
progress.md Deviations.

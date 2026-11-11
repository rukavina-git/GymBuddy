# 11k Feedback: F13 delete account screen

## Problem
F13: Settings → Delete account is two taps (row, then the dialog's
red button) for something you can't undo. Round C decided: a full
screen that lists what is lost, typing "delete" (localised) enables
the button, then the existing password / Google re-auth.

## Change
1. New route `NavRoutes.DeleteAccount` in MainScreen's bottom nav
   graph (back bar, title "Delete account"). The Settings row opens it
   instead of `YouSheet.Delete`. Shares `YouViewModel` (hiltViewModel
   scoped to the You entry, or the screen's own; pick the one that
   keeps `loadDeletionWarning()` and deletion state in one place).
2. `ui/you/DeleteAccountScreen.kt`:
   - Heading "This can't be undone" + body: account, its backup and
     everything on this phone are deleted for good.
   - What is lost, as rows from `LocalDataCounts` (workouts,
     templates, custom exercises, body weight entries; rows with 0
     hidden). Empty counts: one line "Your account and its backup".
   - Unsynced warning ("Not backed up yet: …") when there is any.
   - `AppTextField` "Type delete to confirm". Match is trimmed and
     case-insensitive against `R.string.you_delete_word` ("delete").
   - Red `Delete account` button, disabled until the word matches and
     counts have loaded. Below it: "You'll confirm with Google / your
     password next."
3. Tap → Google re-auth (Google-only) or the existing password dialog,
   then `DeletionProgressDialogs` as today. Success → Welcome as today.
   Cancel/back leaves the screen with nothing deleted.
4. Delete the old confirm dialog path (`YouSheet.Delete`, the confirm
   half of `DeleteAccountFlow`, `you_delete_title/body*` strings if
   unused). Keep the password dialog and progress dialogs.
5. Also delete the unused strings 11f left: summary_time,
   summary_lifted, session_km, session_mi, session_distance.

No api/openapi.yaml, Room, sync or domain change.

## Figma
Owner waived Figma for this step (device feel wins, like 11f/11i).

## Out of scope
- Guest erase (keeps its dialog; it is local only and Undo-less, but
  F13 named only account delete).
- Keep-my-workouts option (decision 4 still stands: always wipes).

## Done when
- Unit test: confirm word match (trim, case, localised word) and the
  button's enabled state (word + counts loaded).
- Compose test: button disabled until "delete" is typed; enabled after;
  back leaves without calling delete.
- `./gradlew testDebugUnitTest` and `assembleDebug` green.
- Phone via adb (test account): Settings → Delete account opens the
  screen, counts show, typing enables the button, re-auth runs and the
  app lands on Welcome. Use a throwaway account.
- F13 ticked in device-feedback.md with the commit hash; progress.md
  line points to this file.

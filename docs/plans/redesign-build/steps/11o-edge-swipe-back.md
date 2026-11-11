# 11o Feedback: F5 edge swipe back

## Problem
F5: owner wants an iOS-style swipe from the left edge to go back on
most screens, with the same rules as Back (ask first if leaving loses
data).

Today:
- Gesture navigation on: the system edge swipe already is Back.
  Predictive back is on (`enableOnBackInvokedCallback`, Navigation
  2.9 scrubs the shared-axis transitions).
- 3-button navigation (owner's phone, `navigation_mode=0`): no edge
  swipe at all.
- Data-loss guards are `BackHandler`s: TemplateEditor and SessionEdit
  (`requestClose` → discard dialog), CreateAccount and Onboarding
  (step back), ActiveWorkout (collapse), WorkoutSummary (close).

## Change
1. `ui/navigation/EdgeSwipeBack.kt`: a modifier/wrapper on the NavHost
   content that, only when the system has no back gesture, catches a
   horizontal drag starting in the left 24 dp. Past ~35% width or a
   fling → `OnBackPressedDispatcher.onBackPressed()`; else snaps back.
   Going through the dispatcher means every existing `BackHandler`
   (discard dialogs, step back) runs exactly as for the Back button.
2. Gesture nav detection: `WindowInsets.systemGestures` left inset > 0
   (no Settings read). Gesture nav on → wrapper does nothing, so it
   never fights the system gesture.
3. Off on screens where a left drag means something else or Back
   isn't wanted: tab roots (nothing to pop), active workout set rows
   (swipe to delete/undo), wheels/keypad sheets. Opt-out via a
   `LocalEdgeSwipeEnabled` flag the screen sets to false.
4. Feedback: page follows the finger (translationX + slight dim of the
   edge), haptic tick on commit (`Haptic` from theme). No predictive
   preview of the screen below; that needs the system gesture.

No api/openapi.yaml, Room schema, sync or backend change.

## Figma
No frame; interaction only, no new visuals. Ask owner to waive (like
11f–11n).

## Open question for owner
Progress line says "only with gesture nav". Read here as: with gesture
nav the system already does it; add the in-app swipe only for 3-button
nav. If you meant "skip it unless gesture nav", this step shrinks to a
check of predictive back + guards on all screens.

## Out of scope
- Predictive-back preview for the in-app swipe.
- Right-edge swipe, swipe-forward.
- Changing any discard/confirm rules.

## Done when
- Unit test: commit threshold logic (distance %, fling velocity, start
  outside edge ignored) as a pure function.
- Compose UI test: edge drag on a pushed screen pops it; on
  TemplateEditor with edits it shows the discard dialog; with gesture
  insets present nothing happens.
- `./gradlew testDebugUnitTest` and `assembleDebug` green.
- Phone via adb (3-button): swipe back from exercise page, session
  detail, settings; editor with changes asks first; set-row swipe in
  the workout still deletes.
- F5 ticked in device-feedback.md with the commit hash; progress.md
  line points to this file.

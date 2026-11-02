# Motion and haptics

Step 07 (2026-11-02). Sources: design-brief.md round logs (Round 5
"set lands", App frame, Templates, Session detail, 06f), motion
tokens in `design/tokens.json`, components in `design/components.md`.
Reviewed by impeccable `animate` (native Android); owner picked the
four options marked (owner). Plan 4 builds from this file.

Token shorthand: `medium2` = `motion.duration.medium2`, `std` =
`easing.standard`, `emph` = `easing.emphasized`, `-dec` / `-acc` =
decelerate / accelerate.

## Rules
1. Motion explains a change of state; it never decorates. No bounce,
   no overshoot, no confetti, no count-up numbers, no looping idle
   animation.
2. Nothing blocks input. A new tap during an animation wins; the
   animation snaps to its end state.
3. The workout screen stays calm: only the set you touch moves.
   Time passing (rest countdown, rest over, workout clock) never
   animates beyond the digits changing.
4. Android "Remove animations" (owner): instant cut. Compose scales
   every duration by `ANIMATOR_DURATION_SCALE`, so at 0 all motion
   ends at once with no extra code; partial scales (0.5×) apply too.
   State that carries meaning (check icon, colours, text) still
   shows. Haptics stay. design-brief.md said "crossfade"; fixed in step 08.
5. Haptics only answer a user action. The one exception is the rest
   buzz, opt-in, off by default.
6. Haptics follow the phone's touch-feedback setting: use
   `View.performHapticFeedback`. Only the rest buzz uses `Vibrator`.
7. Press shrink (owner): buttons and keys only (Button/*, IconButton,
   StepperKey, KeypadKey). Rows, cards and chips get the M3 ripple
   only.

## Inventory
| # | Moment | Trigger | What moves | Tokens | Compose |
|---|---|---|---|---|---|
| 1 | Press | Button, IconButton, StepperKey, KeypadKey | Scale to `press-scale` (0.97); ripple stays | in `short2` `std`, out `short3` `std` | custom `Modifier` on the interaction source |
| 2 | The set lands | Done on the set card | Card shrinks into its SetRow, check stamps, lime sweep across the row, next row grows into the card, RestLine fades in, label digit ("Set 3" → "4") rolls | `set-lands` total, `emph`; stamp `short3` | `LookaheadScope` + `Modifier.animateBounds`, `AnimatedContent` for the digit (same screen; no `SharedTransitionLayout`) |
| 3 | Odometer digits | − / + tap | Changed digits roll up (+) or down (−) | `short4` `std` | `AnimatedContent` per digit, slide Y |
| 3b | Hold to repeat | − / + held | Digits change without rolling (rolls would overlap) | – | plain `Text` while held |
| 4 | Limit reached | − / + at floor or ceiling | Key turns disabled; no shake | – | `enabled = false` |
| 5 | Keypad sheet | Tap a number | Sheet slides up; value panel gets the lime line | in `medium4` `emph-dec`, out `short4` `emph-acc` | `ModalBottomSheet` |
| 6 | Keypad Next | Next key | Label + value swap to the next field | `short4` `std` | `AnimatedContent` fade |
| 7 | Swipe a set | Swipe right (done) / left (delete) | Row follows the finger; on release past threshold: done runs #2, delete collapses the row | collapse `medium1` `std` | `SwipeToDismissBox` |
| 8 | Snackbars | Undo after delete/hide; "Workout restored" | Slides up; Undo grows the row back | in `medium1`, out `short4` | `SnackbarHost`; `animateItem()` |
| 9 | Between exercises | Exercise tabs, sheet jump | Shared axis X, direction follows order | `medium2` `emph` | `AnimatedContent` slide X + fade |
| 10 | Leave workout (⌄) | ⌄ in the workout top bar | Workout shrinks into the LiveBar (container transform) | `long2` `emph` | `SharedTransitionLayout` bounds |
| 11 | Open from LiveBar | LiveBar tap | Reverse of #10 | `long2` `emph` | same |
| 12 | LiveBar leaves | Finish or discard | Slides down behind the nav bar | `medium3` `emph-acc` | `AnimatedVisibility` |
| 13 | Rest countdown / over | Time | Digits change only; "+0:45" same colour | – | plain `Text` |
| 14 | Tab switch | NavBar | Fade-through | `medium2` | Navigation `fadeIn/fadeOut` |
| 15 | Push / pop screen | Row tap, Back | Shared axis X; predictive back follows the gesture | `medium2` `emph` | Navigation transitions + `PredictiveBackHandler` |
| 16 | Large title collapses | Scroll | Title shrinks into the top bar | scroll-linked | `LargeTopAppBar` + `exitUntilCollapsedScrollBehavior` |
| 17 | Sheets and dialogs | Any | M3 defaults | sheet in `medium4`, out `short4` | `ModalBottomSheet`, `AlertDialog` |
| 18 | Row opens in place | Session detail exercise row | Row grows to show sets | `medium1` `std` | `animateContentSize` |
| 19 | Brief outline | Added exercises; row opened from history | Cobalt outline, holds 1 s, fades out | fade `medium2` | `animateColorAsState` |
| 20 | Reorder | Drag handle | Lifted card follows the finger; others slide aside | `medium1` `std` | lazy list `animateItem()` |
| 21 | Chip / switch / segment | Tap | M3 default state change | M3 | M3 components |
| 22 | Onboarding steps | Option tap, Back | Shared axis X | `medium2` `emph` | `AnimatedContent` |
| 23 | Banners, Home cards | Couldn't save, backup stuck, unfinished workout | Grow their space (never slide over content) | `medium1` `std` | `AnimatedVisibility(expandVertically)` |
| 24 | Backing up line | Tap "Not backed up · Retry" | Text crossfades to "Backing up…"; line collapses when done | `short4`, collapse `medium1` | `AnimatedContent` |
| 25 | Sign-in field error | Wrong email / password | Error text fades in under the field; no shake | `short4` | `TextField` supportingText |
| 26 | Chart drag | Press-and-drag | Tooltip follows the finger; line doesn't animate | – | custom |
| 27 | Re-download (410) | Sync | "Getting your workouts…" crossfades to the list | `short4` | `Crossfade` |

## Haptics map
minSdk is 28. Constants are `HapticFeedbackConstants`; ones newer
than 28 need the fallback.

| Moment | Constant | Fallback |
|---|---|---|
| Done on a set (tap) | `CONFIRM` (30) | `VIRTUAL_KEY` |
| Swipe crosses threshold (either way) | `GESTURE_THRESHOLD_ACTIVATE` (34) | `CONTEXT_CLICK` |
| Swipe released past threshold, done | `CONFIRM` (30) | `VIRTUAL_KEY` |
| Set couldn't save (banner appears after Done) | `REJECT` (30) | `LONG_PRESS` |
| − / + press (owner: first tick only, none per repeat) | `CLOCK_TICK` | – |
| A hold hits the floor or the ceiling (once) | `REJECT` (30) | none |
| Keypad key | `KEYBOARD_TAP` | – |
| Keypad Next on a typeable-but-invalid value (0 reps) | `REJECT` (30) | none |
| Long-press a set; lift a drag handle | `LONG_PRESS` (`DRAG_START` 34) | `LONG_PRESS` |
| Finish workout confirmed | `CONFIRM` (30) | `VIRTUAL_KEY` |
| Switch on / off | `TOGGLE_ON` / `TOGGLE_OFF` (34) | `CLOCK_TICK` |
| Rest buzz (opt-in) | `Vibrator`, waveform 80 on · 120 off · 80 on, once at rest target | – |

Rest buzz details:
- Follows the phone (owner): buzzes in sound and vibrate modes, not
  in silent or Do Not Disturb (`USAGE_NOTIFICATION` attributes; no
  alarm bypass).
- With the app in the background it fires from the workout
  foreground service, so it also works on the lock screen.

No haptic on: taps on disabled keys, tab switch, scroll, navigation,
sheets, snackbars, rest over (unless buzz is on), chart drag, sync
events, lock-screen notification buttons (system handles them).

## Figma
"01 System" → Motion section (last, after Components): the "set lands"
strip (0 ms, ~260 ms, 520 ms, built from SetRow / SetCard / RestLine
instances), the rules and the haptics summary. Motion tokens stay on
the Tokens card. Locked frames have no
prototype animation.

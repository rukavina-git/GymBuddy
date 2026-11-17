# 04 Refinement batch 1 (10.1a)

No Figma frame: fixes to existing frames (device feel wins).

## Change
1. Workout summary: the "Keep this safe?" card hides once the user
   signs in from it (ActiveWorkoutViewModel follows ownerFlow).
2. 16:9 phones (window < 720dp tall): the active workout drops the
   Up next row so the set card is not clipped; tabs show what's next.
3. Found on device: the injected Clock was UTC, so Home / Workouts /
   template page showed start times and "today" in UTC (an hour early
   in CET). Provider now Clock.systemDefaultZone().

## Done when
Unit + UI tests (new: backup card hides on sign-in, Up next on tall /
short screens), lint clean, checked on the phone at 1080x1920 and
native size.

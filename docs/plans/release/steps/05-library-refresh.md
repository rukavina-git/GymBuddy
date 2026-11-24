# 05 Default library and plans refresh (0.2.2)

No Figma frame for 05a/05b (data only). 05c touches onboarding copy:
link the onboarding frame before starting it.

## Why
Council review 2026-11-18 (4 independent reviewers: strength coach,
physio/beginner coach, gym-floor trainer, product lead) of the 60
exercises and 10 live plans. Base is balanced (push = pull, sound
sets/reps/rest), but:
- no cardio, though onboarding offers "lose weight" and "endurance";
- beginners get barbell squat/bench/row on day one;
- Pull-Up 3x8 / 4x6 as the default vertical pull fails most users;
- Upper/Lower has 0 weekly sets for side and rear delts;
- Lower B: ADVANCED Hanging Leg Raise on deadlift day, calves 3 sets;
- onboarding picks the plan from days/week only; experience and goal
  change nothing (a 4-day beginner gets Conventional Deadlift).
Rejected: product lead's "cut v1 to 40" (the 60 are live in 0.2.0, so
cuts are deprecations that save nothing; image order solves cost).

One update (owner, 2026-11-18): 05a-05c ship together in 0.2.2,
built as three commits in this order.

## 05a Library 60 -> 64
Deprecate 5 (stay in sync data, hidden from pickers):
- Tricep Dip (merge into Chest Dip, renamed "Dip")
- Upright Row, Concentration Curl, Dumbbell Front Raise, Bicycle Crunch
Add 9 (client UUIDv7 ids, full description/instructions/tips):
- Treadmill (DISTANCE + time), Stationary Bike (DURATION),
  Stair Climber (DURATION)
- Machine Chest Press, Assisted Pull-Up, Dumbbell Romanian Deadlift,
  Hip Abduction Machine, Seated Leg Curl, Glute Bridge
Fix:
- Mountain Climber -> DURATION
- Bodyweight moves consistent: Push-Up, Pull-Up, Chin-Up, Dip,
  Reverse Lunge: pick one rule (weight optional) and apply it to all
- Relabel Dumbbell Chest Fly and Hack Squat BEGINNER -> INTERMEDIATE
- Search: "glute bridge" finds Hip Thrust, "rope" finds Tricep
  Pushdown, "cable fly" finds Cable Crossover (aliases, not renames)
Check first: does the tracking type support distance + time together
for Treadmill? If not, DISTANCE only.

## 05b Plans
Same pattern as Plan 6: deprecate changed templates, add new ids, so
users' history and copies stay intact (confirm against the seeder).
- Full Body A/B/C beginner-friendly: Goblet Squat, Dumbbell Bench
  Press, Dumbbell Row, Dumbbell RDL, seated Dumbbell Shoulder Press,
  Reverse Lunge for Walking Lunge, Glute Bridge for Hip Thrust (keep
  Hip Thrust in Lower B). Keep about 6 chest / 9 back / 8 quad sets a
  week (coach's numbers).
- Upper B and Pull: Lat Pulldown default; Pull-Up stays reachable as a
  swap (Assisted Pull-Up as the beginner swap).
- Upper A + Dumbbell Lateral Raise 3x12; Upper B + Face Pull 3x15.
- Lower B: Hanging Leg Raise -> Cable Crunch 3x12; + Standing Calf
  Raise 3x12.
- Pull + Lateral Raise 2x15. Legs: Leg Press 3x10 (session ~70 min).

## 05c Onboarding mapping
- "New to lifting" -> Full Body whatever the day count; no ADVANCED
  lifts in a beginner's plan.
- "Not sure" -> Full Body, said in the copy. 5-7 days: say "repeat the
  cycle".
- Goal: decide with owner: change reps only, or add an optional cardio
  finisher for lose weight / endurance, or drop the question.

## Each sub-step
- Android assets bump version (default_exercises.json,
  default_workout_templates.json); backend Flyway V9+ via the seed
  generator tools (never hand-edit generated SQL); OpenApiSpecDriftTest
  green. No openapi change expected: say so explicitly if one appears.
- Sync: deprecated rows still sync; existing user sessions untouched.
- Device check on the qa build: pickers, a plan start, onboarding.

## Images (owner, local Qwen-Image-2512, prompts in design/exercise-images)
Done: 0 (Gemini set removed 2026-11-22; see 01b status).
Order: exercises in the live plans after 05b first (about 34), most
used first, each prompt of 4 grouped by equipment/setup; then the
rest. Skip deprecated ones. An exercise without an image shows the
existing fallback. Prompts for the 9 new exercises get added in 05a.

## Later (not this step)
Dumbbell-only home plan, glute-focused lower day, Rowing Machine,
Pec Deck, Smith Machine Squat, Kettlebell Swing.

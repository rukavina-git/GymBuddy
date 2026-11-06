# 07 Exercise page + create

## Problem
The Exercises tab is v1: `ExerciseScreen` (438 lines, list + filter
sheet), `ExerciseDetailsScreen` (432), `ExerciseCreationBottomSheet`
(153) and `ExerciseWizardDialog` (632) with `ExerciseViewModel` (375)
and the `components/ExerciseForm*` files. Roadmap 7.7.
No Room schema or spec change expected: `user_exercise_state` already
has `note` and `defaultRestSeconds`; only DAO/repository queries are added.

Figma (03 Locked designs):
- Exercise page + create https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=97-14553
  (R13-1 Page A 97:14556, R13-4 Create A 99:14836, R13-6 Create C
  quick sheet 99:15034, R13-7 own exercise 99:15135, R13-8 never done
  99:15201, R13-9 menus 99:15253, R13-10 rest sheet 99:15313, R13-11
  delete own 106:15415, R13-12 editor deleted row 106:15479, R13-13
  workout one note 106:15565, R13-14 your note opened 106:15659; notes
  99:15399, 99:15400, owner decisions 106:15762).
- App frame https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=85-9890
  (Exercises list 85:10376, page with photo 85:10656).

## Owner decisions (2026-11-06): 2–6 as recommended
1. **Split**: no, one step (owner).
2. **Progress block** (chart + "Show on Progress" switch, from R7-5):
   no chart component exists; 7.8 builds it. Recommended: 7.7 leaves
   the block out, 7.8 adds it to the page.
3. **History links**: rows open the session (7.9), "All N sessions"
   opens All sessions filtered (7.8). Recommended: rows not tappable
   and no "All N sessions" link until those steps (no dead links).
4. **Photo**: bundled exercises have `thumbnailUrl` on free-exercise-db
   (Unlicense, public domain). Recommended: load with Coil (already a
   dep), no photo block offline/failed (name stays on top like R13-7).
   Record the source in DESIGN.md §licences.
5. **Hidden defaults** (difficulty, category, compound/isolation):
   Recommended: Create sets BEGINNER / from main muscle / COMPOUND, all
   editable under More details.
6. **Delete own exercise**: Recommended: tombstone the exercise only;
   templates keep their row (snapshot name already stamped), editor
   shows R13-12 Swap / Remove / Restore. Restore = undelete via a new
   repository call (update with `deletedAt = null`, new revision).

## Change: list + page
- `ui/exercises/ExercisesScreen` + VM (tab root, 85:10376): large title,
  "+" and search (`TypoMatcher`), muscle chips (same set as picker),
  rows "name · Muscle · Equipment". Hidden exercises left out.
- `ui/exercises/page/ExercisePage` + VM (R13-1/7/8): back, ⋯; photo
  (bundled) or name on top; "Muscle · Equipment" (+ "· your exercise");
  Last / Best tiles (heaviest set; time/distance by tracking type);
  quiet Rest row "Rest m:ss · between sets"; Your note or "+ Add note";
  History (3 latest sessions with this exercise, best set each);
  "Progress shows after 3 sessions." line for own exercises (chart itself is 7.8); How to steps, Tips, Muscles and equipment.
  Never done (R13-8): no numbers, no history, nothing empty.
- Rest sheet (R13-10): 1:00…5:00 chips, Other (keypad m:ss), "Use my
  default (2:00)" clears it. Stored in `defaultRestSeconds`; the editor
  and the workout use it when the exercise is added (else 2:00).
- Note sheet: same as the workout note sheet; stored as the user note.
- ⋯ (R13-9): built-in = Add to template, Hide from lists; own = Edit,
  Add to template, Hide, Delete . Add to template
  = pick a template, appends with editor defaults.
- Delete `ExerciseScreen.kt`, `ExerciseDetailsScreen.kt`,
  `ExerciseFilterContent.kt` and the list/detail parts of
  `ExerciseViewModel`.

## Change: create + delete
- Create A (R13-4, own route): × + lime Save (disabled with no name,
  "Give it a name"), Name, What you log (6 tracking chips + example
  line), Main muscle, Equipment (+ More), More details folded (other
  muscles, steps, rest, decision 5 fields). Also used for Edit.
- Create C (R13-6): from search with no match, "Create "<text>"" row;
  sheet with name prefilled, What you log, Main muscle, Create. Returns
  to the list or the picker (adds it as a pick there; closes the 05b
  picker gotcha).
- Delete own (R13-11): dialog names the templates using it, Hide
  instead, Cancel, red Delete. Editor row R13-12.
- Workout (R13-13/14): one note line; plan note wins, your note behind
  the note icon (dot when set), sheet with Edit note. Only your note →
  it shows as the line.
- Delete `ExerciseCreationBottomSheet.kt`, `ExerciseWizardDialog.kt`,
  `ExerciseFormState.kt`, `components/ExerciseForm*`, `ExerciseViewModel`
  and `ExerciseUiState` if unused.

## Tests
- Unit: list VM (search, chips, hidden left out), page VM (Last /
  Best per tracking type, 3 latest history, never-done, own vs
  built-in menu), rest set/clear, note save (blank → null), rest used
  by editor/workout add.
- Unit: create VM (name rule, defaults, tracking example), quick
  create from search text, delete (templates named, tombstone, restore),
  workout note line rules.
- Compose UI: Create C from an empty search lands back with the pick.

## Out of scope
Progress chart + "Show on Progress" (7.8), session detail links (7.9),
All sessions filter (7.8), Hidden exercises screen restyle (7.10),
"Make a copy" of built-ins (owner: no).

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

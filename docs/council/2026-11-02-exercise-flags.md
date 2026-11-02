# Council 2026-11-02: exercise page flags (delete, copy, notes)

## Brief
```
COUNCIL BRIEF: Exercise page + create own exercise, 3 open flags (GymBuddy, native Android, Soft Dark redesign)

Picked by owner (Round 13): exercise page A (one scroll: photo, Last/Best, quiet rest row, your note,
progress chart, history, how-to, tips; ⋯ menu: built-in = Add to template, Hide from lists; own =
Edit, Add to template, Delete), create A (one screen: name, what you log, main muscle, equipment,
"More details" folded), create C quick sheet from search/picker. Rest stays one fixed number.

Q1. Deleting your OWN exercise that one or more templates use:
 a) Confirm dialog naming the templates; delete removes it from those templates
 b) Block delete while templates use it ("Remove it from Abs Day first")
 c) Delete without asking (Undo snackbar); templates keep showing it, marked "deleted"
 d) Confirm dialog; templates keep it (still startable), only lists/search/picker drop it
Q2. "Make a copy" of a BUILT-IN exercise so the user can change it (name, muscles, steps):
 a) Later (v2 list); v1 = hide built-in + create your own
 b) Now: ⋯ → Make a copy, opens create A prefilled, "(copy)" suffix
 c) Never: built-ins are fixed; per-user note + rest cover personal tweaks
Q3. Two notes: exercise note (yours, everywhere) and template note (that exercise in that template):
 a) Keep both; in a workout show template note first, then exercise note
 b) Keep both; in a workout show only one (template note wins if present)
 c) Only the exercise note in the UI (template note field stays unused)
 d) Only the template note in the UI (exercise note stays unused)

Facts: delete is a soft delete (tombstone). Past workouts snapshot the exercise name, so history is
safe in every option. TemplateExercise also stores exerciseName (snapshot), so a template row can
still render a deleted exercise. Copy support exists in data (Exercise.derivedFromId, "not used yet").
Both note fields already exist (UserExerciseStateEntity.note, TemplateExerciseEntity.notes).
Template round (locked) shows the template note under the exercise name in the workout.
Constraints: Plan 3 = design only; no Room schema or api/openapi.yaml change; v1 features frozen
(design of existing data is fine). Owner prefers simple. Lime = pressable, cobalt = selected.
Read: PRODUCT.md, docs/plans/figma-foundation/design-brief.md (Round logs: Templates, App frame).
Repo: /Users/karlo/Dev/GymBuddy. Do not edit files.
```

## Answers
**Design director (impeccable):** VOTE Q1 c (amended), Q2 a, Q3 b. clarify.md: prefer Undo over confirmation when recovery is safe (tombstone makes it safe); android.md: dialogs only for decisions that must interrupt. Amended: templates stay startable, "deleted" mark only in the template editor, never mid-workout. Q2: a third exercise kind (built-in, own, copy) hurts scanability; v1 frozen. Q3: two stacked notes too dense; locked Templates round shows the template note, so one note, template wins. Risk: user thinks their exercise note is gone. Veto Q1 b (dead end; "user control and freedom"), Q1 a close behind (silently edits templates). Idea: quiet "Your note" icon when the template note wins; "Restore" for a deleted exercise in the template editor.

**Product owner:** VOTE Q1 a, Q2 a, Q3 a. Architecture plan §14.2 already lists "Duplicate-and-edit a default exercise" for v2. Q1 a = simplest honest model (gone everywhere except history); d creates ghost exercises, a second state to design and test. Q3: personal notes are v1 scope (§14) and the template note is locked; c/d would cut a feature or reopen a round. Risk: two notes crowd the set rows; keep both muted, one line, tap to expand. Veto Q2 b (breaks freeze), Q3 c/d. Idea: dialog "Delete Cable Crunch? It's in Abs Day and Core. It will be removed from them. Past workouts keep it."

**Android architect:** VOTE Q1 d, Q2 a, Q3 a. TemplateExerciseEntity.exerciseId is a soft reference meant to tolerate deletion; d = one tombstone write, Undo clears deletedAt. a rewrites N templates through the outbox and sync can resurrect rows from an offline device. Q2: copying photo/steps/muscles + "copy of" UI is v2. Q3: both fields exist; exercise note as a quieter second line hides nothing. Risk: startWorkoutFromTemplate (ActiveWorkoutViewModel.kt:136) skips deleted exercises, so today it starts as "Unknown Exercise" with WEIGHT_REPS; Plan 4 must use the template's snapshot name/type. getExerciseById skips deleted rows, so its page won't open. Veto Q1 b (offline-first can't enforce it). Idea: template editor shows a quiet "Deleted exercise" row with Remove / Swap.

**Accessibility:** VOTE Q1 d, Q2 a, Q3 a. An action that reaches other screens needs a real confirm (WCAG 3.3.4); the dialog names templates so TalkBack reads what's affected; templates keep working. Timed snackbars are missed with sweaty hands/TalkBack. Q2: "(copy)" gives near-identical rows, hard to tell apart. Q3: the exercise note often holds setup/safety info; label notes in text ("Plan note" / "Your note"). Risk: at 200% two notes push sets down; cap each at 2 lines + "more". Veto Q1 c. Idea: focus Cancel by default; Delete as red text button, 48dp, apart from Cancel.

**Beginner:** VOTE Q1 d, Q2 c, Q3 b. If deleting quietly broke my Push Day, or I had to fix templates first, I'd panic; one plain dialog is enough. Copy is one more thing to wonder about. Mid-set I read one line; the plan's note should win. Risk: I might miss "my shoulder hurts" note. Veto Q1 b. Idea: a quiet "Your note" link when it's hidden.

**Veteran:** VOTE Q1 a, Q2 a, Q3 a. I delete because I replaced it; name the templates and clean them up in one tap. d leaves ghost rows. Copy: almost never used it in Strong. Q3: my exercise note ("seat 4") is the one I need most; one line each, template note first. Risk: two lines crowd sets; keep them quiet. Veto Q1 c. Idea: "Hide instead" in the delete dialog, and Hide in ⋯ for own exercises too.

## Synthesis
| Member | Q1 | Q2 | Q3 |
|---|---|---|---|
| impeccable | c* (keep in templates, Undo) | a | b |
| PO | a | a | a |
| Architect | d | a | a |
| Accessibility | d | a | a |
| Beginner | d | c | b |
| Veteran | a | a | a |

- Q1: 4 of 6 keep the exercise in templates (d×3 + impeccable's c*); 2 remove it (a). Veto b ×3, c ×2. Recommendation: d. Confirm dialog naming the templates ("Past workouts keep it"), Cancel focused, red Delete apart, plus "Hide instead". Templates keep working; template editor shows a quiet "Deleted exercise" row with Swap / Remove / Restore. Safer for sync (architect). Plan 4 fix: start from the template's snapshot name and type.
- Q2: a ×5. Later; already on the v2 list (§14.2).
- Q3: a ×4, b ×2 (impeccable, beginner). Recommendation: a, both labelled ("Plan note", "Your note"), muted, one line each, tap to expand. impeccable prefers b + a quiet "Your note" icon; owner decides.

Owner (2026-11-02): Q1 d (confirm dialog, templates keep it, "Deleted exercise" row in the editor). Q2 not in v1; doubtful even for v2 (users make their own, mostly just a name); discuss in v2. Q3 b with impeccable: one note in the workout (plan note wins, your note behind an icon); "no loud screen during a workout, only crucial info".

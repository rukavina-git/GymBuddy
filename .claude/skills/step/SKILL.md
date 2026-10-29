---
name: step
description: Run the next unchecked step of the active plan in docs/plans/. Plan, wait for approval, implement, test, review once, record progress.
disable-model-invocation: true
---

Run exactly one step of the active plan. Keep every message short; I read on my phone.

1. Read `docs/plans/ACTIVE` (the plan folder name), then that folder's
   `00-overview.md` and `progress.md`. Pick the first unchecked step.
   Read only that step's file. If none is unchecked, say the plan is
   done and stop.
2. If the step is marked "(me)", list what I need to check and stop.
   If it has no step file yet, write one in the style of the others,
   show it, and stop for approval.
3. Read the code the step names. Then give a plan of at most 5 lines:
   what changes, which files, which tests. If the code contradicts the
   step file, say so. Wait for my "go". Don't edit anything before it.
4. Implement. Stay inside the step's scope and the overview's invariants.
   Say explicitly if you touch api/openapi.yaml or the Room schema.
5. Run the tests from the step's "Done when". Fix failures. Don't go on
   while tests are red.
6. Run the `reviewer` subagent once on the uncommitted diff, passing the
   step file path. Fix BLOCKERs only. List the rest without fixing them.
   No second review round.
7. Update `progress.md`: tick the step and add the commit hash. Under
   Deviations, note anything that differs from the step file. Under
   Gotchas, note anything the next step should know. Be specific; write
   "None" if there is nothing.
8. Commit code and progress.md together on develop (Co-Authored-By line,
   no other attribution), then `git push origin develop`.
9. Report in at most 10 lines: what changed, test result, reviewer
   findings and what you fixed, open items. End with:
   "Run /clear, then /step."

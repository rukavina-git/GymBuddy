---
name: council
description: Convene a council of independent subagents (design director with impeccable, product owner, Android architect, accessibility, beginner and veteran lifter) on an important product or UI decision, then synthesize a recommendation. The owner decides.
disable-model-invocation: true
---

Run a council on the decision in the arguments (or the one under
discussion). Keep the final report short; I read on my phone.

## 1. Frame the question
Write a brief (max 25 lines) that every member gets verbatim:
- The decision, as 1–3 numbered questions.
- 2–5 concrete options per question, labelled (a, b, c…). Always
  include the plain category-standard option.
- Constraints: what is locked, what is out of scope, open facts.
- Files to read: always `PRODUCT.md` and
  `docs/plans/figma-foundation/design-brief.md`; add others only if
  they matter (roadmap, a step file, code paths).
Show nothing to me yet.

## 2. Convene (parallel, isolated)
Spawn all six members in one message as `general-purpose` agents.
Members never see each other's answers. Each prompt = the brief + the
member's role below + the answer format.

1. **Design director (impeccable).** Before answering, read the
   impeccable skill: `SKILL.md` and the references that fit
   (`mode-operate.md`, `android.md`, `critique.md` heuristics,
   `onboard.md`, `animate.md`…) under
   `~/.claude/plugins/cache/impeccable/impeccable/*/skills/impeccable/`.
   Judge as impeccable would: Operate mode, M3 rules, craft floor,
   cognitive load, slop test. Cite the rules you use.
2. **Product owner.** PRODUCT.md principles, v1 scope freeze, roadmap
   (`docs/roadmap.md`), what earns its place, what can wait.
3. **Android architect.** Material 3 navigation and components,
   Compose feasibility, effect on the existing code (`android/`), data
   model and offline-first, build cost.
4. **Accessibility and gym-floor ergonomics.** WCAG AA, TalkBack,
   font scaling, 48dp, one-handed use, sweaty hands, harsh light.
5. **Beginner lifter (persona).** 3 weeks into the gym, follows a
   plan from the internet, unsure about exercises, easily overwhelmed.
   Answer in first person.
6. **Veteran lifter (persona).** 8 years, logs everything, cares about
   speed and progression, has tried Strong and Hevy. First person.

Answer format for every member (max 200 words):
```
VOTE: Q1 <option>, Q2 <option>…
WHY: 2–4 bullets
RISK of my pick: 1 bullet
I'D VETO: an option I find unacceptable, or "none", and why
IDEA: one thing nobody listed, or "none"
```

## 3. Synthesize (you, as chair)
- Vote table per question (member × option).
- Where they agree, where they split and why.
- Vetoes and new ideas worth keeping.
- Your recommendation, with impeccable's view weighted highest on
  UI questions and the product owner's on scope.
- What only the owner can decide.

## 4. Record
Write the minutes to `docs/council/YYYY-MM-DD-<topic>.md`: brief,
each member's answer verbatim, the synthesis. Leave the decision line
as "Owner: pending". After the owner decides, fill it in, and write an
ADR in `docs/adr/` if it changes architecture.

## 5. Report (max 12 lines)
Votes, recommendation, open choices for me. Link the minutes file.

---
name: council
description: Convene a small council of independent subagents (critic, design director with impeccable, product owner, Android architect) on a big, hard-to-reverse product or UI decision, then synthesize a recommendation. The owner decides.
disable-model-invocation: true
---

Run a council on the decision in the arguments (or the one under
discussion). Keep the final report short; I read on my phone.

## 0. Is a council worth it?
Use it only for big decisions that are hard to undo: navigation, data
model, onboarding, a new feature's shape. Not for copy, wording, store
text or small UI tweaks: there my own knowledge wins, and a quick
skeptic check by you is enough. If the decision is small, say so,
give your own view with its weak spots, and stop.

## 1. Frame the question
Write a brief (max 25 lines) that every member gets verbatim:
- The problem first: what user problem this solves and how we know.
- The decision, as 1–3 numbered questions.
- 2–5 concrete options per question, labelled (a, b, c…). Always
  include the plain category-standard option, and always an option
  (z): don't do it, or solve the problem another way.
- Constraints: what is locked, what is out of scope, open facts.
- Files to read: always `PRODUCT.md` and
  `docs/plans/figma-foundation/design-brief.md`; add others only if
  they matter (roadmap, a step file, code paths).
- Neutral: no recommendation from you or me in the brief, no loaded
  wording.
Show nothing to me yet.

## 2. Convene (parallel, isolated)
Spawn all four members in one message as `general-purpose` agents.
Members never see each other's answers. Each prompt = the brief + the
member's role below + the answer format.

1. **Critic.** Your job is to find the weak points, not to be fair.
   Attack the premise first: is this the real problem, is there
   evidence users need it, what will we regret in 3 months? Then the
   options: hidden cost, maintenance, what breaks, what users will
   hate. Name the cheapest alternative that gets most of the value.
   If the idea is truly sound, say so in one line and spend the rest
   on its biggest risk. No nitpicks: only points that should change
   the decision.
2. **Design director (impeccable).** Before answering, read the
   impeccable skill: `SKILL.md` and the references that fit
   (`mode-operate.md`, `android.md`, `critique.md` heuristics,
   `onboard.md`, `animate.md`…) under
   `~/.claude/plugins/cache/impeccable/impeccable/*/skills/impeccable/`.
   Judge as impeccable would: Operate mode, M3 rules, craft floor,
   cognitive load, slop test. Also covers accessibility (WCAG AA,
   TalkBack, font scaling, 48dp, one-handed use). Cite the rules you
   use.
3. **Product owner.** PRODUCT.md principles, v1 scope freeze, roadmap
   (`docs/roadmap.md`), what earns its place, what can wait. Speak for
   both users: a beginner (weeks into the gym, follows a plan, easily
   overwhelmed) and a veteran (years of logging, has used Strong and
   Hevy, wants speed and progression). Use real tester feedback in
   `docs/plans/release/` when it exists; it beats guessing.
4. **Android architect.** Material 3 navigation and components,
   Compose feasibility, effect on the existing code (`android/`), data
   model and offline-first, build and maintenance cost.

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
- The critic's strongest point, quoted verbatim, and your honest
  answer to it. Don't smooth it over. If it holds, say so.
- Vetoes and new ideas worth keeping.
- Your recommendation, with impeccable's view weighted highest on
  UI questions and the product owner's on scope. If (z) wins, say so.
- What only the owner can decide.

## 4. Record
Write the minutes to `docs/council/YYYY-MM-DD-<topic>.md`: brief,
each member's answer verbatim, the synthesis. Leave the decision line
as "Owner: pending". After the owner decides, fill it in, and write an
ADR in `docs/adr/` if it changes architecture.

## 5. Report (max 12 lines)
Votes, the critic's strongest point, recommendation, open choices for
me. Link the minutes file.

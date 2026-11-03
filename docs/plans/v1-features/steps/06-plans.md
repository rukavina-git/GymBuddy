# 06 Plans + default templates

## Problem
The redesign assumes plans: a plan is a set of templates to rotate
through (owner, council 2026-11-02 onboarding). Onboarding suggests one
by days a week (1–3 Full Body, 4 Upper/Lower, 5–7 Push/Pull/Legs, Not
sure yet → Full Body, or Upper/Lower if not new). Home shows the picked
plan's workouts; Workouts lists "Starter plans" (read-only, Make a copy,
Hide this plan, global "Hide starter plans"). Today the model has only
loose templates, and the 13 defaults (Chest, Back, Arms, …) don't form
plans. Roadmap 6.6. **Spec change (said out loud), Flyway migration,
Room schema change.**

## Owner decisions: 1A, 2A, 3A, 4A (2026-11-03)
1. **Who makes plans**
   - **A (recommended): starter plans only.** Plans are reference data
     (server-owned, read-only), like default templates. Locked designs
     show no "my plan" editor. User-made plans are v2.
   - **B: users make plans too.** New synced aggregate (push/pull,
     owner check, tombstones). Bigger step, no locked UI for it.
2. **Where the chosen plan lives**
   - **A (recommended): profile field `activePlanId`.** Synced, so it
     follows the account to a new phone. Null = no plan.
   - **B: device setting.** No spec change for it; lost on a new phone.
3. **"Next workout" in a plan**
   - **A (recommended): computed.** The plan template after the one
     from the newest finished session that used any of its templates;
     wraps; none yet → the first. No stored pointer, nothing to sync.
   - **B: stored pointer.** Lets the user skip ahead, but one more
     synced field that can conflict across devices.
4. **Old defaults**
   - **A (recommended): deprecate all 13** (ADR 0002) and ship 10 new
     ones: Full Body A/B/C, Upper A/B, Lower A/B, Push, Pull, Legs.
     Copies and history keep their references.
   - **B: keep the 13 and add the new ones.** Two overlapping lists.

## Change
- **Spec** (said out loud): schema `StarterPlan` { id (uuid), title,
  description, templateIds (ordered uuids), suggestedFor
  (`TrainingDays` from step 05, nullable), deprecated }.
  `TemplateLibraryResponse.plans` (required array).
  `UserProfile.activePlanId` (uuid, nullable). Spectral lint green.
- **Backend:** `V7__starter_plans.sql`: `starter_plans` +
  `starter_plan_templates` (plan_id, template_id, order_index), new
  templates and 3 plans seeded, old 13 `deprecated = true`, template
  library version 6. Reference endpoint returns plans. Profile column
  `active_plan_id` (no FK, soft reference). Drift test green.
- **Android:** Room v30 (`MIGRATION_29_30`, 30.json, MigrationTestHelper
  test): `starter_plans`, `starter_plan_templates`, profile
  `activePlanId`. Bundled `default_workout_templates.json` v6 with
  `plans`; seeder and the SyncEngine reference refresh write plans with
  templates in one transaction. Domain: `StarterPlan`,
  `suggestPlan(trainingDays, experience)`, `nextInPlan(plan, sessions)`
  (pure, in domain/). `StarterPlanRepository` (observe plans, set
  active plan via the profile + outbox; saves keep the other profile
  fields). GuestMerge keeps the rule from 05 (account profile wins).
- "Hide this plan" = hide its templates through the existing
  `UserTemplateState`; no new overlay.
- Current UI keeps working: deprecated defaults vanish from lists, the
  new ones appear as loose templates. No plan UI (7.4/7.5).

## Tests
- Backend: reference endpoint returns 3 plans in order, old templates
  deprecated; profile push/pull with `activePlanId`; drift test.
- Android: migration 29→30 keeps profiles and templates; seeder loads
  plans on first install; reference refresh replaces plans;
  `suggestPlan` table (4 day options × experience); `nextInPlan`
  (no sessions, wrap, sessions outside the plan ignored, deprecated
  template skipped); profile mapper and push/pull round trip of
  `activePlanId`; setting the plan keeps the other profile fields.

## Out of scope
Plan screens, onboarding setup, Home rows (7.4, 7.5), user-made plans,
weekly schedule (v2), goal-based suggestions (v2), est. minutes.

## Done when
`npm run lint`, backend `./gradlew test`, android `testDebugUnitTest`
and `assembleDebug` green. No manual checks (owner: none until the
new UI).

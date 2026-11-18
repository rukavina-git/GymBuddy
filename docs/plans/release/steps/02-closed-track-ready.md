# 02 Closed track ready

No Figma frame: no UI change.

## Problem
Closed testing has never been set up. Google reviews the first closed
release, and that review (plus any App content gaps it flags) can take
days. Owner order: get the tester path through review now, with no
testers enrolled, so phase 5 only has to add people.

## Change
1. (Claude) Re-check Google's current closed-testing rule for new
   personal accounts (testers, days, opt-in vs. installed). Note the
   answer and source date in progress.md.
2. (owner, Claude gives links) Play Console → Testing → Closed
   testing: use the default "Closed testing - Alpha" track (fastlane
   `alpha`). Countries: same as internal. Testers: one email list
   "gym-tracker-testers" holding only the owner. Feedback URL or
   email on the track: the in-app feedback mail address.
3. (owner) App content: confirm every declaration is complete (privacy
   policy, data safety, ads = no, content rating, target audience,
   account deletion URL). Store listing complete enough for review.
4. (Claude) Run `android-release.yml` via workflow_dispatch on tag
   `v0.2.0` (promote job, owner approves `release` environment):
   AAB 2000 internal → alpha. Then owner sends the closed release
   for review in Publishing overview.
5. (Claude) `docs/release/tester-guide.md`: opt-in link (placeholder
   until the track shows it), install from Play, what to test, how to
   send feedback (in-app feedback, or email), stay opted in 14 days.
   Plain words, short.

No app code change. No openapi change. No Room schema change.

## Done when
- Closed track has the v0.2.0 release (versionCode 2000), sent for
  review; review state in progress.md.
- Opt-in link recorded in progress.md and the tester guide.
- No tester besides the owner on the list.
- Current tester rule written in progress.md with date checked.

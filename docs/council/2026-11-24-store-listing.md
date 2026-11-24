# Council 2026-11-24: Play store listing text (ASO)

5 members, 1 round: ASO keywords, competitor listings, conversion
copywriter, Play policy, target user. Trigger: closed-test report
("description lacks keywords and substantial text"). The repo
description still opened with "Placeholder listing (Plan 8.3)".

## Agreed (5/5)
- Keep the title "Gym Tracker: Workout Log" (24). It covers gym
  tracker, workout log, gym log, workout tracker.
- Lead with offline + no account needed. No top title for "offline
  workout log" uses "offline" (one smaller app, OfflineGym, owns
  "private offline lift log"; our angle is offline first, sync only if
  you want, which it lacks).
- No "free" or "no ads" anywhere (keeps monetization open; banned in
  title by policy). No best/#1/fastest, no competitor names, no
  testimonials, no keyword lists.
- Never claim images, PRs, 1RM, AI. No health result claims.
- No "private"/"never leaves your phone": Crashlytics runs for
  everyone and synced workouts go to our backend. Say crash reports
  exist; Data safety form must match.

## Result (android/fastlane/metadata/android/en-US/)
- Short (76): keyword member's phrasing ("Offline workout log: ...")
  first, since short text shows in search; 3 members preferred "Log
  sets, reps and weight in a few taps. Works offline, no account
  needed." (74) as the alternate.
- Full (~2.4k chars): copywriter draft, trimmed to verified features
  (exercise library, no count; 23 templates + 3 starter plans, rest timer + buzz,
  lock-screen controls, notes, save as template, onboarding plan
  suggestion, Send feedback). "workout log" x2, "offline" x2,
  "workout tracker" x1.

## Follow-ups
- Not uploaded by fastlane (skip_upload_metadata): owner pastes into
  Play Console > Main store listing.
- No exercise count in the listing (owner: the library changes).
- Check keyword rankings in Play Console 2-4 weeks after going public.
- Screenshots: next, after the exercise images ship.

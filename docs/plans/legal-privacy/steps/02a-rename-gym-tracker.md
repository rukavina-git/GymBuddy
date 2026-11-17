# 02a Rename to "Gym Tracker"

Figma: Onboarding + auth https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=114-20520
(Welcome wordmark) and the Home top bar (R11-1). The new wordmark is
mocked there first; the owner picks.

## Problem
"GymBuddy" collides with a live EU mark (EUIPO 018637587 GYMBUDDY,
classes 9/38/41) and UK word marks, plus a near-identical live app
("Workout Log - Gym Buddy", ~74K installs). Council 2026-11-15
(docs/council/2026-11-15-app-name.md). Owner then chose "Gym Tracker":
descriptive, so nobody can own or block it (TMview: only a Chinese
"STUGYM TRACKER" filing). Zero users, so this is the cheapest moment.

## Owner decisions (2026-11-15)
- Launcher name **Gym Tracker**; qa app **Gym Tracker Test**.
- Play title **Gym Tracker: Workout Log**; developer name Rukavina
  (or full name), changeable any time in Play Console.
- Package `com.rukavina.gymbuddy` stays (invisible; renaming = new
  Play + Firebase app). Code names (classes, theme, repo) stay.

## Change (Claude)
1. **One place for the name**: `app_name` holds it; strings that say
   it use `%1$s` (`cd_app_name`, `no_account_body`,
   `you_export_file_sub`, `signin_offline` if it names the app); the
   About feedback subject reads `app_name`. qa overrides only
   `app_name`.
2. **Legal**: `legal/build.py` gets `APP_NAME = "Gym Tracker"` and
   substitutes `{app}` in privacy.md and terms.md. Texts reworded
   where "GymBuddy" was the subject.
3. **Store**: fastlane title + full description.
4. **Wordmark**: new `brand_wordmark` drawing from the Figma pick
   (options to mock: "gym tracker." / "gym." / none). Lime dot stays.
   Splash, Welcome, Home use the same drawable, so one swap. App
   icon: keep the "G" unless the owner picks otherwise in Figma.
5. **Docs**: PRODUCT.md name line; council minutes owner line (GymBuddy
   decision replaced); memory note `app-name` updated.

Export: check ADR 0015 for a format id containing "gymbuddy" before
renaming; the file name changes, a format id only if import still
accepts the old one.

## Owner answers (2026-11-15)
0. Wordmark picked: **A "gym tracker."** (Figma R19, node 255-4821;
   component brand/wordmark 151:106 updated, so all locked frames show
   it). Later (v3/v4 maybe): add the small "by ikr" byline (option D)
   as company branding; same wordmark, one extra line.
1. Wordmark: not "gym.". Mock in Figma: "gym tracker." and "gym
   tracker by ikr." (small byline); owner picks there. "IKR" as a
   brand is undecided, not forced; the developer name can change later.
2. Legal URLs move to `rukavina.app/gym-tracker/privacy` and `/terms`
   (not live yet, free to change). build.py output dir, nginx docs,
   in-app links follow.
3. Everything user-visible that says gymbuddy changes: export file
   `gym-tracker-export-<date>.json`, feedback mail subject, wakelock
   and log tags. Not changed (decided): package
   `com.rukavina.gymbuddy`, Kotlin class/theme names, repo name.

## Tests
- Unit: strings that use `%1$s` format with `app_name` (Robolectric or
  plain resource check), legal build `--check` passes, no "GymBuddy"
  left in user-facing strings (grep test in CI step of the build).
- Device: launcher label, Welcome, Home wordmark, About → feedback
  mail subject. Screenshots.

## Done when
No user-facing "GymBuddy" or "Gym Buddy" is left in the app, store
metadata or legal pages; the new wordmark is on the phone; the name
lives in one string.

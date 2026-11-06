# 10 You + Settings

## Problem
The You tab opens the v1 `SettingsScreen` (952 lines) + `SettingsViewModel`
(470), with `AppPreferencesScreen`, `AboutScreen`, `HiddenExercisesScreen`,
`HiddenTemplatesScreen`, `ProfileScreen` + `ProfileViewModel` and eleven
`ui/profile/edit/Edit*Screen`s behind it. Plan 6 added the data (body
weight 6.4, profile fields + avatarId 6.5, export 6.7) with v1 UI or none.
Roadmap 7.10.

What exists vs Figma:
- Body weight: `BodyWeightRepository` (observe, save, delete), no screen.
- Profile: `experienceLevel`, `avatarId` (server: `^[a-z0-9-]{1,32}$`).
  No animal images in the repo yet.
- Export (JSON), guest erase, sign out with unsynced changes, change
  password, delete account (+ Google / password re-auth) all work in v1.
- Delete account always wipes the phone (`AccountDeletionCoordinator`).
  Figma's default "Keep my workouts · continue as a guest" doesn't exist.
- Prefs (DataStore): units (one setting for weight + distance), rest
  buzz, quote of the day. No global default rest (constant 120 s), weight
  step, week start, keep screen on, show tips again, starter plans toggle.

Figma (03 Locked designs):
- You + Settings https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=110-17469
  (R15-1 guest 110:17472, R15-2 signed in 110:17614, R15-3 profile
  112:20016, R15-4 picture 112:20135, R15-5 body weight 112:20313, R15-6
  log weight 112:20418, R15-7 guest → account 110:18198, R15-8 sign out
  unsynced 112:19622, R15-9 delete account 112:19720, R15-10 guest erase
  112:19826, R15-11 export 110:18444, R15-12 backup failed 110:18524,
  R15-13 sign out offline 112:19917; notes 112:20519; export JSON only
  165:8930).

## Owner decisions (proposed, need "go")
1. **Split**: 10a You page + account/data dialogs + Hidden lists + About;
   10b Profile, Change picture, Body weight + Log weight. Recommended:
   split. Until 10b the card's "Profile" row opens the old ProfileScreen.
2. **Rows with no feature behind them** (weight step, week starts on,
   keep screen on, show tips again, starter plans, global default rest):
   Recommended: leave them out, list as follow-ups. No new prefs here.
3. **Units**: Figma has Weight and Distance rows; the app has one
   setting. Recommended: one "Units" row (kg · km / lb · mi) for now.
4. **Delete account → keep my workouts**: needs re-owning account data
   as guest data (ADR 0014), a data change. Recommended: not in 10;
   show only the erase path (no radio), button "Delete account".
   Keep-as-guest becomes a follow-up with its own ADR note.
5. **Avatars** (10b): 20 animals from Microsoft Fluent Emoji (MIT),
   "Flat" style PNG → WebP in `res/drawable-nodpi/avatar_<id>.webp`,
   ids like `fox`. Licence row in DESIGN.md. Unknown id → letter.
   Recommended as written.
6. **Quote of the day** pref: not in Figma. Recommended: drop the row
   (the pref stays, default on), unless Home no longer shows a quote,
   then delete the pref too.

## Change (10a): You page
- `ui/you/YouScreen` + `YouViewModel` (one page, R15-1/2). Card: guest
  → "saved on this phone only" + Create account (lime) / Sign in;
  signed in → avatar/letter, name, email, backup line ("Backed up · 2 min
  ago" / "3 changes waiting" / "Couldn't back up · Retry" after 24 h,
  R15-12), Profile row.
- Groups: Workout (Units, Rest buzz switch, Workout notification → Android
  settings), Lists (Hidden exercises N, Hidden templates N), Your data
  (Export data; guest: Erase data on this phone), Help (About), Account
  (signed in: Change password, Sign out, Delete account red). Switches
  cobalt.
- Dialogs, M3 style per notes: sign out with unsynced changes (R15-8,
  R15-13 offline line), delete account (R15-9 minus radio, then re-auth),
  guest erase with Export first (R15-10). Export sheet R15-11 (JSON only).
  Guest → create account (R15-7) reuses 04's auth sheet with the counts.
- Hidden exercises / templates: new-style list pages (unhide), same VMs.
  About: plain page (version, licences link) from components.
- Delete `SettingsScreen`, `SettingsComponents`, `AppPreferencesScreen`
  + VM, `AboutScreen`, old Hidden*Screens, `UnsyncedChanges` if unused.
  Move logic kept from `SettingsViewModel` into `YouViewModel`; Sync
  debug stays debug-only (row in About on debug builds).

## Change (10b): Profile, picture, body weight
- `ui/you/profile/ProfileScreen` (R15-3): avatar + "Change picture",
  Name, email line, Training (Body weight latest, Experience), About you
  rows with "—" (height, birth date, gender, activity level, goal, target
  weight) editing in sheets, not separate screens.
- Change picture sheet (R15-4): letter or one of 20 animals; saves
  `avatarId` (null = letter). TalkBack "Fox, selected".
- Body weight page (R15-5): latest, change vs 30 days ago, range chips
  30/90/1y/All, `LiftChart`-style chart, entries newest first (swipe
  delete + Undo), Log weight button. Log sheet (R15-6): − / + 0.1 kg
  (hold repeats), tap number → `Keypad`, date row, Save.
- Delete `ui/profile/**` old screens + `ProfileViewModel`; nav routes
  Edit* go.

## Tests
- Unit (10a): backup line states (synced / waiting / failed > 24 h),
  guest vs account sections, sign-out dialog variants (online/offline),
  hidden counts.
- Unit (10b): change vs 30 days (none, more, less), range filter, log
  ± step + keypad commit, avatar save/clear, About you edits.
- Compose UI: guest card shows Create account; delete account dialog
  confirms then calls re-auth.

## Out of scope
Keep-as-guest on delete (decision 4), new prefs (decision 2), separate
weight/distance units, language picker, own photos (v2), BMI/calories.

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

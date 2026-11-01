# Council 2026-11-02: You tab + Settings

## Brief
```
COUNCIL BRIEF: "You" tab + Settings (GymBuddy, native Android, Soft Dark redesign)
Owner: "use council a lot and very detailed; we accept new ideas and changes in v1; I probably missed things."
So for THIS council, new v1 features are allowed if they earn their place (say the cost).

Today (v1 code, the "before"): Settings has Language, Units, App Preferences (only "quote of the day",
which breaks our no-fake-motivation rule), Export data (not built), Password, Delete account (with
unsynced-changes warning), About, Sync debug, Log out (warns about unsynced changes), Hidden
exercises, Hidden templates. Profile screen edits: name, email, photo, birth date, body weight,
height, gender, fitness goal, activity level, target weight, bio. Stored device prefs: units, quote.
Locked so far: 5th tab "You" (person icon). Guest card on top ("Your workouts are saved on this
phone only", lime Create account, Sign in), never forced. Placeholder list: Units, Starter plans,
Rest buzz, Theme, Export data, About. Owner already wants here: default rest time (also per
exercise now in v1), "Hide starter plans" toggle, hidden exercises/templates. Onboarding asks units
+ experience. Other designed behaviours that may need a switch: keep screen on during a workout,
lock-screen notification, rest buzz (opt-in soft buzz, off by default), Remove animations follows
Android. Light theme comes later via token modes. Sync is offline-first (outbox, unsynced changes).

Q1. Structure of the tab:
 a) One page: account card, then settings sections (category standard)
 b) "You" = account, profile, your data; a gear opens Settings (2 levels)
 c) "You" = a small personal summary (totals, member since) + account; Settings behind a gear
 d) Other (describe)
Q2. Profile: which of the 11 profile fields does v1 show/ask for, and why? (keep / hide / cut each)
Q3. The settings list: give your full ordered list with groups, default values, and what you cut
 from today's list. Mark each item NEW or EXISTING.
Q4. Account and data: sync status (how visible?), guest -> account, sign out with unsynced work,
 delete account, export (v1 or v2?), "this device vs account" wording.
Q5. New ideas: up to 3 things nobody listed that belong in You/Settings in v1, each with cost
 (S/M/L) and why it earns its place.

Constraints: Operate mode, Material 3, WCAG AA, 48dp, font 200%, TalkBack. Lime = pressable only,
cobalt = selected only. No streaks, no fake motivation, no greetings, no hero metrics. Metric
storage, imperial display only. Plan 3 is design; a new feature may need code/schema later, say so.
Read: PRODUCT.md, docs/plans/figma-foundation/design-brief.md (all Round logs, onboarding picks),
docs/architecture-and-execution-plan-v2.md §14 (v1/v2 scope). Code if useful:
android/app/src/main/java/com/rukavina/gymbuddy/ui/settings, ui/profile, domain/model/UserProfile.kt.
Repo /Users/karlo/Dev/GymBuddy. Do not edit files.
```

## Answers (condensed from the members' full replies)
**Design director (impeccable):** Q1 a: one page, grouped, Profile as a pushed screen; b puts a gear in front of ~12 rows; c fails "no hero metrics" (mode-operate: known patterns earn their place; critique.md: 5–7 items per group). Q2: name keep; email read-only; photo hide (Google avatar or initial); experience keep (NEW in Profile); body weight, birth date, gender, height hide; goal, activity level, target weight, bio cut. Q3: account card (sync line, Profile ›); Workout: units, default rest 2:00, rest buzz off, keep screen on (on), lock-screen controls (on); Lists: starter plans, hidden exercises/templates with counts; App: language (Android per-app screen), About; Account: change password (email only), sign out, delete account red last. Cut quote of the day, theme (one-option control), animations switch (Android governs), sync debug (debug builds). Q4: one muted line "Backed up · 2 min ago" / "3 changes waiting" / "Couldn't back up · Retry"; guest→account "Your 14 workouts will be backed up"; sign out dialog "Back up first" / "Sign out anyway"; delete account own screen, lists what goes, offers export, re-auth; wording "On this phone" / "In your account", never local/cloud/sync; export v1 as on-device JSON + CSV (M) or cut the row, no "coming soon". Q5: Android Auto Backup for the Room DB (S, config only); "Week starts on" Mon/Sun from locale (S); smallest weight step 2.5/1.25 kg (S–M). Risk: ~15 rows at 200% font. Veto: asking for unused sensitive fields; quote of the day.

**Product owner:** Q1 a, no gear (a hidden gear is hunting); account actions at the bottom. Q2: name keep (never a greeting), email read-only; photo cut (also avoids ImageStorageUtil bug); birth date, weight, height, gender, goal, activity hide; target weight, bio cut. No Profile screen in v1; name edited on the card. Q3: Workout: default rest 2:00, rest buzz off, keep screen on, lock-screen controls; Units, Language; Lists: show starter plans (link to hidden templates), hidden exercises, hidden templates; Your data; About (privacy policy needed for Plan 4.5), sync debug debug-only. Cut quote, App Preferences screen, theme, animations switch. Q4: sync line on the card only, warning after 24 h failed; guest→account uploads, never wipes (test in Plan 4); sign out tries a flush, then "3 changes aren't backed up yet…" Stay / Sign out; delete account last, red, names what goes; guests get "Delete data on this phone"; wording "This phone" / "Your account". Q5: export JSON/CSV in v1 (M, client-only, §14 amendment); guest erase (S); weight steps 2.5/1.25 kg, 5/2.5 lb (S). Risk: reopening the §14 freeze (R-01). Veto: profile fields in onboarding; "member since"/totals on You.

**Android architect:** Q1 a (one LazyColumn, no second destination). Q2: columns stay (no migration/openapi change). name keep (empty shows nothing, not "Guest User"); email read-only (change needs Firebase re-verify); photo hide (Google avatar; no object storage); birth date, height, gender, activity, target, bio cut (GDPR minimisation); weight hide (v2); goal cut. Experience isn't in the schema: use once for the starter plan, keep device-only (syncing = openapi change). Q3: Units device-only; default rest NEW device DataStore 2:00 (syncing = openapi change; per-exercise rest already synced on the overlay); rest buzz, keep screen on device-only; notification row opens the Android channel settings; hide starter plans device-only; hidden lists synced; Your data: export, change password (email only), sign out, delete account; About; sync debug debug-only. Cut quote (remove its key), Language (app has no translations), theme, animations. Q4: outbox counts exist (pendingByType); "2 min ago" needs a new device timestamp. Guest mode is NOT in the code: record locally, don't push; on Create account re-key and push; on Sign in merge (UUIDv7, no collisions). No Firebase anonymous auth (it would upload while saying "on this phone only"). Sign out: keep today's flow. Delete account: re-auth, online only, then wipe the phone (built); guests get "Erase data on this phone". Q5: local JSON export via system Save dialog (M); guest erase reusing wipeEverythingForLogout (S); "Back up now" retry via SyncRequester (S). Risk: guest→account migration ~M–L, untested. Veto: anonymous auth; synced unused health fields.

**Accessibility:** Q1 b (two short lists are easier at 200% and TalkBack; c drifts to hero metrics). Q2: name optional, email read-only, photo cut (initial), birth date cut, weight hide (kg), height, gender, goal, activity, target, bio cut; "data nothing reads is a privacy liability". Q3: Workout: units (kg/lb, km/mi), default rest 2:00, rest buzz off, keep screen on, lock-screen controls (link to Android); Display: theme (disabled "Light coming later" or hidden), language, hide starter plans, hidden lists; one muted line "Text size and animations follow your phone's settings" linking to Android; haptics follow the system; Account: change password, log out, delete; About; cut quote and sync debug. Q4: sync line icon + text, never colour alone; "On this phone" / "In your account"; sign out: list what's lost, red "Log out and delete", Cancel default in thumb reach; delete account two steps with typed DELETE or re-auth, offer export first; export v1 JSON/CSV via share sheet (scope change). Q5: left-handed mode (M); FLAG_SECURE "Protect the app" (S); haptic strength off/normal (S). Veto: lime or thumb-position destructive actions without a typed/re-auth step; asking gender/birth date.

**Beginner:** Q1 b (calm You; a page of toggles feels like setup homework; totals in week 3 feel like judgement). Q2: name optional, email read-only, photo cut, birth date cut ("scary"), body weight keep optional (for bodyweight exercises), height hide, gender cut, goal hide (implies adaptation), activity cut, target weight cut (anxious), bio cut. Q3: Workout: units, default rest 90 s with a one-line "what rest is", rest buzz off, keep screen on, lock-screen notification; Library: starter plans ("you can bring them back"), hidden exercises/templates with counts; Look: theme only if honest, language; About; cut quote, sync debug. Q4: words not icons ("Backed up 2 min ago"); guest card honest ("If you lose the phone, it's gone"), signing up keeps everything; sign out: "Back up first" (lime) then sign out anyway; delete account red, two steps, say exactly what's deleted; export v1 CSV. Q5: reset tips (S); change experience level (S); restore a backup file (M). Veto: required fields; nagging; vague delete.

**Veteran:** Q1 a (5 changes a year, want them one tap away; c is a hero metric). Q2: name optional, email read-only, photo cut, birth date cut, body weight keep optional single value (weighted dips later), height, gender, goal, activity, target, bio cut. Q3: account card; Workout: default rest 2:00, weight step 2.5 kg/5 lb (1.25/2.5/5), rest buzz off, keep screen on, lock-screen notification; Units: weight kg/lb and distance km/mi split; Library: hide starter plans, hidden lists with counts; App: language, theme hidden; Your data: export; About, sign out, delete account red last; cut quote, password inside the account row (email only), sync debug debug-only. Q4: sync line, amber after 24 h; "Your 84 workouts will be backed up"; sign out offers Sync now first; delete account type-to-confirm, local data stays as guest; export CSV v1, never "cloud". Q5: weight step (S); Strong-compatible CSV export (M); import from Strong/Hevy CSV (L, else v2). Veto: silent loss on sign out; export needing an account; quote of the day under another name.

## Synthesis
| Member | Q1 | Profile beyond name/email | Export v1 |
|---|---|---|---|
| impeccable | a + Profile screen | experience; rest hidden | yes (M) |
| PO | a, no Profile screen | none | yes |
| Architect | a | none (experience device-only) | yes |
| Accessibility | b | none | yes |
| Beginner | b | body weight optional | yes |
| Veteran | a | body weight optional | yes |

Agreed by all: name optional + email read-only; no photo upload; birth date, gender, height, activity, target weight, bio, goal not shown (columns stay); quote of the day cut; sync debug debug-only; default rest, rest buzz (off), keep screen on (on), lock-screen notification, hide starter plans, hidden exercises/templates; sync as one quiet line on the account card; "On this phone" / "In your account"; destructive actions red, last, never lime; export in v1.
Split: Q1 a×4 / b×2. Body weight hide×4 / keep×2. Theme, Language rows. Default rest 2:00×5 / 90 s×1. Delete account: phone wiped (architect, built) vs data stays as guest (veteran).
Fact: guest mode doesn't exist in code (M–L in Plan 4); no Firebase anonymous auth.
Ideas, counted: weight step ×3 (impeccable, PO, veteran); guest erase ×2; export ×6; week starts on; Auto Backup; Back up now; distance unit split; reset tips; change experience; left-handed (M); FLAG_SECURE; haptic strength; restore/import (M/L).

Owner (2026-11-02, partial): guest mode is a v1 must (ADR 0014). Export in v1, versioned so later versions can import it (ADR 0015). Body weight optional now. Default rest 2:00 (may depend on goal/level later). Theme and Language rows wait for v2 (light theme, Croatian). Delete account: offer keep-as-guest or erase. Layout a vs b and profile fields (BMI/calorie plans): mocked in Round 15, pending.

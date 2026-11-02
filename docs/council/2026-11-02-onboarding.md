# Council 2026-11-02: Onboarding, sign-in, account, session expired

8 members (6 standard + growth/retention + privacy/compliance), 2 rounds, after a competitor research pass.

## Research summary
Full notes (not committed, scratchpad): onboarding-research.md. Every gym app checked (Strong, Hevy, Fitbod, Strava, NTC, MyFitnessPal, Boostcamp) requires an account; none has a guest mode or merges data. Duolingo is the try-first model. Top complaint: duplicate empty accounts from the wrong provider or Sign up vs Log in. Android guidance: don't ask for notifications at first launch. Credential Manager: bottom sheet + persistent Google button, authorized accounts first, clearCredentialState() on sign-out.

## Round 1 brief
```
COUNCIL BRIEF (round 1): Onboarding, sign-in, account and session expired (GymBuddy, native Android)
Owner: "think hard, more councilors and iterations are fine, note how big apps do it." New v1 ideas allowed
if they earn their place (say the cost). FIRST read the competitor research:
/private/tmp/claude-501/-Users-karlo-Dev-GymBuddy/c5e54302-69b1-4b4c-a572-8d1013679323/scratchpad/onboarding-research.md

Facts / locked:
- Guest mode is a v1 must (docs/adr/0014-guest-mode.md): full use without an account, nothing uploads until
  an account exists; create account keeps everything; sign in to an existing account MERGES guest data
  (UUIDv7 ids, server wins on conflicts). No competitor has a guest mode.
- Earlier idea-board pick (2026-10-31, may be changed): (1) Welcome: lime "Create account", "Sign in", text
  "Continue without account"; (2) account: Google first, then email, Apple; (3) About you: units from locale
  + experience; (4) starter plan pre-selected from experience. Guests get ONE "Keep this safe? Create an
  account" prompt after their first finished workout, then only in You. Account preferred, never forced.
- Locked You tab (design-brief "Round log: You + Settings"): guest card, profile (picture = letter or 20
  animals; name; body weight; experience; optional About you: height, birth date, gender, activity, goal,
  target weight), body weight tracking, export, sign out / delete account dialogs.
- Home (locked): new user sees the starter plan from onboarding in the 3 rows. Starter plans exist
  (read-only templates, "Hide starter plans").
- Auth today: Firebase; Google via Credential Manager (GetGoogleIdOption) + email/password, account-linking
  dialog. Apple is a Firebase provider (required on iOS later). Roadmap 4.2a: session dies outside the app
  -> Login with "Session expired"; don't wipe unsynced data before re-sign-in as the same uid; a different
  uid goes through the logout wipe (this rule may need revisiting now that guest mode exists).
- Permissions that may be asked: notifications (rest timer / lock-screen workout notification, Android 13+).
- No paywall. No social. No streaks, no fake motivation, no greetings. Lime = pressable, cobalt = selected.

Questions:
Q1 Entry: (a) Welcome: Create account / Sign in / Continue without account (current pick)
   (b) Straight into the app as guest; account offered later (try-first, Duolingo-like)
   (c) 2-3 setup questions first, then the account offer at the end
   (d) Welcome with just two buttons "Get started" (guest + setup) and "I have an account"  (e) other
Q2 What to ask during onboarding vs later vs never, and defaults: units, experience, goal, days per week,
   body weight, name, picture, starter plan, notifications permission. Max how many screens?
Q3 Starter plan: (a) pre-selected from experience, confirm (b) choose from a short list (c) skip, empty Home
   (d) goal + days/week -> recommend one (e) other
Q4 Sign-in methods and order on Android (Google one-tap / Credential Manager, email+password, email code /
   magic link, passkeys, Apple). How to prevent the #1 complaint: duplicate empty accounts / wrong provider.
Q5 Guest -> account nudges: when, how often, where, wording. What exact moment earns the ask?
Q6 Edge cases: session expired (same uid / different uid), signing in on a phone that has guest data, signing
   in to an account that already has data, sign-in fails offline, account deleted elsewhere. Behaviour + copy.
Q7 Up to 3 new ideas with cost (S/M/L).
Constraints: Operate mode, M3, WCAG AA, 48dp, font 200%, TalkBack. Plan 3 = design; code is Plan 4.
Read: PRODUCT.md, docs/plans/figma-foundation/design-brief.md, docs/adr/0014-guest-mode.md,
docs/roadmap-details.md (4.2a). Code if useful: android/app/src/main/java/com/rukavina/gymbuddy/ui/auth.
Repo /Users/karlo/Dev/GymBuddy. Do not edit files.
```

## Round 2 brief
```
COUNCIL ROUND 2: Onboarding. Anonymous synthesis of round 1 (8 members), then 5 contested questions.

Agreed by all 8 (not reopened):
- Q1 (d): Welcome with two buttons, lime "Get started" (setup, then guest use) and "I have an account".
  No "Create account" on screen 1. Account is offered at the moment there is something to save, and in You.
- Onboarding = units (from locale) + experience, then starter plan. Max 2 screens after Welcome.
  Goal, days/week, body weight, name, picture: later. Birth date, gender: never in onboarding.
  Notifications: never in onboarding; ask when the first rest timer starts (or first workout starts).
- Sign-in: Credential Manager sheet (authorized accounts first) + persistent Google button, then email +
  password, Apple last (iOS). No magic link in v1; passkeys v2+. Duplicate prevention: isNewUser guard on the
  sign-in path ("No GymBuddy account for x@… yet. Create one?"), email-already-in-use -> sign in prefilled,
  one account per email + linking dialog, "Last time you used Google (k***@gmail.com)" hint, "Signed in with
  Google" on the You card. Firebase email-enumeration protection blocks "this email uses Google" lookups.
- Guest nudge: inline card (not modal) on the first finished workout's summary, once; then only in You.
- Session expired, same uid: no wipe, no forced Login screen; a non-blocking banner "Sign in again to keep
  backing up"; outbox resumes after sign-in. (Changes roadmap 4.2a.)
- Offline sign-in: stay guest, nothing lost, Retry. Account deleted elsewhere: "Your workouts stay on this
  phone" -> Keep as guest / Erase.
- Never wipe local data without showing counts and asking. No anonymous auth.
Facts raised: pushing account A's rows as account B fails on the server (FORBIDDEN) and the client would drop
them = silent loss; re-keying to a new owner is possible (like guest -> account). A device-local
"dataOwnerUid" is needed. Experience and preset avatar may need new profile fields (openapi change).
Privacy: Play needs a web account-deletion link, Data safety form, privacy policy in-app + public; Art 9
(health data) may need explicit consent when workouts/body weight sync.

Contested (vote again, you may change your mind):
R1. Session expired, then someone signs in with a DIFFERENT account while the old account has unsynced changes:
   (a) Block: "2 changes aren't backed up for k***@. Sign in to that account, or discard them."
   (b) Offer "Add them to this account" (re-key to the new owner) / Erase / Cancel
   (c) Three ways: Sign in to k***@ to back up / Add to this account / Erase
R2. Guest data on this phone, signing in to an account that ALREADY has data:
   (a) Explicit confirm with counts before merging: "Add 5 workouts from this phone to x@…?" Add / Cancel (stay guest)
   (b) Merge automatically; a line above the sign-in buttons says it will happen; a receipt after
       ("Added 5 workouts from this phone. Kept your account's profile.")
R3. Experience + starter plan screen:
   default experience: (a) Beginner (b) Some experience (c) none pre-picked (Skip = beginner)
   plan screen: (d) one pre-selected card + "See other plans" + "Start with no plan"
                (e) all 3 starter plans as cards, best one pre-selected, + "Start with no plan"
   advanced lifters: (f) same as everyone (g) default to no plan, empty Home
R4. Health-data consent at account creation (Art 9):
   (a) Terms + Privacy line AND one unticked required checkbox "Store my workouts and body data on GymBuddy
       servers to back up and sync"
   (b) Terms + Privacy line only (contract basis), consent text inside the policy
   (c) Other (say)
R5. Guest nudges: (a) once only, then You (b) once, plus once more at ~10 workouts, then You
Answer (max 250 words): your vote per R1–R5 with one line why each; anything in "Agreed" you think is wrong.
```

## Round 1 answers (condensed)
- **impeccable:** Q1 d. Units + experience on one screen (default "New to lifting"), plan on screen 2; days/week never asked (plan cards show "3 days a week"); notifications at first rest timer. Q3 a+b (best plan pre-selected, other 2 visible, "Start with no plan"). Sign-in: Credential Manager on "I have an account", Google, email+password, Apple under "More options"; isNewUser dialog; EMAIL_ALREADY_IN_USE → sign in. Nudge: inline card "Back up your workouts / This workout is saved on this phone only. An account keeps it if you lose or change phones." once. Expired same uid: banner, no Login takeover. Ideas: last-used provider hint; isNewUser guard; skip onboarding when Auto Backup restored data. Veto: any full-screen Login gate mid-workout ("the set is sacred").
- **Product owner:** metric = time from first launch to first logged set. Q1 d; units + experience (default "Some experience") + plan; goal/days/body stats never in onboarding. Q3 a + "Start with no plan". isNewUser guard with delete on cancel; last-method hint. Nudge once on the finish screen. Expired other uid: never wipe silently. Risk: profile precedence in merges. Veto: goal/days/body/permission screens in onboarding.
- **Android architect:** Q1 d; max 3 screens. Experience needs a profile field (openapi + Flyway); body weight needs an entity (openapi); preset avatar wants its own field. Sign-in: single getCredential (password + Google authorized accounts) on "I have an account"; decide re-key vs merge from isNewUser; verify email at sign-up (Google silently removes unverified password providers); remember last provider + masked email. Needs device-local dataOwnerUid; 401 → getIdToken(true) to tell expired vs deleted vs offline. Expired same uid: banner, no wipe (differs from 4.2a). Other uid with unsynced changes: block (pushing A's rows as B → FORBIDDEN → silent loss). Splash reads onboardingDone + dataOwnerUid, not FirebaseAuth. Idea: welcome-back check (silent authorized-accounts lookup on first run). Risk: guest mode is L, not M. Veto: auto-wipe, forced Login, autoSelect for guests, anonymous auth, passkeys/magic links in v1.
- **Accessibility:** Q1 d with TalkBack order heading → value line → buttons in the bottom third. Units as a labelled segmented control, experience as 3 radio cards. Email+password: one password field with show/hide, no confirm field (3.3.7), paste + autofill, no CAPTCHA (3.3.8). Errors inline with liveRegion, never snackbars. Notes the v1 code has a confirm-password field, snackbar errors and no autofill. Veto: CAPTCHA, blocked paste, confirm-password, snackbar-only errors, non-scrolling screens at 200%.
- **Beginner:** Q1 d; max 2 screens; "Use my own plan / start empty" as visible as the starter plan. One Google button that works both ways. Ask for backup after the first workout, once. Copy must never say merge/uid/sync. Veto: account before use, notifications on screen 1, unskippable plan.
- **Veteran:** Q1 d (Sign up vs Sign in split is how I made a duplicate on Hevy). Advanced → no plan, empty Home. Show "34 workouts in this account" after sign-in. Ask at first workout and again at ~10. Wants Strong/Hevy CSV import. Risk: server-wins can overwrite an offline edit. Veto: any wipe without counts + asking.
- **Growth:** activation = first finished workout with ≥1 set within 24 h. Measure only server-side aggregates for account holders; no analytics SDK; guests unmeasured by design. Q1 d; experience not pre-picked. One nudge ever. Veto: analytics SDK, notifications in onboarding, more than one nudge.
- **Privacy:** Q1 d. Health data (Art 9) needs explicit consent once it syncs: an unticked checkbox at account creation. Play needs a web account-deletion URL, accurate Data safety form, privacy policy in-app + public, target audience 16+. Firebase email-enumeration protection blocks provider lookups. Risks: Crashlytics non-fatals carry workout payloads (§6.8), exact-alarm and foreground-service rules for the rest timer/lock-screen notification. Veto: anonymous auth, pre-ticked consent, silent merge, birth date/gender in onboarding, shipping without the web deletion link.

## Round 2 votes
| Member | R1 other account | R2 guest → account with data | R3 default / plan screen / advanced | R4 consent | R5 nudges |
|---|---|---|---|---|---|
| impeccable | c | b | Beginner / e / g (pre-select no plan) | a | a |
| PO | c | a | Beginner / e / f | a | a |
| Architect | a (+Erase) | b | Beginner / d / f | a | a |
| Accessibility | c | a | none / d / f | a | a |
| Beginner | c | a | none / e / f | a | a |
| Veteran | c | a | none / d / g | a | a |
| Growth | c | a | none / e / f | a | a |
| Privacy | c | a | none / d / f | a | a |

Round 2 notes: architect says R1(b) "add to this account" is only partly possible (Room has no owner column; edits to A's server rows come back FORBIDDEN; moving them means copying under new ids, M–L) and unsafe on shared phones. Privacy: Art 6(1)(b) does not lift the Art 9 ban; explicit consent (9(2)(a)) is the only realistic basis; consent time + policy version must be provable (architect: openapi change). isNewUser guard: Firebase has already created the user; on decline call currentUser.delete() + clearCredentialState() and don't call /v1/me first. Notifications: impeccable wants a quiet "Get notified when rest ends · Turn on" row in the rest card instead of a system dialog mid-set. Account deleted elsewhere: Keep as guest is the primary button. Avatar: architect prefers its own profile field over "avatar:fox" in profileImageUrl (openapi change, S).

## Synthesis
- Agreed (both rounds): Welcome = "Get started" / "I have an account"; units + experience + starter plan, max 2 screens after Welcome; Google (Credential Manager + button) → email+password → Apple last; no magic link, passkeys later; isNewUser guard with delete-on-decline; email-already-in-use → sign in; last-used provider hint (device only, cleared by Erase); "Signed in with Google" on You; one inline backup card after the first finished workout, never again; notifications asked in context at the first rest; session expired = banner, no wipe, no forced Login (4.2a changes); offline = stay guest; account deleted elsewhere = Keep as guest (primary) / Erase; explicit unticked consent checkbox at account creation (8/8), inline error, not a disabled button.
- R1: 7 want three choices, but the architect shows "Add to this account" is M–L and unsafe. Chair: v1 = "Sign in to k***@ to back up" / "Erase them" / Cancel; moving data between accounts → v2.
- R2: 6–2 for an explicit confirm with counts (GDPR on shared phones), plus the receipt.
- R3: default not pre-picked 5–3; plan screen tie 4–4 (impeccable e); advanced same flow 6–2.
- New openapi changes requested (owner sign-off needed): experience field, avatar field, consent record; body weight entity (already noted).
- Release work for the roadmap: web deletion link, privacy policy, Data safety, 16+ audience, Crashlytics payloads to counts only, rest-timer exact-alarm / foreground-service limits.

Owner (2026-11-02): no Apple sign-in on Android. Account offer: after a finished workout, at most once a week, max 5 times (device-local), then only in You. R1: a different account never gets the previous account's unsynced data; it stays on the phone and syncs when that account signs in again (per-account local data). R2: ask first. R3: show all three plans. R4: experience not pre-picked (Skip = Beginner). openapi changes OK for Plan 4: experience, avatar, consent record, body weight. Session expired: keep logging, outbox waits, push after re-sign-in. Privacy, law and publishing work is planned as one pass at the end of v1 (roadmap 4.6), unless it blocks or reshapes something we build; the consent checkbox is designed now, wording finalised in 4.6.

Owner, after the Round 16 mocks (2026-11-02): no "last time you used…" hint; if the last sign-in used email, the email is filled in and only the password is asked (stored on the phone). No plan step: setup is one screen (units + experience); experience picks which 3 starter workouts Home shows first; all starters stay in Workouts › Starter plans. No "Skip counts as…" line.
Owner, second review: plans stay (a plan = a set of workouts to rotate through; plans and the default templates get reworked before the end of v1). Units are never asked; they come from the region and can be changed in You. Setup = experience, then plan. Sign-in screen is centred with the logo and wordmark above "Welcome back".
Owner, third review: setup asks days a week (2–6) and fitness goal, then suggests a plan by days a week only (2–3 Full Body, 4 Upper/Lower, 5–6 Push/Pull/Legs); goal-based suggestions are v2. Units are not mentioned. Experience stays in Profile. Error/offline messages are designed in the error-states round (the offline box is a placeholder). New logo (an animal, e.g. a duck) before the end of v1.
Owner, fourth review: no Next buttons. Setup is experience → goal → days a week → plan → a ready screen (the plan, its workouts, 3 short tips, "Start first workout" / "Go to Home"). Each option is a button: tapping saves it and moves on; Back keeps the answer; Skip skips the rest with defaults.

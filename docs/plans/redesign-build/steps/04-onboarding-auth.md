# 04 Onboarding + auth

## Problem
Start-up is still v1: `SplashScreen` shows "Loading..." and goes to
`LoginScreen` (Google, email/password, guest link) or Main.
`RegisterScreen` is separate (Figma notes: split sign-up/sign-in makes
duplicate accounts). No onboarding, no consent at sign-up, no forgot
password. The 6.2/6.3 parts are plain M3 placeholders:
`SessionExpiredBanner`, `AccountDeletedDialog`, `GuestMergeDialogs`.
Roadmap 7.4. **Spec change (decision 2), no Room schema change.**
Figma: Onboarding + auth https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=114-20520
(Welcome 114:20523, Experience 118:22191, Goal 118:22275, Days
118:22365, Plan 118:22455, Ready 119:22682, Home after setup 116:21695,
I have an account 116:21852, Account sheet 114:20860, No account
114:20938, Create account 114:21009, Consent missing 114:21094, Add to
account 114:21123, Receipt 114:21194, Session expired 114:21307,
Other account's changes 116:22027, Offline 116:22058, Wrong password
116:21939, Deleted elsewhere 114:21466). Notes text 114:21632.
Specs: DESIGN.md §licences (Google button: Google's own dark style).

## Owner decisions: 1B, 2A, 3A, 4A (2026-11-03)
1. **One step** (not split into 04a/04b).
2. **Goal list:** add `FitnessGoal.GET_STRONGER`; "Stay fit and
   healthy" → GENERAL_FITNESS; MAINTAIN stays for old rows. Stored as
   a string: no Room schema change. **Spec change** (owner approved):
   `api/openapi.yaml` goal enum + backend enum.
3. **"No account found" (Google) in this step.** Credential Manager
   with `filterByAuthorizedAccounts(true)` first, then all accounts;
   `isNewUser` → ask; "Use another account" → `currentUser.delete()`.
4. **Setup only after "Get started"** (fresh install, no owner, not
   guest). Existing guests/accounts skip it. Sign-in goes to Home;
   Ready only after setup (tip 3 by guest/signed-in).

## Change
- `ui/onboarding/`: Welcome (app icon + wordmark, Primary "Get
  started", Outline "I have an account"), setup pager-free screens:
  each option a tappable row (cobalt for a moment, then next), back
  keeps the answer, Skip = defaults, 4-segment progress. Plan list
  from `StarterPlanRepository`, "Suggested" via
  `StarterPlans.suggestPlan`. Ready: title, line, 3 tips, lime
  "Let's go" → Main. Answers saved once at the end into the guest
  profile (`experienceLevel`, `fitnessGoal`, `trainingDays`,
  `activePlanId`) and `setGuest(true)`.
- `OnboardingViewModel` holds answers (SavedStateHandle).
- `SplashScreen`: app icon on ground, no 1 s delay; routes
  Main / Welcome. Android 12 SplashScreen API only if already set up.
- Units from region, never asked (notes): no change, metric default.

- `ui/auth/SignInScreen.kt` replaces `LoginScreen` + `RegisterScreen`
  + `PasswordTextField`: "Welcome back", Google dark button (G logo,
  Google's assets, licence row), "or", one email + password form
  (Show, autofill), Forgot password (`sendPasswordResetEmail`),
  errors under the field, enumeration-safe text, offline banner +
  Retry. Last email-sign-in address stored on the phone (DataStore).
- Create account (from "Back up your workouts", close ×): counts line,
  unticked consent checkbox + "Why?", Google + email; without the tick
  the reason shows under the box. Writes `healthDataConsentVersion/At`.
- Merge ask (13) and Receipt (14) restyled as sheet/banner per Figma;
  `GuestMergeDialogs` restyled in place.
- `SessionExpiredBanner` per 16 (cloud-off icon, "Sign in" action);
  `AccountDeletedDialog` per 20 (Keep as guest primary, Erase with
  counts + confirm). Other account's changes (17) note on sign-in.
- Delete `LoginScreen`, `RegisterScreen`, `LoginUiState`, old routes.

## Tests
- Unit: OnboardingViewModel (tap saves + advances, back keeps answer,
  Skip defaults, suggestion from days/experience, save on Let's go);
  SignIn VM (consent required, error mapping wrong-password/offline,
  last email kept, no-account → ask → delete on cancel).
- Compose UI (Robolectric): Welcome buttons route; option tap moves
  to next screen; consent missing shows reason; banner action.
- Existing auth/guest tests stay green or move with the code.

## Out of scope
Backup card after a workout (R16-11) and lock-screen row (R16-17):
7.6. Error/offline final wording: 7.11. Profile/avatar: 7.10. Apple.
Magic links. Privacy wording (roadmap 4.6).

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual checks.

## Amendment 1: account-first (owner, 2026-11-03, after device check)
Figma: Round 18 + 18b on "02 Rounds"
https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=167-8931
Owner picked option C plus the 18b split. Lock it into "03 Locked
designs" (replacing the affected frames in 114:20520) before code.

Decisions:
- **Welcome:** lime "Create account", outline "I have an account",
  text link "Continue without account" (→ setup as guest).
- **Create account, split:** (1) "Create your account": Continue with
  Google / Continue with email / "Already have an account? Sign in";
  (2) email + password ("At least 8 characters", Show, paste and
  password managers work, **no repeat-password field**); (3) consent
  "Keep a copy on our servers?" with "Agree and create account" and
  "Not now". The tap on Agree is the consent (no checkbox); recorded
  as before (version + time). Terms line is plain text; links in 9.2.
- **Setup step 5:** after the plan question, guests see screen (1)
  with back, a 5-segment progress bar and "Not now". Not shown when
  already signed in. Then "You're all set" (signed-in tip when made).
- **Sign in:** no logo/wordmark, "Welcome back" as left headline
  (32 Bold), Google and email buttons 48dp, fields 56dp, "Forgot
  password?" small muted, "No account yet? Create one" link.
- **Progress:** a spinner replaces the label of the tapped button while
  Firebase answers.

Code: Welcome, `SignInScreen` (sign-in only), new create-account route
with three steps (method / email / consent), OnboardingViewModel gets
step 5 and keeps the answers until the account exists, Settings guest
entries point at the new route. Google on the create path: token →
consent screen → sign-in (no probe needed; probe stays for Sign in).
ADR 0014 gets a short note: guest is still always offered, accounts
are now encouraged first (owner).

Tests: create flow VM (method → email → consent; Not now keeps guest;
consent parked only on Agree; Google token held until Agree), setup
step 5 skipped when signed in, spinner state, UI tests for Welcome
buttons and consent screen. Device check on the owner's phone (owner
asked for it in this step).

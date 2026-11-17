# 04 Compliance pass

No Figma frame: an audit plus small fixes. A text change in the app
(if one is found) reuses existing screens.

## Problem
Before Play review and real accounts on prod (10.3), check what the
app actually does against what the privacy policy and the Play
policies say. Roadmap 9.4 lists health-data consent, 16+, Crashlytics
payloads and timer/notification rules. Step 03 found one more gap:
`FirebaseTokenVerifier` calls `verifyIdToken(token)` without
`checkRevoked`. A phone that is still signed in can push for up to
~1 h after the account is deleted, and that push recreates rows
under the deleted uid.

## Change (Claude)
1. **Health-data consent** (GDPR art. 9 explicit consent). Check in
   code and tests:
   - The consent screen (consent_title/body, "Agree and create
     account") comes before any upload, on every path: new account,
     guest → account, sign in to an account that has no consent
     record yet.
   - Its version and time are stored (`health_data_consent_*`, V6).
   - Withdrawal = delete account, and the policy says so.
   Fix only gaps found. Expected verdict: a separate screen with an
   affirmative button that names health data counts as explicit.
2. **16+.** The policy and terms already say 16+. No age gate in the
   app (see question 2). If the optional birth date in About you is
   under 16, show no special handling, just record the decision.
3. **Crashlytics payloads.** Read both `recordNonFatal` call sites and
   `setUserId`: keys and exception messages must hold no email, name,
   workout or body data. Uid only, as the policy says. Add a unit
   test pinning the allowed key names.
4. **Timer and notifications.**
   - POST_NOTIFICATIONS is asked at the first workout start, never at
     launch.
   - The workout service starts only from the foreground (Android 14
     FGS rules).
   - The lock-screen notification shows no body data.
   - Draft the `specialUse` justification text for the Play Console
     (9.5).
5. **Revoked tokens (backend).** After an account is deleted, its
   tokens stop working (see question 1). Test: delete, then a push
   with the old token gets 401 and creates no rows.
6. **Data safety answers.** Write `docs/plans/legal-privacy/data-safety.md`:
   each Play Data safety question with the answer and the code or
   policy line behind it, ready for the owner to paste in 9.5.

No openapi change (401 is already a documented response). No Room
schema change. No new data collected.

## Open questions (owner)
1. Revoked tokens:
   - (a) `verifyIdToken(token, checkRevoked = true)` on every request
     (recommended: one line, exact). It costs one Firebase lookup per
     request, which is fine at our size.
   - (b) The same, but cache "uid is alive" for 60 s: cheaper, but
     leaves a 60 s window.
   - (c) Keep a table of deleted uids and reject them. That stores the
     uid after deletion, which the delete page doesn't mention.
2. Age: (a) no gate, 16+ in the terms and in the Play target audience
   (recommended: what Strong and Hevy do; Play doesn't require a gate
   outside the Families program); (b) ask for the birth year at
   sign-up.

## Tests
- Backend: the revoked-token test above; existing auth tests stay
  green.
- Android: Crashlytics key test. Consent-path tests, only where a gap
  is fixed.
- Device: the notification permission prompt and lock-screen
  notification on the S731B over adb (Claude).

## Done when
The 5 checks are written up in progress.md, each with a verdict and a
fix or a reason. Deleted accounts' tokens get 401. data-safety.md
exists. Tests and lintQaDebug are green.

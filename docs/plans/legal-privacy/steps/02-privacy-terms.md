# 02 Privacy policy + terms

Figma: Onboarding + auth https://www.figma.com/design/MdD33rDNKJ3S82azTOYfEk/GymBuddy?node-id=114-20520
(Welcome, Create account, consent screen). The Welcome line and the
link styling need a frame there before the Android part starts.

## Problem
Play requires a privacy policy at a public URL and in the app. Today
"Terms" and "Privacy policy" are plain text on Create account
(`create_terms`) and nowhere else. Guests skip account creation, so
they never see either. The health-data consent (`CONSENT_VERSION`
"2026-11-03") points to a policy that doesn't exist yet.

## Change (Claude)
1. **Texts in the repo**: `legal/privacy.md` and `legal/terms.md`,
   with a version date at the top. Plain English, short sections.
   - Privacy: controller and contact; what is stored (account email,
     Firebase uid, profile, workouts, body weight, consent time and
     version; crash reports); guests: nothing leaves the phone; legal
     bases (contract 6(1)(b), explicit consent 9(2)(a) for health
     data, legitimate interest for crash reports); processors with
     DPAs (netcup DE, Google Firebase Auth + Crashlytics, GitHub for
     30-day DB backups); transfers to the US (EU-US DPF / SCCs);
     retention (until deletion, backups age out in 30 days); rights
     (access, export in the app, deletion in the app and on the web
     page from 9.3, withdraw consent = delete the account, complaint
     to the supervisory authority); 16+; changes.
   - Terms: use of the app, accounts, the training disclaimer (not
     medical advice; consult a doctor and a qualified trainer; train
     at your own risk), liability limited but not excluded, governing
     law, changes, contact. Marked "lawyer glance before release".
2. **Publish**: a small script turns both into static HTML (one
   shared CSS, Soft Dark colours, no tracking, no cookies). Hosted
   where the owner picks (open question 1). Stable URLs, e.g.
   `https://rukavina.app/gym-tracker/privacy` and `/terms` (renamed in 02a).
3. **Android**: URLs in one place (`LegalLinks.kt`, same for both
   flavors). "Terms" and "Privacy policy" become tappable links on
   Create account, Sign in, and You → About. Welcome gets the guest
   line "By continuing you agree to the Terms and Privacy policy"
   under "Continue without account". Links open the browser
   (ACTION_VIEW, no new library).
4. **Consent**: final wording of the consent screen, checked against
   the policy. If it changes, bump `CONSENT_VERSION` to the policy
   date.

No openapi change. No Room schema change. No new data collected.

## Open questions (owner)
1. Hosting: (a) nginx on the VPS, deployed by the existing deploy
   workflow (recommended: already ours, certbot in place, 9.3's page
   can live there too); (b) GitHub Pages (already used by the API
   docs, one site per repo, so it would share that site); (c) Vercel.
2. Controller line: your full name and a contact address/email to
   print. Country for governing law and the supervisory authority
   (Croatia → AZOP?).
3. Contact email for privacy requests (a new alias on rukavina.app,
   or your Gmail).

## Tests
- Unit: `LegalLinks` URLs are https and end in `/privacy` and
  `/terms`.
- Script: builds both pages; CI fails if a page is missing its
  version date.
- Device: Welcome, Create account, Sign in, About: each link opens
  the right page. Screenshots.

## Done when
Both pages are live at their URLs, every in-app link opens them on
the phone, consent wording matches the policy, and the terms are
flagged for a lawyer glance before 9.6.

## Implementation notes (2026-11-15)
- Figma R20 (02 Rounds, node 255-5853): owner picked W1 (Welcome line
  under "Continue without account") and C1 (consent), links underlined
  in the muted text colour.
- Deviation: no link on Sign in (no frame shows terms there; existing
  users agreed at creation). Links use Compose `LinkAnnotation.Url`
  (opens the browser through LocalUriHandler) instead of ACTION_VIEW;
  About rows call `openUri` inside runCatching.


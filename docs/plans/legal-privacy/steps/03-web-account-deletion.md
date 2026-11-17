# 03 Web account deletion

No Figma frame: a web page in the existing legal-site style, no app UI.

## Problem
Play's data deletion rule needs a public URL, entered in the Data
safety form, where people can ask for their account and data to be
deleted without the app (for example after uninstalling). The page
must name the app and developer as on the listing, list the steps,
say what is deleted and what is kept, and say for how long.
Today the app can delete the account (You → Delete account →
`DELETE /v1/account` → `AccountDeletionService`: DB rows, then the
Firebase user). The privacy policy says "without the app, ask at
privacy@". That is the only route, and there is no page and no
tested way for the owner to carry out such a request.

## Change (Claude)
1. **Page** `legal/delete-account.md` → gymtracker.rukavina.app/delete-account
   (added to `PAGES` in `legal/build.py`, same CSS, date line):
   - "Gym Tracker by Rukavina" (developer name as on Play).
   - In the app: You → Delete account (exact labels from strings.xml).
   - Without the app: email privacy@rukavina.app **from the email
     address of the account**, subject "Delete my Gym Tracker
     account". We reply to confirm and delete within 30 days (GDPR
     one month), usually much sooner.
   - Deleted: account, profile, workouts, plans, body weight,
     consent record, the Firebase login.
   - Kept: database backups age out within 30 days; crash reports
     (not tied to the account email) expire after 90 days.
   - Guests: nothing is on our servers; uninstalling the app (or
     clearing its data) deletes everything.
2. **Privacy policy**: the "Delete it" line links the page. Terms
   unchanged. The "Last updated" date moves to the change date.
3. **Owner's tool for email requests**: `docker/server/delete-account.sh <uid>`.
   It runs the existing `AccountDeletionService` once, in a short-lived
   container from the prod backend image, behind an
   `account-delete` Spring profile: no web server, then it exits. It
   uses the same code as the in-app delete, so nothing can drift.
   The runbook goes in `docker/README.md`: find the uid by email in
   the Firebase console, run the script, reply to the email.
4. **Android**: no change. The in-app path already exists; the page
   is for people without the app.

No openapi change. No Room schema change. No new data collected.

## Open questions (owner)
1. Deleting for an email request: (a) the one-off runner above
   (recommended: same code as the app, tested); (b) an admin HTTP
   endpoint with a secret (new attack surface on a public server);
   (c) hand-written SQL in the runbook (can drift from the code when
   tables change).
2. Identity check: matching the account's email address is enough
   (recommended; it's what Play and most apps accept). Or also ask for
   a reply from that address before deleting.

## Tests
- Script: `build.py` builds 3 pages; CI fails without the date line.
- Backend: a test that the `account-delete` profile with a uid
  deletes that user's rows, leaves another user's alone, and starts
  no web server.
- Server: owner runs the script once against a throwaway account on
  prod (or test), then signs in again: the account is gone.
- Device: none (no app change).

## Done when
gymtracker.rukavina.app/delete-account is live from main via
`legal-site.yml`, the privacy policy links it, and the owner has
deleted a test account by uid with the script.

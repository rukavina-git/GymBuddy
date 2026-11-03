# 14. Guest mode: the app works fully without an account

Date: 2026-11-02
Status: Accepted (owner, 2026-11-02). Details are refined in Plan 6.

## Context

The redesign (Plan 3) lets people use GymBuddy without an account:
onboarding offers "Continue without account", and the You tab shows a
guest card ("Your workouts are saved on this phone only"). The v1 code
has no guest mode: the outbox and the profile assume a signed-in uid.
The owner made guest mode a v1 requirement (council
docs/council/2026-11-02-you-settings.md).

## Decision

- A guest is local-only. Room stores everything as usual; the outbox
  keeps recording changes but never pushes while there is no account.
- No Firebase anonymous auth. It would upload data while the app says
  "saved on this phone only".
- Create account: the guest profile row is re-keyed to the new uid and
  the whole outbox is pushed. Nothing is wiped. The app says what will
  be backed up ("Your 14 workouts will be backed up").
- Sign in to an existing account: guest data merges into that account.
  Client UUIDv7 ids mean no collisions; server-wins (D-12) still applies
  to rows that exist on both sides.
- Guests get "Erase data on this phone" (reuses the logout wipe).
- Delete account offers a choice: delete everything, or keep the data
  on this phone and continue as a guest.
- Export (ADR 0015) works for guests; it is their only off-phone copy.

## Additions (owner + council, 2026-11-02, docs/council/2026-11-02-onboarding.md)

- Data on the phone belongs to one owner at a time: a guest or one
  account (device-local dataOwnerUid). Sync uses the signed-in
  account's token, and the server refuses writes to rows another
  account owns.
- Session expired (same account): nothing is wiped, no forced Login.
  The user keeps logging; changes wait in the outbox exactly like a
  long offline stretch and push after signing in again as the same
  account. A banner asks to sign in again.
- A different account signs in while the previous account still has
  unsynced changes: those changes are never shown to, merged into, or
  uploaded under the new account. They stay on the phone, kept apart,
  and sync when the original account signs in again. This needs
  per-account local storage (for example one Room database file per
  owner, plus one for the guest); Plan 6 decides the mechanism.
- Guest data into an account that already has data: ask first, with
  counts ("Add 5 workouts from this phone to x@…?"); Cancel keeps the
  guest. A receipt follows. A new account takes the guest data as is.
- Account offer for guests: a card after a finished workout, at most
  once a week and at most 5 times in total (counted on the device),
  then only in You.
- Apple sign-in is not offered on Android (iOS only).

## Consequences

- Plan 6 gains a guest-mode step (roughly M–L), with tests for "guest
  signs in to an account that already has data" and "account deleted,
  kept as guest".
- Screens never assume an account: sync line, sign out and delete
  account only show for signed-in users.
- Android Auto Backup rules should include the Room database so a
  guest's data survives a phone change.

## Amendment (2026-11-03, owner)

Accounts are encouraged first; guest mode stays, as the secondary path.
Welcome leads with "Create account", then "I have an account", then a
"Continue without account" link. Guests who finish setup get one
"Create your account" step with "Not now" before the app opens. Sign in
links to Create account. Health-data consent is its own screen; tapping
"Agree and create account" is the consent. Everything else above holds:
the app works fully without an account, and guest data merges once.
Figma: Onboarding + auth, frames R18-1 to R18-7. Plan 7 step 04,
Amendment 1.

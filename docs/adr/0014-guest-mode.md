# 14. Guest mode: the app works fully without an account

Date: 2026-11-02
Status: Accepted (owner, 2026-11-02). Details are refined in Plan 4.

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

## Consequences

- Plan 4 gains a guest-mode step (roughly M–L), with tests for "guest
  signs in to an account that already has data" and "account deleted,
  kept as guest".
- Screens never assume an account: sync line, sign out and delete
  account only show for signed-in users.
- Android Auto Backup rules should include the Room database so a
  guest's data survives a phone change.

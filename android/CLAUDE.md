# Android

Project status (phases, open items) lives in docs/status.md, not here.

## Sync rules
- The server owns conflict resolution and assigns all timestamps and
  revisions.
- Push, then pull, never at the same time.
- The server's response overwrites local state.
- Aggregates (session tree, template tree) sync whole.

## Database
- Bump the database version whenever the schema changes.
- fallbackToDestructiveMigration() stays on until Phase 5 removes it.

## Code
- The app must build and run at every commit.
- Exercise tracking type: WEIGHT_REPS is the default and covers ~90%
  of cases. In UI, never present the six values as an equal-weight
  required choice. Default to WEIGHT_REPS and treat the picker as an
  override.

## Verification
- Do not drive the device with adb tap/screenshot loops. Verify with
  the compiler and unit tests; leave manual UI verification to me.

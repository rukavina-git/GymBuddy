# Progress

- [ ] 01 Per-account local data (steps/01-per-account-data.md)
- [ ] 02 Guest mode
- [ ] 03 Auth gate
- [ ] 04 Body weight
- [ ] 05 Profile fields
- [ ] 06 Plans + default templates
- [ ] 07 Data export
- [ ] 08 CI green, PR develop → main (me)

## Deviations
None yet.

## Gotchas
- Open from Plan 4 (manual, owner): step 03 "Don't keep activities"
  check and a release build reading a draft; step 04 airplane-mode
  push check.
- From Plan 4 step 04: `push-outbox` uses KEEP, so a set logged while
  the job is still running can wait up to 6 h. Fix it if step 01
  touches the workers anyway.
- ProfileViewModel reads the Firebase uid statically; step 05 should
  inject AuthSession so its save paths become testable.

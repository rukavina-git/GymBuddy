# Plan 6: v1 features (data + logic)

## Why
The redesign (Plan 7) needs things v1 doesn't have: data kept apart
per account, guest mode, an auth gate that never wipes, body weight,
new profile fields, plans and export (roadmap-details.md, Plan 6;
ADR 0014, ADR 0015).

## Scope
1. Per-account local data + account switching rules (ADR 0014).
2. Guest mode: local-only use; create account / sign in merges.
3. Auth gate: dead session → "sign in again" banner, no wipe.
4. Body weight tracking: new table. Spec change.
5. Profile fields: experience, avatar, consent record, days per week.
   Spec change (owner OK 2026-11-02).
6. Plans (templates to rotate through) + new default templates and
   starter plans. Data model change.
7. Data export: versioned JSON + CSV (ADR 0015).
8. (me) CI green, PR develop → main.

Out of scope: new screens and restyling (Plan 7), staging/prod
(Plan 5), CI gates (8.1), OWASP (10.0).

## Invariants
- Sync rules in android/CLAUDE.md hold.
- Each step leaves the current UI working. New screens come in Plan 7;
  until then a feature has a plain entry point or none.
- `api/openapi.yaml` changes only in steps 4 and 5 (and 6 if its data
  model needs it), each said out loud, with OpenApiSpecDriftTest green.
- Room: every schema bump has a Migration + MigrationTestHelper test.
  Never a destructive fallback.
- No data of one owner (an account or the guest) is ever shown to,
  merged into or uploaded under another, except the guest → account
  merge in step 2, which asks first (ADR 0014).
- Every new dependency or asset: commercial-friendly licence, recorded.
- Each step builds and passes its tests on its own.

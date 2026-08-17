# 11. Storage is metric; conversion happens at display

Date: 2026-08-09
Status: Accepted

## Context

Users expect kilograms or pounds depending on where they are. The naive
approach stores whatever the user typed alongside a unit column.

That means every consumer — statistics, sync, the backend, both clients
— must handle both units, and a query that forgets the unit column
produces numbers that look plausible and are wrong by a factor of 2.2.
Aggregation across mixed units is meaningless.

## Decision

Storage is always metric: kilograms, centimetres, metres. Column names
carry the unit — `weightKg`, `heightCm`, `distanceMeters` — so a
mismatch is visible at the call site.

Conversion happens only at the display boundary, driven by a
device-local preference. `preferredUnits` is not part of the synced
profile: the same account on two devices may reasonably want different
display units, and nothing about the stored data changes.

## Consequences

- Comparison and aggregation are always valid without consulting a unit
  column.
- Only the presentation layer knows units exist.
- The round trip must be lossless. Display rounds to two decimals with
  trailing zeros trimmed; that rounding is never written back to
  storage. Without this, stored values degrade slightly every time a
  user toggles the setting.
- A user entering 225 lb has 102.0582 kg stored and sees 225.00 lb
  returned. Some applications also store the value as entered, purely so
  the user sees exactly what they typed. That is an extra column and
  extra sync surface for a cosmetic gain, and is not done here.

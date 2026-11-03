# 15. Data export is a versioned format that future versions can import

Date: 2026-11-02
Status: Accepted (owner, 2026-11-02)

## Context

Export moves into v1 (council docs/council/2026-11-02-you-settings.md).
The data model will change in v2 and v3. The owner wants a v1 export to
stay importable later ("in v3 you can still import some v1").

## Decision

- Export runs on the phone (Room to a file, Android Save dialog). It
  works offline and for guests; no server or api/openapi.yaml change.
- The canonical file is JSON with a top-level `formatVersion` (integer,
  starts at 1), `appVersion`, `exportedAt` (epoch ms) and the data.
- Contents: profile fields in use, custom exercises, templates,
  sessions with performed exercises and sets, per-user overlays (hidden,
  notes, rest). Ids are the existing UUIDv7s; units are metric with the
  unit in the field name (ADR 0011); history keeps its snapshots.
- A CSV of sets (one row per set) is offered alongside for spreadsheets.
  CSV is for people, not for re-import.
- Fields are only ever added within a formatVersion. Renames or removals
  bump formatVersion.
- Every future importer keeps a reader for each past formatVersion and
  upgrades step by step (1 to 2 to 3), so a v1 file imports in v3.
- Import itself is not in v1.

## Consequences

- formatVersion 1 is a contract from the first release: changes to it
  need a new version and a migration step.
- A test fixture of a v1 export is committed once export ships, and
  later importers must pass it.

## Amendment 2026-11-03 (owner)

CSV dropped from v1. The JSON is the backup and the source for any later
import, including a sets-only import into another account. A CSV for
spreadsheets can come back later, derived from the same data; it needs
no change to formatVersion.

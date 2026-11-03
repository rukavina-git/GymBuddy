# 07 Data export

## Problem
Users can't take their data out. ADR 0015 puts export in v1 as a
versioned JSON file (importable by later versions). CSV dropped (owner,
2026-11-03): the JSON covers backup and later sets-only import. It runs on the phone, offline, for guests too.
Roadmap 6.7. **No spec change, no Room schema change.**

## Owner decisions: 1A, 2A, 3A (2026-11-03)
1. **How the file leaves the phone** (ADR says Save dialog, roadmap
   says share sheet)
   - **A (recommended): Save dialog** (`ACTION_CREATE_DOCUMENT`, SAF).
     User picks a folder/Drive; no FileProvider, nothing left in cache.
   - **B: share sheet** (FileProvider + cache file). One tap to mail or
     Drive, but a copy sits in cache until cleared.
2. **Entry point before Plan 7**
   - **A (recommended): plain "Export data" row in Settings** opening
     the R15-11 sheet (Figma 110:18444) without the checkboxes: counts
     line + "Save file". Plan 7.10 restyles it.
   - **B: none.** Exporter + tests only.
3. **Default exercises and templates**
   - **A (recommended): only what the user owns** (custom exercises,
     own templates, overlays). Defaults are referenced by id; sessions
     already carry name snapshots, so history reads without them.
   - **B: include every referenced default too.** Bigger file, self-
     contained, but duplicates reference data the importer can refetch.

## Change
- **Domain** (`domain/export`, no Android imports): `ExportV1` model
  (formatVersion = 1, appVersion, exportedAt, owner kind guest/account,
  profile, bodyWeightEntries, customExercises, templates with
  exercises, sessions → performed exercises → sets, exerciseStates,
  templateStates, activePlanId). Metric units with the unit in the
  field name (`weightKg`, `heightCm`). Tombstoned rows left out.
- **Data:** `DataExporter` reads one consistent snapshot from the open
  file (`holder.db`, one read transaction, owner checked before and
  after), writes JSON with Moshi (already a dependency). Streams to an `OutputStream`, so it works with SAF or a file.
- **UI**: Settings row → `CreateDocument` launcher →
  `SettingsViewModel.export(uri)`; snackbar on done/failure.
  File name `gymbuddy-export-YYYY-MM-DD.json`.
- **Fixture:** `app/src/test/resources/export/v1-sample.json`,
  committed (ADR 0015), produced from a seeded test DB.

## Tests
- JSON: seeded DB (profile, body weight, custom exercise, template,
  session with sets, overlays, a tombstone) → exported JSON equals the
  committed fixture (ids, timestamps fixed). Tombstones absent.
- Another owner's data never appears (guest file vs account file).
- Empty guest file exports a valid JSON with empty arrays.
- ViewModel: success and IO failure states.

## Out of scope
Import (v2+), CSV (dropped; can come back from the same data),
server-side export, PDF, restyled UI (7.10).

## Done when
android `testDebugUnitTest` and `assembleDebug` green. No manual
checks (owner: none until the new UI).

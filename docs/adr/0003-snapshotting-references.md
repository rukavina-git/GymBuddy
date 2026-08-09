# 3. Snapshot display fields at write time

Date: 2026-08-09
Status: Accepted

## Context

Workout history and user templates reference exercises from
server-owned reference data. Rendering them by joining to the live
exercises table means a library update rewrites them: renaming
"Bench Press" to "Barbell Bench Press" retroactively changes every
workout ever logged.

## Decision

Records that must survive their references copy the fields they
display, at creation, and never update them.

- PerformedExercise snapshots exerciseName, category, trackingType
  and primaryMuscles.
- TemplateExercise snapshots exerciseName.

`exerciseId` is retained as a soft reference — not a foreign key —
supporting "history for this exercise" and tolerating a missing target.

## Consequences

- History is immutable and renders correctly regardless of library
  changes.
- Snapshot and live data can diverge. This is intended: the snapshot
  records what was true at the time.
- trackingType must be snapshotted alongside the name, since it
  determines how historical sets are rendered.
- A template referencing a genuinely absent exercise can still show
  a name rather than a blank row.
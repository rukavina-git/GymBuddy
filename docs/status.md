# Status

Current project status. Dated audits with full evidence are in
STATUS_YYYY-MM-DD.md files; this file is the short, current summary. What's next, step by step:
roadmap.md (overview) and roadmap-details.md.

## Phase 4: Android sync engine (built)

Details: STATUS_2026-09-26.md §Phase 4.

- CI green on main at 4349324 (PR #4, Plan 2 merged): Android Build,
  Backend Build, Sync E2E, API Client Codegen, API Docs, CodeQL. OWASP fails, non-blocking (ADR 0013).
- Hardening (docs/plans/phase4-hardening): logout wipes under the sync
  lock, logout errors end in a closable dialog, the scheduler never drops
  a request, pull-to-refresh says "sign in" when signed out
  (`syncNow` returns SyncOutcome).
- Device checks done 2026-10-31 (debug and minified release, hosted
  backend): phase5-data-safety progress.md, step 05. Follow-ups:
  roadmap 4.2a (auth gate), 4.2b (durable push).
- Plan 2 (data safety): Room v26 baseline with migration tests, no
  destructive fallback, Crashlytics, JSON logs with traceId/userId/
  client version on Render.

## Phase 1: schema hardening (done except Group K)

Audited 2026-09-26 against code, tests and commits. Evidence:
STATUS_2026-09-26.md §4a.

- A. Housekeeping: package move, UserProfile split, jvmToolchain [done]
- B. Identity: UUIDv7 generator + tests [done]
- C. ID migration: Exercise/PerformedExercise/TemplateExercise ids
  Int -> String, plus all foreign keys [done]
- D. Seed data: bundled JSON regenerated with stable UUIDs [done]
- E. Tracking types: ExerciseTrackingType on Exercise [done]
- F. Set model: nullable measurements, setType, isCompleted,
  restTakenSeconds [done]
- G. Snapshots: exercise name/category/trackingType/muscles copied into
  PerformedExercise; orderIndex; explicit ordering [done]
- H. Session/template: startedAt, endedAt, notes, templateId; planned
  duration/distance/weight on TemplateExercise [done]
- I. Overlay: user_exercise_state / user_template_state; isHidden and
  note off Exercise; isCustom/isDefault -> source [done]
- J. Sync metadata: updatedAt, deletedAt, revision, syncState [done]
- K. Prefs + UI [partial]: units, set editor and hidden screens done.
  Favourites and rest timer not started. Group K is separate from sync.
  - `isFavorite` is a column on both overlay entities, but no DAO query,
    repository, domain model or screen reads or writes it.
  - The active-workout screen says "there is no rest timer in v1"
    (ActiveWorkoutScreen.kt) and shows only the planned restSeconds.

## Next (not started)
- Phase 5: includes removing fallbackToDestructiveMigration().
  See STATUS_2026-09-26.md §5 for the wider list.

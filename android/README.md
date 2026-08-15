# GymBuddy — Android

Native Android client. Kotlin, Jetpack Compose, offline-first.

## Getting started

Open the `android/` directory in Android Studio — not the repository
root, which has no Gradle build.

Requires JDK 17 and a `google-services.json` from the Firebase console,
placed at `app/src/`. Neither is in the repository.

    ./gradlew assembleDebug
    ./gradlew testDebugUnitTest
    ./gradlew jacocoTestReport

Coverage report lands at `app/build/reports/jacoco/test/html/index.html`.

## Architecture

MVVM with a clean-architecture layering. The UI reads from Room and
never from the network directly — this is what makes offline a property
of the structure rather than a feature layered on top.

    domain/
      model         entities and enums, no Android imports
      id            UUIDv7 generation
      validation    set validation per tracking type
      repository    interfaces
      usecase       application logic
    data/
      local         Room entities, DAOs, converters, seeders
      mapper        entity ↔ domain
      repository    implementations
    ui/             Compose screens and ViewModels
    di/             Hilt modules

The domain layer is plain Kotlin and stays free of Android and Room
imports by convention. Room entities and domain models are separate
types with explicit mappers between them.

## Data model notes

Some of this is unusual enough to be worth explaining before you read
the code.

**All identifiers are client-generated UUIDv7**, because an entity
created offline cannot wait for a server to assign a key. The generator
is in `domain/id` and takes an injected `Clock` so it is deterministic
under test.

**Exercises come in two kinds in one table**, distinguished by `source`.
`DEFAULT` rows are server-owned reference data and are read-only on the
client, enforced in the repository layer. `CUSTOM` rows belong to the
user.

**User opinions about exercises live in overlay tables.** Hiding an
exercise, adding a note or favouriting it writes to
`user_exercise_state`, never to the exercise row — otherwise a reference
data refresh would destroy those preferences, and they could not sync,
because the server would be modifying a row shared with every other
user. Every list query left-joins the overlay.

**Workout history snapshots what it displays.** A `PerformedExercise`
copies the exercise's name, category, tracking type and muscles at
creation and never updates them, so renaming an exercise does not
retroactively rewrite last year's workouts. `exerciseId` is retained as
a soft reference and is deliberately not a foreign key.

**Sets carry four nullable measurements** — weight, reps, duration,
distance. Which are required depends on the exercise's
`ExerciseTrackingType`, enforced by `WorkoutSetValidator`. A set that is
not yet completed always passes validation, because a row the user has
added but not filled in legitimately has everything null.

**Storage is metric, always.** Kilograms, centimetres, metres. Column
names carry the unit. Conversion to imperial happens only at display,
and the round trip is lossless.

**Reference data is deprecated, never deleted.** Removing a default
exercise would break user templates referencing it, so entries carry a
`deprecated` flag: hidden from browsing, still resolvable by id.

## Deliberately unused columns

Several columns exist with no interface. This is not an oversight —
adding a column to an existing table after a backend exists costs a
schema migration on every client, while deferring an interface costs
nothing.

`setType`, `restTakenSeconds`, `defaultRestSeconds`, `isFavorite`,
`supersetGroup` and `derivedFromId` are all populated with defaults and
round-tripped through the mappers, awaiting the features that will read
them.

## Testing

85% line coverage on `domain/`, measured with UI and generated code
excluded. Coverage is deliberately uneven: use cases with logic are
fully covered, pure delegation is not tested at all.

ViewModel coverage is the known gap, and UI tests are intentionally
absent until the interface settles.

## Known issues

- `fallbackToDestructiveMigration()` is still enabled. Correct while the
  schema churns and there are no users; **must be removed and replaced
  with real migrations before release.**
- Several DAO filter queries are implemented but not reachable from the
  UI, which currently exposes only muscle group and equipment.
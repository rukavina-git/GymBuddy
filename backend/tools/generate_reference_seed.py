#!/usr/bin/env python3
"""
Generates backend/src/main/resources/db/migration/V2__seed_reference_data.sql
from the Android bundle's default exercise/template JSON.

Those two JSON files are the only source of truth for default exercise
and template ids. This script parses them and emits SQL - it never
hand-transcribes an id - so the server's reference library is
byte-for-byte the same set of ids as whatever ships in the app. If the
ids diverge, a client that seeded locally from the bundle and then syncs
gets the same exercises back under different ids: silent duplication,
with templates pointing at rows the server has never heard of.

Re-run after any change to the source JSON:

    python3 backend/tools/generate_reference_seed.py

The output is deterministic (fixed updated_at, stable row order) so a
re-run with unchanged inputs produces a byte-identical file and Flyway's
checksum for V2 never moves without the inputs actually changing.
"""
import json
from pathlib import Path
from typing import Optional

REPO_ROOT = Path(__file__).resolve().parents[2]
EXERCISES_JSON = REPO_ROOT / "android/app/src/main/assets/default_exercises.json"
TEMPLATES_JSON = REPO_ROOT / "android/app/src/main/assets/default_workout_templates.json"
OUTPUT_SQL = REPO_ROOT / "backend/src/main/resources/db/migration/V2__seed_reference_data.sql"

# Deliberately the Unix epoch, not "now" - a sentinel, not a real event
# time. Keeps the migration deterministic so its Flyway checksum only
# ever changes when the seeded data actually changes.
SEED_UPDATED_AT = 0


def sql_str(value: str) -> str:
    """Single-quoted SQL string literal, with '' escaping."""
    return "'" + value.replace("'", "''") + "'"


def sql_str_or_null(value: Optional[str]) -> str:
    if value is None or value == "":
        return "NULL"
    return sql_str(value)


def sql_int_or_null(value) -> str:
    if value is None:
        return "NULL"
    return str(int(value))


def sql_float_or_null(value) -> str:
    if value is None:
        return "NULL"
    return str(float(value))


def sql_bool(value: bool) -> str:
    return "TRUE" if value else "FALSE"


def sql_jsonb(value) -> str:
    return sql_str(json.dumps(value, ensure_ascii=False)) + "::jsonb"


def exercise_insert(e: dict) -> str:
    cols = [
        "id", "name", "primary_muscles", "secondary_muscles", "description",
        "instructions", "tips", "difficulty", "equipment_needed", "category",
        "exercise_type", "tracking_type", "video_url", "thumbnail_url",
        "source", "owner_id", "derived_from_id", "deprecated",
        "updated_at", "deleted_at", "revision",
    ]
    values = [
        sql_str(e["id"]),
        sql_str(e["name"]),
        sql_jsonb(e["primaryMuscles"]),
        sql_jsonb(e["secondaryMuscles"]),
        sql_str_or_null(e.get("description")),
        sql_jsonb(e["instructions"]),
        sql_jsonb(e["tips"]),
        sql_str(e["difficulty"]),
        sql_jsonb(e["equipmentNeeded"]),
        sql_str(e["category"]),
        sql_str(e["exerciseType"]),
        sql_str(e["trackingType"]),
        sql_str_or_null(e.get("videoUrl")),
        sql_str_or_null(e.get("thumbnailUrl")),
        "'DEFAULT'",
        "NULL",
        "NULL",
        sql_bool(e.get("deprecated", False)),
        str(SEED_UPDATED_AT),
        "NULL",
        "0",
    ]
    return f"INSERT INTO exercises ({', '.join(cols)})\nVALUES ({', '.join(values)});"


def template_insert(t: dict) -> str:
    cols = ["id", "title", "source", "owner_id", "derived_from_id", "deprecated", "updated_at", "deleted_at", "revision"]
    values = [
        sql_str(t["id"]),
        sql_str(t["title"]),
        "'DEFAULT'",
        "NULL",
        "NULL",
        sql_bool(t.get("deprecated", False)),
        str(SEED_UPDATED_AT),
        "NULL",
        "0",
    ]
    return f"INSERT INTO workout_templates ({', '.join(cols)})\nVALUES ({', '.join(values)});"


def template_exercise_insert(template_id: str, te: dict) -> str:
    cols = [
        "id", "template_id", "exercise_id", "exercise_name", "exercise_tracking_type",
        "planned_sets", "planned_reps", "order_index", "rest_seconds",
        "planned_duration_seconds", "planned_distance_meters", "planned_weight_kg", "notes",
    ]
    # restSeconds of 0 is treated as "not set", matching
    # WorkoutTemplateSeeder.kt's optInt(...).takeIf { it > 0 }.
    rest_seconds = te.get("restSeconds")
    if rest_seconds is not None and rest_seconds <= 0:
        rest_seconds = None
    values = [
        sql_str(te["id"]),
        sql_str(template_id),
        sql_str(te["exerciseId"]),
        sql_str(te["exerciseName"]),
        sql_str(te["exerciseTrackingType"]),
        sql_int_or_null(te["plannedSets"]),
        sql_int_or_null(te.get("plannedReps")),
        sql_int_or_null(te["orderIndex"]),
        sql_int_or_null(rest_seconds),
        sql_int_or_null(te.get("plannedDurationSeconds")),
        sql_float_or_null(te.get("plannedDistanceMeters")),
        sql_float_or_null(te.get("plannedWeightKg")),
        sql_str_or_null(te.get("notes")),
    ]
    return f"INSERT INTO template_exercises ({', '.join(cols)})\nVALUES ({', '.join(values)});"


def reference_version_insert(exercise_version: int, template_version: int) -> str:
    cols = ["id", "exercise_library_version", "template_library_version", "updated_at"]
    values = ["1", str(exercise_version), str(template_version), str(SEED_UPDATED_AT)]
    return f"INSERT INTO reference_version ({', '.join(cols)})\nVALUES ({', '.join(values)});"


def main() -> None:
    exercises_data = json.loads(EXERCISES_JSON.read_text())
    templates_data = json.loads(TEMPLATES_JSON.read_text())

    exercises = exercises_data["exercises"]
    templates = templates_data["templates"]
    total_template_exercises = sum(len(t["exercises"]) for t in templates)

    lines = [
        "-- Generated by backend/tools/generate_reference_seed.py. Do not hand-edit.",
        "-- Regenerate after changing:",
        f"--   {EXERCISES_JSON.relative_to(REPO_ROOT)} (version {exercises_data['version']}, {len(exercises)} exercises)",
        f"--   {TEMPLATES_JSON.relative_to(REPO_ROOT)} (version {templates_data['version']}, {len(templates)} templates, {total_template_exercises} template exercises)",
        "--",
        "-- Every id below is parsed from those files, never transcribed by",
        "-- hand: the client's bundle and the server's reference library must",
        "-- agree on ids exactly, or a synced client silently double-seeds.",
        "",
        "-- ============================================================",
        "-- exercises",
        "-- ============================================================",
        "",
    ]

    for e in exercises:
        lines.append(exercise_insert(e))
        lines.append("")

    lines.append("-- ============================================================")
    lines.append("-- workout_templates")
    lines.append("-- ============================================================")
    lines.append("")
    for t in templates:
        lines.append(template_insert(t))
        lines.append("")

    lines.append("-- ============================================================")
    lines.append("-- template_exercises")
    lines.append("-- ============================================================")
    lines.append("")
    for t in templates:
        for te in t["exercises"]:
            lines.append(template_exercise_insert(t["id"], te))
            lines.append("")

    lines.append("-- ============================================================")
    lines.append("-- reference_version")
    lines.append("-- ============================================================")
    lines.append("")
    lines.append(reference_version_insert(exercises_data["version"], templates_data["version"]))
    lines.append("")

    OUTPUT_SQL.write_text("\n".join(lines))
    print(f"Wrote {OUTPUT_SQL.relative_to(REPO_ROOT)}")
    print(f"  {len(exercises)} exercises, {len(templates)} templates, {total_template_exercises} template_exercises")


if __name__ == "__main__":
    main()

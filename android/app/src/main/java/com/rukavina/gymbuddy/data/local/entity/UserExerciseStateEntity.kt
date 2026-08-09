package com.rukavina.gymbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-user overlay state for an Exercise: hidden/favorite flags, a personal
 * note, and a preferred default rest time. Exists so user preferences
 * survive a reference-data refresh - hiding an exercise must never mean
 * mutating the exercises row itself, since that row can be replaced
 * wholesale by a library update or (once synced) is owned by the server
 * rather than the user.
 *
 * Sparse table: a row exists only once the user has expressed an opinion
 * about the exercise. A 300-exercise library with 4 hidden produces 4
 * rows.
 *
 * exerciseId is a soft reference to the exercises table, not a foreign
 * key: a row is left in place if the referenced exercise is later removed.
 * Orphan rows are harmless - the preference simply returns if an exercise
 * with that id reappears - and are excluded from user-facing listings by
 * joining back to exercises rather than by cleanup.
 *
 * Upsert-only. Rows are never hard-deleted, even once "cleared" (all flags
 * false, note null, defaultRestSeconds null) - this deliberately deviates
 * from the deprecated-flag soft-delete convention used elsewhere (see
 * docs/adr/0002-deprecation-over-deletion.md). There is no undelete
 * concept for a preference row; a cleared row and a missing row mean the
 * same thing, so there is nothing to gain from ever deleting one, and it
 * keeps the write path a single upsert instead of an upsert-or-delete
 * branch.
 */
@Entity(tableName = "user_exercise_state")
data class UserExerciseStateEntity(
    @PrimaryKey
    val exerciseId: String,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val note: String? = null,
    val defaultRestSeconds: Int? = null
)

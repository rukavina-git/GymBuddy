package com.rukavina.gymbuddy.domain.model

/**
 * Whether a reference-data row (Exercise, WorkoutTemplate) was bundled
 * with the app / provided by the server, or created by the user.
 *
 * Replaces the old isCustom/isDefault booleans so a future server-owned
 * source doesn't need a third boolean bolted on.
 */
enum class EntitySource {
    DEFAULT,
    CUSTOM
}

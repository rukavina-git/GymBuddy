package com.rukavina.gymbuddy.data.model

/**
 * Enum representing whether an exercise is compound or isolation.
 * Compound exercises work multiple joints/muscle groups.
 * Isolation exercises target a single muscle group.
 */
enum class ExerciseType {
    /** Multi-joint exercises that work multiple muscle groups (e.g., squat, deadlift, bench press) */
    COMPOUND,

    /** Single-joint exercises that target one specific muscle group (e.g., bicep curl, leg extension) */
    ISOLATION
}

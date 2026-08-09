package com.rukavina.gymbuddy.data.model

/**
 * Enum representing the general category or purpose of an exercise.
 * Helps classify exercises by training modality.
 */
enum class ExerciseCategory {
    /** Resistance training exercises for building strength and muscle */
    STRENGTH,

    /** Cardiovascular exercises for endurance and heart health */
    CARDIO,

    /** Stretching and mobility exercises */
    FLEXIBILITY,

    /** Explosive jumping and power exercises */
    PLYOMETRIC,

    /** Olympic weightlifting movements (clean, snatch, etc.) */
    OLYMPIC_LIFTING,

    /** Strongman-style exercises (farmer's walks, tire flips, etc.) */
    STRONGMAN,

    /** Calisthenics and gymnastics movements */
    CALISTHENICS
}

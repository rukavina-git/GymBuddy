package com.rukavina.gymbuddy.data.model

/**
 * Enum representing equipment types needed for exercises.
 * Helps users filter exercises based on available equipment.
 */
enum class Equipment {
    /** Standard Olympic barbell */
    BARBELL,

    /** Dumbbells (pair or single) */
    DUMBBELL,

    /** Kettlebell */
    KETTLEBELL,

    /** Cable machine */
    CABLE,

    /** Weight machine (leg press, chest press, etc.) */
    MACHINE,

    /** No equipment needed - uses body weight */
    BODYWEIGHT,

    /** Resistance bands or loops */
    RESISTANCE_BAND,

    /** EZ curl bar (angled barbell) */
    EZ_BAR,

    /** Trap bar (hex bar) for deadlifts */
    TRAP_BAR,

    /** Pull-up bar */
    PULL_UP_BAR,

    /** Bench (flat, incline, or decline) */
    BENCH,

    /** Medicine ball */
    MEDICINE_BALL,

    /** Suspension trainer (TRX, etc.) */
    SUSPENSION_TRAINER,

    /** Smith machine */
    SMITH_MACHINE
}

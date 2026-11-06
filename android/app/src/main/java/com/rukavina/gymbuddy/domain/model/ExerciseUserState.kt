package com.rukavina.gymbuddy.domain.model

/**
 * Your own state for one exercise (user_exercise_state overlay): hidden
 * from lists, your note (shown everywhere), your rest between sets (used
 * when the exercise is added to a template or workout; null = default).
 */
data class ExerciseUserState(
    val exerciseId: String,
    val hidden: Boolean = false,
    val note: String? = null,
    val restSeconds: Int? = null,
)

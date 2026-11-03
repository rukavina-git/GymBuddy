package com.rukavina.gymbuddy.domain.model

/**
 * An in-progress workout, kept on the device so it survives process
 * death, swipe-away and reboot. Local only, never synced. Set values are
 * the raw text the user typed, in their display units at the time.
 * Stored as JSON: new fields need defaults so older drafts still load.
 *
 * [savedAt] is when [elapsedSeconds] was captured: if the timer was
 * running, the time since then counts towards the workout on restore.
 */
data class ActiveWorkoutDraft(
    val title: String,
    val templateId: String?,
    val templateTitle: String?,
    val startedAt: Long,
    val elapsedSeconds: Long,
    val isTimerRunning: Boolean,
    val savedAt: Long,
    val exercises: List<Exercise>,
    /** Exercise on screen. Drafts from before Plan 7 06a lack it: 0. */
    val currentExerciseIndex: Int = 0,
    /** Rest running after the last done set, null when none. */
    val rest: Rest? = null
) {
    /**
     * Rest after set [afterSetId], counted on the workout clock: it began
     * at [startedAtElapsed] seconds into the workout and lasts [seconds].
     */
    data class Rest(
        val afterSetId: String,
        val startedAtElapsed: Long,
        val seconds: Int
    )

    data class Exercise(
        val id: String,
        val exerciseId: String,
        val exerciseName: String,
        val trackingType: ExerciseTrackingType,
        val plannedSets: Int,
        val plannedReps: Int?,
        val restSeconds: Int?,
        val sets: List<Set>
    )

    data class Set(
        val id: String,
        val setNumber: Int,
        val reps: String,
        val weight: String,
        val duration: String,
        val distance: String,
        val notes: String,
        val isCompleted: Boolean
    )
}

package com.rukavina.gymbuddy.domain.usecase.workout

/**
 * F25 (Plan 7 11m): a workout nobody logs a set in for [STALE_AFTER_MS]
 * was forgotten. Pure rules; the active workout and its draft supply the
 * times.
 */
object StaleWorkout {
    /** No set (or resume) for this long: ask "Still training?". */
    const val STALE_AFTER_MS = 2 * 60 * 60 * 1000L

    /** Not opened for this long: finish at the last set without asking. */
    const val EXPIRED_AFTER_MS = 24 * 60 * 60 * 1000L

    enum class Status { Active, Stale, Expired }

    /**
     * The last sign of life: the latest done set or resume, else the
     * start. All epoch ms.
     */
    fun lastActivity(startedAt: Long, doneAts: List<Long>, resumedAt: Long?): Long =
        maxOf(startedAt, doneAts.maxOrNull() ?: Long.MIN_VALUE, resumedAt ?: Long.MIN_VALUE)

    fun status(lastActivity: Long, now: Long): Status {
        val idle = now - lastActivity
        return when {
            idle >= EXPIRED_AFTER_MS -> Status.Expired
            idle >= STALE_AFTER_MS -> Status.Stale
            else -> Status.Active
        }
    }

    /** Where "finish at last set" ends: (endedAt, durationSeconds), or null with no timed set. */
    data class End(val endedAt: Long, val durationSeconds: Long)

    /**
     * The end of the latest done set: its wall time, and the workout clock
     * then. [sets] are (doneAt, doneAtElapsed) of done sets.
     */
    fun endAtLastSet(sets: List<Pair<Long, Long>>): End? =
        sets.maxByOrNull { it.first }?.let { (at, elapsed) -> End(at, elapsed) }
}

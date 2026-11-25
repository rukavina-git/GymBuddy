package com.rukavina.gymbuddy.update

/**
 * Whether the start screen covers the app, so Play's update screen
 * doesn't open over a flash of Home (and backing out of a forced update
 * doesn't flash it either). Covered from launch until the first check
 * answers "nothing" or [TIMEOUT_MS] passes; covered again while Play's
 * update screen is up.
 */
class UpdateCover {
    var covered: Boolean = true
        private set
    private var decided = false

    fun onDecided(action: UpdateAction) {
        decided = true
        covered = action != UpdateAction.NONE
    }

    /** No answer from Play in time (slow or no network): show the app. */
    fun onTimeout() {
        if (!decided) covered = false
    }

    /** Back from Play's update screen. A forced update backed out of closes the app: stay covered. */
    fun onResult(closing: Boolean) {
        if (!closing) covered = false
    }

    companion object {
        const val TIMEOUT_MS = 1_000L
    }
}

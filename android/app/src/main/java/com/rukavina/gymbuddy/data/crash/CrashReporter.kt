package com.rukavina.gymbuddy.data.crash

/**
 * Non-fatal reports for testers' builds, so sync code doesn't import
 * Firebase. Nothing passed here may contain an ID token, password,
 * email or backend URL; user ids are the Firebase uid only.
 */
interface CrashReporter {
    /** Reports [error] with [keys] attached to this report only. */
    fun recordNonFatal(error: Throwable, keys: Map<String, String> = emptyMap())

    /** Firebase uid of the signed-in user, or null once signed out. */
    fun setUserId(uid: String?)
}

/** Reports nothing. The default where no reporter is wired in. */
object NoOpCrashReporter : CrashReporter {
    override fun recordNonFatal(error: Throwable, keys: Map<String, String>) = Unit
    override fun setUserId(uid: String?) = Unit
}

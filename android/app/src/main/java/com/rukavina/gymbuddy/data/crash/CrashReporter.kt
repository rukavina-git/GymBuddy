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

/**
 * Keys that may reach Crashlytics (Plan 9 step 04). The privacy policy
 * promises the error, app and phone details and the uid, never workout
 * or body data. A new key must be added here on purpose: anything else
 * is dropped. Values: sync reason, server trace id, HTTP status, and
 * for discarded_<type> the random UUIDv7 ids of the lost rows.
 */
private val CRASH_KEYS = setOf("reason", "traceId", "httpStatus")
private const val DISCARDED_PREFIX = "discarded_"

internal fun allowedCrashKeys(keys: Map<String, String>): Map<String, String> =
    keys.filterKeys { it in CRASH_KEYS || it.startsWith(DISCARDED_PREFIX) }

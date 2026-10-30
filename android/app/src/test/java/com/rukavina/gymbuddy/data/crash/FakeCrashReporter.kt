package com.rukavina.gymbuddy.data.crash

/** Records reports instead of sending them to Crashlytics. */
class FakeCrashReporter : CrashReporter {
    data class Report(val error: Throwable, val keys: Map<String, String>)

    val reports = mutableListOf<Report>()
    var currentUserId: String? = null
        private set

    override fun recordNonFatal(error: Throwable, keys: Map<String, String>) {
        reports += Report(error, keys)
    }

    override fun setUserId(uid: String?) {
        currentUserId = uid
    }
}

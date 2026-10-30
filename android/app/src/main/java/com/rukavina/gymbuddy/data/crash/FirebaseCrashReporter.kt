package com.rukavina.gymbuddy.data.crash

import com.google.firebase.crashlytics.CustomKeysAndValues
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Crashlytics-backed [CrashReporter]. Collection is off in debug builds
 * (manifest flag), so these calls are dropped there. Crashlytics is
 * resolved on first use, like FirebaseAuthSession, so building the
 * object graph never requires an initialised FirebaseApp.
 */
class FirebaseCrashReporter(
    crashlyticsProvider: () -> FirebaseCrashlytics = { FirebaseCrashlytics.getInstance() }
) : CrashReporter {

    private val crashlytics by lazy(crashlyticsProvider)

    override fun recordNonFatal(error: Throwable, keys: Map<String, String>) {
        val custom = CustomKeysAndValues.Builder()
        keys.forEach { (key, value) -> custom.putString(key, value) }
        crashlytics.recordException(error, custom.build())
    }

    // Crashlytics has no "clear": an empty id stops attributing reports.
    override fun setUserId(uid: String?) = crashlytics.setUserId(uid ?: "")
}

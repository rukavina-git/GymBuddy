package com.rukavina.gymbuddy.data.crash

import org.junit.Assert.assertEquals
import org.junit.Test

/** Plan 9 step 04: only allow-listed keys reach Crashlytics. */
class CrashKeysTest {

    @Test
    fun keepsTheKeysTheSyncAndLogoutReportsUse() {
        val keys = mapOf("reason" to "PERIODIC", "traceId" to "abc", "httpStatus" to "500", "discarded_SESSION" to "id1,id2")
        assertEquals(keys, allowedCrashKeys(keys))
    }

    @Test
    fun dropsAnythingElse() {
        val keys = mapOf("reason" to "PERIODIC", "email" to "a@b.c", "bodyWeight" to "80")
        assertEquals(mapOf("reason" to "PERIODIC"), allowedCrashKeys(keys))
    }
}

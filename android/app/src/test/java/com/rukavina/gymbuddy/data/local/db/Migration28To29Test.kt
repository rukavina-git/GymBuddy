package com.rukavina.gymbuddy.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** v29 adds nullable profile fields (Plan 6 step 05) and keeps the profile row. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration28To29Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun `28 to 29 keeps the profile and leaves the new fields empty`() {
        helper.createDatabase(TEST_DB, 28).apply {
            execSQL(
                "INSERT INTO user_profile (uid, name, email, joinedDate, updatedAt, revision, syncState, weight) " +
                    "VALUES ('uid-a', 'Karlo', 'k@example.com', 1, 2, 3, 'SYNCED', 80.5)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 29, true, MIGRATION_28_29)

        db.query("SELECT name, weight, revision, experienceLevel, avatarId, trainingDays, healthDataConsentVersion, healthDataConsentAt FROM user_profile WHERE uid = 'uid-a'").use { c ->
            c.moveToFirst()
            assertEquals("Karlo", c.getString(0))
            assertEquals(80.5f, c.getFloat(1))
            assertEquals(3, c.getInt(2))
            (3..7).forEach { assertNull(c.getString(it)) }
        }
        db.execSQL("UPDATE user_profile SET experienceLevel = 'SOME', avatarId = 'cat', healthDataConsentAt = 5 WHERE uid = 'uid-a'")
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-28-29-test"
    }
}

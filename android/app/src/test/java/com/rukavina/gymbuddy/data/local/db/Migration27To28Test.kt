package com.rukavina.gymbuddy.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** v28 adds body_weight_entries (Plan 6 step 04) and keeps existing data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration27To28Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun `27 to 28 adds the body weight table and keeps existing rows`() {
        helper.createDatabase(TEST_DB, 27).apply {
            execSQL("INSERT INTO sync_meta (`key`, `value`) VALUES ('marker', 'kept')")
            execSQL("INSERT INTO active_workout_draft (id, payloadJson, updatedAt) VALUES (0, '{}', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 28, true, MIGRATION_27_28)

        val marker = db.query("SELECT `value` FROM sync_meta WHERE `key` = 'marker'")
            .use { c -> if (c.moveToFirst()) c.getString(0) else null }
        assertEquals("kept", marker)
        val drafts = db.query("SELECT COUNT(*) FROM active_workout_draft").use { c -> c.moveToFirst(); c.getInt(0) }
        assertEquals(1, drafts)

        db.execSQL(
            "INSERT INTO body_weight_entries (id, measuredAt, weightKg, updatedAt, deletedAt, revision, syncState) " +
                "VALUES ('e1', 1, 80.5, 1, NULL, 0, 'PENDING')"
        )
        val weight = db.query("SELECT weightKg FROM body_weight_entries WHERE id = 'e1'")
            .use { c -> c.moveToFirst(); c.getFloat(0) }
        assertEquals(80.5f, weight)
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-27-28-test"
    }
}

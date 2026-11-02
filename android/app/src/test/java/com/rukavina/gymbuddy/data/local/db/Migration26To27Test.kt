package com.rukavina.gymbuddy.data.local.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** v27 adds the local-only active_workout_draft table and keeps existing data. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration26To27Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun `26 to 27 adds the draft table and keeps existing rows`() {
        helper.createDatabase(TEST_DB, 26).apply {
            execSQL("INSERT INTO sync_meta (`key`, `value`) VALUES ('marker', 'kept')")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 27, true, MIGRATION_26_27)

        val marker = db.query("SELECT `value` FROM sync_meta WHERE `key` = 'marker'")
            .use { c -> if (c.moveToFirst()) c.getString(0) else null }
        assertEquals("kept", marker)

        db.execSQL(
            "INSERT INTO active_workout_draft (id, payloadJson, updatedAt) VALUES (0, '{}', 1)"
        )
        val count = db.query("SELECT COUNT(*) FROM active_workout_draft")
            .use { c -> c.moveToFirst(); c.getInt(0) }
        assertEquals(1, count)
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-26-27-test"
    }
}

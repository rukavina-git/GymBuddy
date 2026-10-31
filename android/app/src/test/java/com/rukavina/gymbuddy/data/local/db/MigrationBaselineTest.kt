package com.rukavina.gymbuddy.data.local.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v26 is the schema on installed devices. Builds it from the exported
 * schema and opens it with the app's configuration: no migration runs
 * and no table is dropped.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationBaselineTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private var db: AppDatabase? = null

    @After
    fun tearDown() {
        db?.close()
    }

    @Test
    fun `v26 baseline opens with the app configuration`() {
        helper.createDatabase(TEST_DB, 26).apply {
            execSQL("INSERT INTO sync_meta (`key`, `value`) VALUES ('marker', 'kept')")
            close()
        }

        val opened = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java,
            TEST_DB
        ).applyAppConfig().allowMainThreadQueries().build()
        db = opened

        val sqlite = opened.openHelper.writableDatabase
        val tables = sqlite.query(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        ).use { c -> buildSet { while (c.moveToNext()) add(c.getString(0)) } }
        assertEquals(EXPECTED_TABLES, tables.intersect(EXPECTED_TABLES))

        val marker = sqlite.query("SELECT `value` FROM sync_meta WHERE `key` = 'marker'")
            .use { c -> if (c.moveToFirst()) c.getString(0) else null }
        assertEquals("kept", marker)
    }

    private companion object {
        const val TEST_DB = "migration-test"
        val EXPECTED_TABLES = setOf(
            "user_profile",
            "exercises",
            "exercise_version",
            "template_version",
            "workout_sessions",
            "performed_exercises",
            "workout_templates",
            "template_exercises",
            "workout_sets",
            "user_exercise_state",
            "user_template_state",
            "sync_outbox",
            "sync_log",
            "sync_meta",
        )
    }
}

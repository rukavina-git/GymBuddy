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

/** v30 adds starter plans and the profile's activePlanId (Plan 6 step 06). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration29To30Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun `29 to 30 keeps profiles and templates and adds empty plan tables`() {
        helper.createDatabase(TEST_DB, 29).apply {
            execSQL(
                "INSERT INTO user_profile (uid, name, email, joinedDate, updatedAt, revision, syncState, trainingDays) " +
                    "VALUES ('uid-a', 'Karlo', 'k@example.com', 1, 2, 3, 'SYNCED', 'FOUR')"
            )
            execSQL(
                "INSERT INTO workout_templates (id, title, source, deprecated, updatedAt, revision, syncState) " +
                    "VALUES ('t-1', 'Chest', 'DEFAULT', 0, 0, 0, 'SYNCED')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 30, true, MIGRATION_29_30)

        db.query("SELECT name, trainingDays, revision, activePlanId FROM user_profile WHERE uid = 'uid-a'").use { c ->
            c.moveToFirst()
            assertEquals("Karlo", c.getString(0))
            assertEquals("FOUR", c.getString(1))
            assertEquals(3, c.getInt(2))
            assertNull(c.getString(3))
        }
        db.query("SELECT title FROM workout_templates WHERE id = 't-1'").use { c ->
            c.moveToFirst()
            assertEquals("Chest", c.getString(0))
        }
        db.query("SELECT COUNT(*) FROM starter_plans").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        db.execSQL("INSERT INTO starter_plans (id, title, displayOrder, deprecated) VALUES ('p-1', 'Full Body', 0, 0)")
        db.execSQL("INSERT INTO starter_plan_templates (planId, orderIndex, templateId) VALUES ('p-1', 0, 't-1')")
        db.execSQL("UPDATE user_profile SET activePlanId = 'p-1' WHERE uid = 'uid-a'")
        db.close()
    }

    private companion object {
        const val TEST_DB = "migration-29-30-test"
    }
}

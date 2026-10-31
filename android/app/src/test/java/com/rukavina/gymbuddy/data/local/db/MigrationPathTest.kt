package com.rukavina.gymbuddy.data.local.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Gate for every schema bump: the current version must be reachable from
 * the v26 baseline through ALL_MIGRATIONS, and the migrated schema must
 * match the exported one. Trivially true while the version is 26.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationPathTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    // @Database isn't retained at runtime; Room's open helper reports the version.
    private val currentVersion: Int by lazy {
        val db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        try {
            db.openHelper.writableDatabase.version
        } finally {
            db.close()
        }
    }

    @Test
    fun `every version from the baseline has a migration`() {
        val steps = ALL_MIGRATIONS.map { it.startVersion to it.endVersion }.toSet()
        val missing = (BASELINE until currentVersion)
            .filter { (it to it + 1) !in steps }
            .map { "$it -> ${it + 1}" }
        assertTrue("Missing migrations: $missing", missing.isEmpty())
    }

    @Test
    fun `baseline migrates to the current schema`() {
        helper.createDatabase(TEST_DB, BASELINE).close()
        helper.runMigrationsAndValidate(TEST_DB, currentVersion, true, *ALL_MIGRATIONS).close()
    }

    private companion object {
        const val BASELINE = 26
        const val TEST_DB = "migration-path-test"
    }
}

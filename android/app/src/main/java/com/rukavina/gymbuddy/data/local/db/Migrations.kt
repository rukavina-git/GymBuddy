package com.rukavina.gymbuddy.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v27: local-only table for the in-progress workout (plan 4 step 03). */
val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `active_workout_draft` (" +
                "`id` INTEGER NOT NULL, `payloadJson` TEXT NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}

/**
 * Every schema migration, in version order. v26 is the baseline
 * (app/schemas/.../26.json); each version bump adds its Migration here
 * and a MigrationTestHelper test.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_26_27)

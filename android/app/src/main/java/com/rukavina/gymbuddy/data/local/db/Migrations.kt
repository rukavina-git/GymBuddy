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

/** v28: body weight history (Plan 6 step 04), synced like workouts. */
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `body_weight_entries` (" +
                "`id` TEXT NOT NULL, `measuredAt` INTEGER NOT NULL, `weightKg` REAL NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, `revision` INTEGER NOT NULL, " +
                "`syncState` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
    }
}

/** v29: profile fields for onboarding and You (Plan 6 step 05). All nullable. */
val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `experienceLevel` TEXT")
        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `avatarId` TEXT")
        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `trainingDays` TEXT")
        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `healthDataConsentVersion` TEXT")
        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `healthDataConsentAt` INTEGER")
    }
}

/**
 * Every schema migration, in version order. v26 is the baseline
 * (app/schemas/.../26.json); each version bump adds its Migration here
 * and a MigrationTestHelper test.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29)

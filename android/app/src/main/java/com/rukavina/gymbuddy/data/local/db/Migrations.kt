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
 * Plan 6 step 06: starter plans (reference data) and the profile's
 * active plan. Plans arrive with the next template library load: the
 * server's library version moves to 6, so SyncEngine reloads it.
 */
val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `activePlanId` TEXT")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `starter_plans` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                "`description` TEXT, `suggestedFor` TEXT, `displayOrder` INTEGER NOT NULL, " +
                "`deprecated` INTEGER NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `starter_plan_templates` (`planId` TEXT NOT NULL, " +
                "`orderIndex` INTEGER NOT NULL, `templateId` TEXT NOT NULL, PRIMARY KEY(`planId`, `orderIndex`), " +
                "FOREIGN KEY(`planId`) REFERENCES `starter_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_starter_plan_templates_templateId` ON `starter_plan_templates` (`templateId`)"
        )
    }
}

/**
 * Every schema migration, in version order. v26 is the baseline
 * (app/schemas/.../26.json); each version bump adds its Migration here
 * and a MigrationTestHelper test.
 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30)

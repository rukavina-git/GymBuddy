package com.rukavina.gymbuddy.data.local.db

import androidx.room.RoomDatabase

/** File name of the on-device database. Installed devices hold their data here. */
const val DATABASE_NAME = "gym_buddy_db"

/**
 * The app's Room configuration, shared by AppModule and the migration
 * tests so both open the database the same way.
 */
fun RoomDatabase.Builder<AppDatabase>.applyAppConfig(): RoomDatabase.Builder<AppDatabase> =
    addMigrations(*ALL_MIGRATIONS)

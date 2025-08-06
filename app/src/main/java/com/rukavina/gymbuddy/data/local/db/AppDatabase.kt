package com.rukavina.gymbuddy.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rukavina.gymbuddy.data.local.dao.UserProfileDao
import com.rukavina.gymbuddy.data.model.UserProfile

@Database(entities = [UserProfile::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
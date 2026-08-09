package com.rukavina.gymbuddy.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rukavina.gymbuddy.data.local.converter.ExerciseConverters
import com.rukavina.gymbuddy.data.local.converter.MuscleGroupConverter
import com.rukavina.gymbuddy.data.local.converter.ProfileEnumConverters
import com.rukavina.gymbuddy.data.local.dao.ExerciseDao
import com.rukavina.gymbuddy.data.local.dao.ExerciseVersionDao
import com.rukavina.gymbuddy.data.local.dao.TemplateVersionDao
import com.rukavina.gymbuddy.data.local.dao.UserExerciseStateDao
import com.rukavina.gymbuddy.data.local.dao.UserProfileDao
import com.rukavina.gymbuddy.data.local.dao.UserTemplateStateDao
import com.rukavina.gymbuddy.data.local.dao.WorkoutSessionDao
import com.rukavina.gymbuddy.data.local.dao.WorkoutTemplateDao
import com.rukavina.gymbuddy.data.local.entity.ExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.ExerciseVersionEntity
import com.rukavina.gymbuddy.data.local.entity.PerformedExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateExerciseEntity
import com.rukavina.gymbuddy.data.local.entity.TemplateVersionEntity
import com.rukavina.gymbuddy.data.local.entity.UserExerciseStateEntity
import com.rukavina.gymbuddy.data.local.entity.UserProfileEntity
import com.rukavina.gymbuddy.data.local.entity.UserTemplateStateEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutSessionEntity
import com.rukavina.gymbuddy.data.local.entity.WorkoutTemplateEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ExerciseEntity::class,
        ExerciseVersionEntity::class,
        TemplateVersionEntity::class,
        WorkoutSessionEntity::class,
        PerformedExerciseEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        com.rukavina.gymbuddy.data.local.entity.WorkoutSetEntity::class,
        UserExerciseStateEntity::class,
        UserTemplateStateEntity::class
    ],
    version = 23,
    exportSchema = false
)
@TypeConverters(MuscleGroupConverter::class, ProfileEnumConverters::class, ExerciseConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseVersionDao(): ExerciseVersionDao
    abstract fun templateVersionDao(): TemplateVersionDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun userExerciseStateDao(): UserExerciseStateDao
    abstract fun userTemplateStateDao(): UserTemplateStateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gym_app_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
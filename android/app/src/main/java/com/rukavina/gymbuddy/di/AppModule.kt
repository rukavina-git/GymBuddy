package com.rukavina.gymbuddy.di

import android.content.Context
import androidx.room.Room
import com.rukavina.gymbuddy.data.local.dao.ExerciseDao
import com.rukavina.gymbuddy.data.local.dao.ExerciseVersionDao
import com.rukavina.gymbuddy.data.local.dao.TemplateVersionDao
import com.rukavina.gymbuddy.data.local.dao.UserExerciseStateDao
import com.rukavina.gymbuddy.data.local.dao.UserProfileDao
import com.rukavina.gymbuddy.data.local.dao.UserTemplateStateDao
import com.rukavina.gymbuddy.data.local.dao.WorkoutSessionDao
import com.rukavina.gymbuddy.data.local.dao.WorkoutTemplateDao
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutTemplateRepositoryImpl
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.id.Uuid7Generator
import com.rukavina.gymbuddy.domain.repository.ExerciseRepository
import com.rukavina.gymbuddy.domain.repository.WorkoutSessionRepository
import com.rukavina.gymbuddy.domain.repository.WorkoutTemplateRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext appContext: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "gym_buddy_db"
        ).fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }

    // DAOs
    @Provides
    fun provideUserProfileDao(database: AppDatabase): UserProfileDao {
        return database.userProfileDao()
    }

    @Provides
    fun provideExerciseDao(database: AppDatabase): ExerciseDao {
        return database.exerciseDao()
    }

    @Provides
    fun provideExerciseVersionDao(database: AppDatabase): ExerciseVersionDao {
        return database.exerciseVersionDao()
    }

    @Provides
    fun provideWorkoutSessionDao(database: AppDatabase): WorkoutSessionDao {
        return database.workoutSessionDao()
    }

    @Provides
    fun provideWorkoutTemplateDao(database: AppDatabase): WorkoutTemplateDao {
        return database.workoutTemplateDao()
    }

    @Provides
    fun provideTemplateVersionDao(database: AppDatabase): TemplateVersionDao {
        return database.templateVersionDao()
    }

    @Provides
    fun provideUserExerciseStateDao(database: AppDatabase): UserExerciseStateDao {
        return database.userExerciseStateDao()
    }

    @Provides
    fun provideUserTemplateStateDao(database: AppDatabase): UserTemplateStateDao {
        return database.userTemplateStateDao()
    }

    // Repositories
    @Provides
    @Singleton
    fun provideExerciseRepository(
        exerciseDao: ExerciseDao,
        userExerciseStateDao: UserExerciseStateDao
    ): ExerciseRepository {
        return ExerciseRepositoryImpl(exerciseDao, userExerciseStateDao)
    }

    @Provides
    @Singleton
    fun provideWorkoutSessionRepository(
        workoutSessionDao: WorkoutSessionDao
    ): WorkoutSessionRepository {
        return WorkoutSessionRepositoryImpl(workoutSessionDao)
    }

    @Provides
    @Singleton
    fun provideWorkoutTemplateRepository(
        workoutTemplateDao: WorkoutTemplateDao,
        userTemplateStateDao: UserTemplateStateDao
    ): WorkoutTemplateRepository {
        return WorkoutTemplateRepositoryImpl(workoutTemplateDao, userTemplateStateDao)
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideIdGenerator(clock: Clock): IdGenerator = Uuid7Generator(clock)
}
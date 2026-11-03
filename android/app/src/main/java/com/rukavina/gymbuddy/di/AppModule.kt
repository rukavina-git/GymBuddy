package com.rukavina.gymbuddy.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.local.db.applyAppConfig
import com.rukavina.gymbuddy.data.repository.ActiveWorkoutDraftRepositoryImpl
import com.rukavina.gymbuddy.data.repository.ExerciseRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutSessionRepositoryImpl
import com.rukavina.gymbuddy.data.repository.WorkoutTemplateRepositoryImpl
import com.rukavina.gymbuddy.data.sync.AuthSession
import com.rukavina.gymbuddy.data.sync.OutboxRecorder
import com.rukavina.gymbuddy.domain.id.IdGenerator
import com.rukavina.gymbuddy.domain.id.Uuid7Generator
import com.rukavina.gymbuddy.domain.repository.ActiveWorkoutDraftRepository
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

    /**
     * One Room file per data owner (ADR 0014). Opens the signed-in uid's
     * file (or the guest's) and moves older files first. FirebaseAuth
     * restores the signed-in user synchronously at startup.
     *
     * The pre-Plan-6 file is only moved when Firebase is known to be up:
     * a null uid then really means signed out, not "Firebase failed", so
     * deleting it can't lose a signed-in user's data.
     */
    @Provides
    @Singleton
    fun provideDatabaseHolder(
        @ApplicationContext appContext: Context,
        auth: AuthSession
    ): DatabaseHolder {
        val pathOf = { name: String -> appContext.getDatabasePath(name) }
        val uid = auth.currentUid
        val firebaseUp = FirebaseApp.getApps(appContext).isNotEmpty() &&
            runCatching { FirebaseAuth.getInstance() }.isSuccess
        if (firebaseUp) DatabaseHolder.migrateLegacyFile(pathOf, uid)
        DatabaseHolder.migrateSignedOutFile(pathOf)
        return DatabaseHolder(
            uid,
            open = { owner ->
                Room.databaseBuilder(appContext, AppDatabase::class.java, DatabaseHolder.fileName(owner))
                    .applyAppConfig()
                    .build()
            },
            files = DatabaseHolder.OwnerFiles.inDirectory(pathOf)
        )
    }

    // Repositories
    @Provides
    @Singleton
    fun provideExerciseRepository(
        holder: DatabaseHolder,
        outbox: OutboxRecorder,
        clock: Clock
    ): ExerciseRepository = ExerciseRepositoryImpl(holder, outbox, clock)

    @Provides
    @Singleton
    fun provideWorkoutSessionRepository(
        holder: DatabaseHolder,
        outbox: OutboxRecorder,
        clock: Clock
    ): WorkoutSessionRepository = WorkoutSessionRepositoryImpl(holder, outbox, clock)

    @Provides
    @Singleton
    fun provideWorkoutTemplateRepository(
        holder: DatabaseHolder,
        outbox: OutboxRecorder,
        clock: Clock
    ): WorkoutTemplateRepository = WorkoutTemplateRepositoryImpl(holder, outbox, clock)

    @Provides
    @Singleton
    fun provideActiveWorkoutDraftRepository(
        holder: DatabaseHolder,
        clock: Clock
    ): ActiveWorkoutDraftRepository = ActiveWorkoutDraftRepositoryImpl(holder, clock)

    @Provides
    @Singleton
    fun provideAppPreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("app_preferences") }
        )
    }

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideIdGenerator(clock: Clock): IdGenerator = Uuid7Generator(clock)
}
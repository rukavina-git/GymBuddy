package com.rukavina.gymbuddy.di

import com.rukavina.gymbuddy.BuildConfig
import com.rukavina.gymbuddy.data.local.db.AppDatabase
import com.rukavina.gymbuddy.data.sync.AuthSession
import com.rukavina.gymbuddy.data.sync.FirebaseAuthSession
import com.rukavina.gymbuddy.data.sync.LogoutCoordinator
import com.rukavina.gymbuddy.data.sync.SyncEngine
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.data.sync.SyncScheduler
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideFirebaseAuthSession(): FirebaseAuthSession = FirebaseAuthSession()

    @Provides
    fun provideAuthSession(session: FirebaseAuthSession): AuthSession = session

    @Provides
    @Singleton
    fun provideSyncLocalStore(database: AppDatabase, clock: Clock) = SyncLocalStore(database, clock)

    @Provides
    @Singleton
    fun provideSyncApis(auth: AuthSession): SyncApis = SyncApis.create(BuildConfig.API_BASE_URL, { auth.idToken() })

    @Provides
    @Singleton
    fun provideSyncEngine(store: SyncLocalStore, apis: SyncApis) = SyncEngine(store, apis)

    @Provides
    @Singleton
    fun provideSyncScheduler(engine: SyncEngine, store: SyncLocalStore, auth: AuthSession) = SyncScheduler(
        runCycle = { engine.sync() },
        outboxCount = { store.outboxCount() },
        isSignedIn = { auth.currentUid != null },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )

    @Provides
    fun provideSyncRequester(scheduler: SyncScheduler): SyncRequester = scheduler

    @Provides
    @Singleton
    fun provideLogoutCoordinator(store: SyncLocalStore, engine: SyncEngine, auth: AuthSession) =
        LogoutCoordinator(store, { engine.flushOutbox() }, auth)
}

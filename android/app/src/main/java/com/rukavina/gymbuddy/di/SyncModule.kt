package com.rukavina.gymbuddy.di

import android.content.Context
import com.rukavina.gymbuddy.BuildConfig
import com.rukavina.gymbuddy.data.crash.CrashReporter
import com.rukavina.gymbuddy.data.crash.FirebaseCrashReporter
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.sync.AccountDeletionCoordinator
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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter = FirebaseCrashReporter()

    @Provides
    @Singleton
    fun provideFirebaseAuthSession(): FirebaseAuthSession = FirebaseAuthSession()

    @Provides
    fun provideAuthSession(session: FirebaseAuthSession): AuthSession = session

    @Provides
    @Singleton
    fun provideSyncLocalStore(holder: DatabaseHolder, clock: Clock) = SyncLocalStore(holder, clock)

    @Provides
    @Singleton
    fun provideSyncApis(auth: AuthSession): SyncApis = SyncApis.create(BuildConfig.API_BASE_URL, { auth.idToken() })

    @Provides
    @Singleton
    fun provideSyncEngine(store: SyncLocalStore, apis: SyncApis, auth: AuthSession, holder: DatabaseHolder) =
        SyncEngine(
            store,
            apis,
            // Only the signed-in account's own file syncs, never another owner's outbox (ADR 0014).
            isSignedIn = { auth.currentUid.let { it != null && it == holder.owner } },
            // Shared with the holder, so a database switch never runs mid-cycle.
            mutex = holder.lock
        )

    @Provides
    @Singleton
    fun provideSyncScheduler(engine: SyncEngine, store: SyncLocalStore, auth: AuthSession, crashReporter: CrashReporter) =
        SyncScheduler(
            runCycle = { engine.sync() },
            outboxCount = { store.outboxCount() },
            isSignedIn = { auth.currentUid != null },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            crashReporter = crashReporter
        )

    @Provides
    fun provideSyncRequester(scheduler: SyncScheduler): SyncRequester = scheduler

    @Provides
    @Singleton
    fun provideLogoutCoordinator(store: SyncLocalStore, engine: SyncEngine, auth: AuthSession, crashReporter: CrashReporter) =
        LogoutCoordinator(store, { engine.flushOutbox() }, auth, { block -> engine.exclusive(block) }, crashReporter)

    @Provides
    @Singleton
    fun provideAccountDeletionCoordinator(
        store: SyncLocalStore,
        apis: SyncApis,
        engine: SyncEngine,
        auth: AuthSession,
        @ApplicationContext context: Context
    ) = AccountDeletionCoordinator(
        store,
        deleteOnServer = { apis.account.deleteAccount().code() },
        // Older builds let users pick a photo; remove any left on the phone.
        deleteProfileImages = { File(context.filesDir, "profile_images").deleteRecursively() },
        auth = auth,
        runExclusive = { block -> engine.exclusive(block) }
    )
}

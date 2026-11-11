package com.rukavina.gymbuddy.di

import android.content.Context
import com.rukavina.gymbuddy.BuildConfig
import com.rukavina.gymbuddy.data.crash.CrashReporter
import com.rukavina.gymbuddy.data.crash.FirebaseCrashReporter
import com.rukavina.gymbuddy.data.local.db.DatabaseHolder
import com.rukavina.gymbuddy.data.sync.AccountDeletionCoordinator
import com.rukavina.gymbuddy.data.sync.AuthGate
import com.rukavina.gymbuddy.data.sync.AuthSession
import com.rukavina.gymbuddy.data.sync.DataOwnerStore
import com.rukavina.gymbuddy.data.sync.OwnerReleasingAuthSession
import com.rukavina.gymbuddy.data.sync.SharedPrefsDataOwnerStore
import com.rukavina.gymbuddy.data.sync.FirebaseAuthSession
import com.rukavina.gymbuddy.data.sync.LogoutCoordinator
import com.rukavina.gymbuddy.data.sync.SyncEngine
import com.rukavina.gymbuddy.data.sync.SyncLocalStore
import com.rukavina.gymbuddy.data.sync.SyncScheduler
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import com.rukavina.gymbuddy.domain.sync.SyncRequester
import com.rukavina.gymbuddy.data.repository.AppPreferencesRepository
import com.rukavina.gymbuddy.data.repository.ConsentRecorder
import com.rukavina.gymbuddy.data.sync.GuestMergeState
import com.rukavina.gymbuddy.data.sync.GuestMerge
import com.rukavina.gymbuddy.data.sync.GuestMergeCoordinator
import com.rukavina.gymbuddy.domain.sync.SyncReason
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
    @Singleton
    fun provideDataOwnerStore(@ApplicationContext context: Context, firebase: FirebaseAuthSession): DataOwnerStore =
        SharedPrefsDataOwnerStore(
            context.getSharedPreferences("data_owner", Context.MODE_PRIVATE),
            initialOwner = { firebase.currentUid }
        )

    /** Every app sign-out goes through here, so it's voluntary: the data owner is released first. */
    @Provides
    @Singleton
    fun provideAuthSession(firebase: FirebaseAuthSession, owners: DataOwnerStore): AuthSession =
        OwnerReleasingAuthSession(firebase, owners)

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
            mutex = holder.lock,
            owner = holder.ownerFlow
        )

    @Provides
    @Singleton
    fun provideSyncScheduler(
        engine: SyncEngine,
        store: SyncLocalStore,
        auth: AuthSession,
        crashReporter: CrashReporter,
        gate: dagger.Lazy<AuthGate> // the gate's merge coordinator needs the scheduler
    ) =
        SyncScheduler(
            runCycle = { engine.sync() },
            outboxCount = { store.outboxCount() },
            isSignedIn = { auth.currentUid != null },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            crashReporter = crashReporter,
            onUnauthorized = { gate.get().onUnauthorized() }
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

    @Provides
    @Singleton
    fun provideGuestMerge(holder: DatabaseHolder) = GuestMerge(holder)

    @Provides
    @Singleton
    fun provideGuestMergeCoordinator(
        merge: GuestMerge,
        scheduler: SyncScheduler,
        auth: AuthSession,
        owners: DataOwnerStore,
        preferences: AppPreferencesRepository,
        consent: ConsentRecorder
    ) = GuestMergeCoordinator(
        merge,
        syncNow = { scheduler.syncNow(SyncReason.SIGNED_IN) },
        requestSync = { scheduler.requestSync(SyncReason.SIGNED_IN) },
        currentUid = { auth.currentUid },
        accountEmail = { owners.email },
        signOut = { auth.signOut() },
        setGuest = { preferences.setGuest(it) },
        afterMerge = { consent.applyTo(it) },
        onCancel = { consent.clear() }
    )

    @Provides
    @Singleton
    fun provideAuthGate(
        holder: DatabaseHolder,
        owners: DataOwnerStore,
        merge: GuestMerge,
        firebase: FirebaseAuthSession,
        coordinator: GuestMergeCoordinator,
        preferences: AppPreferencesRepository,
        consent: ConsentRecorder
    ) = AuthGate(
        holder,
        owners,
        merge,
        firebaseUid = { firebase.currentUid },
        probe = { firebase.probe() },
        lastFailure = { firebase.lastFailure },
        clearFailure = { firebase.clearFailure() },
        firebaseEmail = { firebase.currentEmail },
        dropDeadSession = { firebase.signOut() },
        afterSignIn = {
            coordinator.onOwnerChanged(it)
            // While the merge question is open the account's profile may still
            // come from the guest; afterMerge records the consent then.
            if (coordinator.state.value !is GuestMergeState.Asking) consent.applyTo(it)
        },
        setGuest = { preferences.setGuest(it) },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )
}

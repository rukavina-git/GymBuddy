package com.rukavina.gymbuddy

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import androidx.work.WorkManager
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.google.firebase.FirebaseApp
import com.rukavina.gymbuddy.data.local.seeder.ExerciseSeeder
import com.rukavina.gymbuddy.data.local.seeder.WorkoutTemplateSeeder
import com.rukavina.gymbuddy.data.sync.FirebaseAuthSession
import com.rukavina.gymbuddy.data.sync.SyncScheduler
import com.rukavina.gymbuddy.data.sync.SyncTriggers
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class GymBuddyApplication : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject
    lateinit var exerciseSeeder: ExerciseSeeder

    @Inject
    lateinit var workoutTemplateSeeder: WorkoutTemplateSeeder

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var authSession: FirebaseAuthSession

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        Log.d("AppInfo", "Gym Buddy Application starting...")

        SyncTriggers.observeForeground(ProcessLifecycleOwner.get().lifecycle, syncScheduler)
        SyncTriggers.observeNetwork(this, syncScheduler)
        SyncTriggers.schedulePeriodic(WorkManager.getInstance(this))
        // Always true in the app (google-services initialises Firebase at
        // startup); false only where Firebase isn't configured, e.g.
        // Robolectric tests booting this Application.
        if (FirebaseApp.getApps(this).isNotEmpty()) authSession.observeSignIn(syncScheduler)

        // Seed default exercises and templates on app startup
        applicationScope.launch {
            try {
                // Seed exercises first
                val exercisesSeeded = exerciseSeeder.seedIfNeeded(applicationContext)
                if (exercisesSeeded) {
                    Log.i("AppInfo", "Default exercises seeded successfully")
                } else {
                    Log.d("AppInfo", "Default exercises already up to date")
                }

                // Then seed workout templates
                val templatesSeeded = workoutTemplateSeeder.seedIfNeeded(applicationContext)
                if (templatesSeeded) {
                    Log.i("AppInfo", "Default workout templates seeded successfully")
                } else {
                    Log.d("AppInfo", "Default workout templates already up to date")
                }
            } catch (e: Exception) {
                Log.e("AppInfo", "Failed to seed default data", e)
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { OkHttpClient() }))
            }
            .crossfade(true)
            .build()
    }
}
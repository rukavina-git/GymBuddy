package com.rukavina.gymbuddy

import android.app.Application
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.rukavina.gymbuddy.data.local.seeder.ExerciseSeeder
import com.rukavina.gymbuddy.data.local.seeder.WorkoutTemplateSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class GymBuddyApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var exerciseSeeder: ExerciseSeeder

    @Inject
    lateinit var workoutTemplateSeeder: WorkoutTemplateSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("AppInfo", "Gym Buddy Application starting...")

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
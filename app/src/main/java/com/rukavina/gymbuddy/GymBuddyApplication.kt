package com.rukavina.gymbuddy

import android.app.Application
import android.util.Log
import com.rukavina.gymbuddy.data.local.seeder.ExerciseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class GymBuddyApplication : Application() {

    @Inject
    lateinit var exerciseSeeder: ExerciseSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("AppInfo", "Gym Buddy Application starting...")

        // Seed default exercises on app startup
        applicationScope.launch {
            try {
                val seeded = exerciseSeeder.seedIfNeeded(applicationContext)
                if (seeded) {
                    Log.i("AppInfo", "Default exercises seeded successfully")
                } else {
                    Log.d("AppInfo", "Default exercises already up to date")
                }
            } catch (e: Exception) {
                Log.e("AppInfo", "Failed to seed default exercises", e)
            }
        }
    }
}
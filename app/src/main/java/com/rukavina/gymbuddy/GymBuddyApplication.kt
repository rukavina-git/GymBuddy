package com.rukavina.gymbuddy

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GymBuddyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("AppInfo", "Gym Buddy Application starting...")
    }
}
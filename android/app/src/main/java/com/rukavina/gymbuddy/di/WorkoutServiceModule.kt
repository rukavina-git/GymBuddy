package com.rukavina.gymbuddy.di

import com.rukavina.gymbuddy.ui.workout.service.RestBuzzScheduler
import com.rukavina.gymbuddy.ui.workout.service.WorkoutServiceController
import com.rukavina.gymbuddy.ui.workout.service.WorkoutServiceStarter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The workout foreground service and rest buzz (Plan 7 06c). */
@Module
@InstallIn(SingletonComponent::class)
interface WorkoutServiceModule {
    @Binds
    fun bindStarter(controller: WorkoutServiceController): WorkoutServiceStarter

    @Binds
    fun bindRestBuzz(controller: WorkoutServiceController): RestBuzzScheduler
}

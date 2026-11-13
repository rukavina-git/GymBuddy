package com.rukavina.gymbuddy.di

import com.rukavina.gymbuddy.ui.home.HomeSyncSignals
import com.rukavina.gymbuddy.ui.home.RoomHomeSyncSignals
import com.rukavina.gymbuddy.ui.template.RoomTemplateSources
import com.rukavina.gymbuddy.ui.template.TemplateSources
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Home / Workouts / template page reads (Plan 7 step 05a). */
@Module
@InstallIn(SingletonComponent::class)
interface TemplateModule {
    @Binds
    fun bindTemplateSources(impl: RoomTemplateSources): TemplateSources

    @Binds
    fun bindHomeSyncSignals(impl: RoomHomeSyncSignals): HomeSyncSignals
}

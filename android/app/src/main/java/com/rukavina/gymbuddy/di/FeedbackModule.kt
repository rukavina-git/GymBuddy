package com.rukavina.gymbuddy.di

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.work.WorkManager
import com.rukavina.gymbuddy.BuildConfig
import com.rukavina.gymbuddy.data.feedback.FEEDBACK_DATABASE_NAME
import com.rukavina.gymbuddy.data.feedback.FeedbackDatabase
import com.rukavina.gymbuddy.data.feedback.FeedbackDevice
import com.rukavina.gymbuddy.data.feedback.FeedbackOutbox
import com.rukavina.gymbuddy.data.feedback.FeedbackRemote
import com.rukavina.gymbuddy.data.feedback.FeedbackWorker
import com.rukavina.gymbuddy.data.feedback.RetrofitFeedbackRemote
import com.rukavina.gymbuddy.data.sync.AuthSession
import com.rukavina.gymbuddy.domain.id.IdGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeedbackModule {

    @Provides
    @Singleton
    fun provideFeedbackDatabase(@ApplicationContext context: Context): FeedbackDatabase =
        Room.databaseBuilder(context, FeedbackDatabase::class.java, FEEDBACK_DATABASE_NAME).build()

    @Provides
    @Singleton
    fun provideFeedbackRemote(auth: AuthSession): FeedbackRemote = RetrofitFeedbackRemote(BuildConfig.API_BASE_URL, auth)

    @Provides
    @Singleton
    fun provideFeedbackOutbox(
        @ApplicationContext context: Context,
        db: FeedbackDatabase,
        remote: FeedbackRemote,
        ids: IdGenerator,
        clock: Clock,
        auth: AuthSession,
    ) = FeedbackOutbox(
        dao = { db.pendingFeedbackDao() },
        remote = remote,
        ids = ids,
        clock = clock,
        device = FeedbackDevice(
            appVersion = BuildConfig.VERSION_NAME.take(32),
            osVersion = "Android ${Build.VERSION.RELEASE}".take(32),
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".take(64),
        ),
        currentUid = { auth.currentUid },
        scheduleSend = { FeedbackWorker.enqueue(WorkManager.getInstance(context)) },
    )
}

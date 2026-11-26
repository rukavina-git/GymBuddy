package com.rukavina.gymbuddy.data.feedback

import com.rukavina.gymbuddy.data.remote.generated.apis.FeedbackApi
import com.rukavina.gymbuddy.data.remote.generated.models.FeedbackRequest
import com.rukavina.gymbuddy.data.sync.AuthSession
import com.rukavina.gymbuddy.data.sync.remote.AuthTokenProvider
import com.rukavina.gymbuddy.data.sync.remote.SyncApis
import okhttp3.OkHttpClient

/**
 * `POST /v1/feedback` through the generated client. The token goes out
 * only if the message's author is still the one signed in; otherwise
 * (signed out since, another account) it goes as a guest message, never
 * under someone else's account.
 */
class RetrofitFeedbackRemote(
    private val baseUrl: String,
    private val auth: AuthSession,
    private val client: OkHttpClient = SyncApis.defaultClient(),
) : FeedbackRemote {

    override suspend fun send(request: FeedbackRequest, uid: String?): Int {
        val token = AuthTokenProvider { if (uid != null && auth.currentUid == uid) auth.idToken() else null }
        return SyncApis.retrofit(baseUrl, token, client)
            .create(FeedbackApi::class.java)
            .sendFeedback(request)
            .code()
    }
}

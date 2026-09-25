package com.rukavina.gymbuddy.data.sync.remote

import com.rukavina.gymbuddy.data.remote.generated.apis.ReferenceApi
import com.rukavina.gymbuddy.data.remote.generated.apis.SyncApi
import com.rukavina.gymbuddy.data.remote.generated.infrastructure.Serializer
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

/** Supplies the bearer token for API calls. Null means nobody is signed in. */
fun interface AuthTokenProvider {
    suspend fun idToken(): String?
}

/** The generated Retrofit services the sync engine talks to. */
class SyncApis(val sync: SyncApi, val reference: ReferenceApi) {

    companion object {
        /**
         * Builds the generated services against [baseUrl]. Uses the
         * generated client's own Moshi instance so UUID/URI adapters match
         * what the spec declares.
         */
        fun create(baseUrl: String, tokenProvider: AuthTokenProvider, client: OkHttpClient = defaultClient()): SyncApis {
            val retrofit = Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .client(client.newBuilder().addInterceptor(BearerTokenInterceptor(tokenProvider)).build())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(Serializer.moshi))
                .build()
            return SyncApis(retrofit.create(SyncApi::class.java), retrofit.create(ReferenceApi::class.java))
        }

        private fun defaultClient() = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

/**
 * Adds `Authorization: Bearer <Firebase ID token>`. Runs on OkHttp's
 * worker thread, never the main thread, so blocking on the token lookup
 * here is safe. With no signed-in user the request goes out without a
 * token and the server answers 401.
 */
private class BearerTokenInterceptor(private val tokenProvider: AuthTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenProvider.idToken() }
        val request = if (token == null) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}

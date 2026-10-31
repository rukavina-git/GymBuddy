package com.rukavina.gymbuddy.data.sync.remote

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Every API call carries the client version and platform headers. */
class ClientInfoInterceptorTest {

    private val server = MockWebServer()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `api calls carry X-Client-Version and X-Client-Platform`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())
        val apis = SyncApis.create(server.url("/").toString(), { "token" }, clientVersion = "1.2.3-test")

        apis.account.deleteAccount()

        val request = server.takeRequest()
        assertEquals("1.2.3-test", request.headers["X-Client-Version"])
        assertEquals("android", request.headers["X-Client-Platform"])
        assertEquals("Bearer token", request.headers["Authorization"])
    }
}
